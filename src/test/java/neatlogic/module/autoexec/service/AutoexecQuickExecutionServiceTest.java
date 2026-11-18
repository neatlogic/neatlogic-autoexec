package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobSourceType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountBaseVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.framework.util.SpringContextUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

/** 不连接数据库或 Runner，验证快捷入口目标收敛和创建阶段无执行副作用。 */
public class AutoexecQuickExecutionServiceTest {
  private Object previousContext;
  private TestService service;
  private neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler
      previousSourceHandler;

  /** 建立测试消息源，生产依赖仅由测试注入。 */
  @Before
  public void setUp() throws Exception {
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    previousContext = context.get(null);
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    StaticApplicationContext app = new StaticApplicationContext();
    app.getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
    new SpringContextUtil().setApplicationContext(app);
    service = new TestService();
    previousSourceHandler =
        neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
            .getSourceTypeMap()
            .get(JobSourceType.AUTOEXEC.getValue());
    UserContext user = UserContext.init((UserContext) null);
    user.setUserUuid("operator-1");
    UserContext.init(user);
  }

  /** 恢复静态上下文，避免污染其他审计测试。 */
  @After
  public void tearDown() throws Exception {
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    context.set(null, previousContext);
    UserContext.init((UserContext) null);
    Map<String, neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler>
        handlers =
            neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
                .getSourceTypeMap();
    if (previousSourceHandler == null) {
      handlers.remove(JobSourceType.AUTOEXEC.getValue());
    } else {
      handlers.put(JobSourceType.AUTOEXEC.getValue(), previousSourceHandler);
    }
  }

  /** 无阶段覆盖的运行时目标允许调用。 */
  @Test
  public void inheritedRuntimeTargetIsAllowed() {
    service.validateConfig(config());
  }

  /** 阶段启用覆盖后，即使只覆盖协议也必须拒绝。 */
  @Test
  public void phaseOverrideIsRejected() {
    AutoexecCombopVersionConfigVo c = config();
    AutoexecCombopExecuteConfigVo override = new AutoexecCombopExecuteConfigVo();
    override.setIsPresetExecuteConfig(1);
    c.getCombopPhaseList().get(0).getConfig().setExecuteConfig(override);
    rejected(() -> service.validateConfig(c));
  }

  /** 编辑器实际会保存空对象或关闭状态的配置，二者都沿用全局目标。 */
  @Test
  public void editorInheritancePlaceholdersAreAllowedWithoutMutation() {
    AutoexecCombopVersionConfigVo c = config();
    AutoexecCombopExecuteConfigVo placeholder =
        com.alibaba.fastjson.JSON.parseObject(
            "{\"isPresetExecuteConfig\":0,\"executeUser\":{\"value\":\"\",\"mappingMode\":\"constant\"},\"executeNodeConfig\":{}}",
            AutoexecCombopExecuteConfigVo.class);
    c.getCombopPhaseList().get(0).getConfig().setExecuteConfig(placeholder);
    String before = com.alibaba.fastjson.JSON.toJSONString(c);
    service.validateConfig(c);
    Assert.assertEquals(before, com.alibaba.fastjson.JSON.toJSONString(c));
    c.getCombopPhaseList().get(0).getConfig().setExecuteConfig(new AutoexecCombopExecuteConfigVo());
    service.validateConfig(c);
    // 原执行器以预设开关为准，关闭后保留的旧目标并不覆盖全局目标。
    placeholder.setExecuteNodeConfig(new AutoexecCombopExecuteNodeConfigVo());
    placeholder.getExecuteNodeConfig().setParamList(Collections.singletonList("oldTarget"));
    c.getCombopPhaseList().get(0).getConfig().setExecuteConfig(placeholder);
    service.validateConfig(c);
  }

  /** 灰度组空配置可继承，但即使预设标志关闭，其实际目标仍不可放行。 */
  @Test
  public void groupPlaceholderCannotHideEffectiveOverrides() {
    AutoexecCombopVersionConfigVo c = config();
    AutoexecCombopGroupVo group = new AutoexecCombopGroupVo();
    group.setConfig("{\"executeConfig\":{}}");
    c.setCombopGroupList(Collections.singletonList(group));
    service.validateConfig(c);
    group.setConfig(
        "{\"executeConfig\":{\"isPresetExecuteConfig\":0,\"executeNodeConfig\":{\"paramList\":[\"otherHost\"]}}}");
    rejected(() -> service.validateConfig(c));
  }

  /** 灰度组私有目标与阶段私有目标一样必须拒绝。 */
  @Test
  public void groupOverrideIsRejected() {
    AutoexecCombopVersionConfigVo c = config();
    AutoexecCombopGroupVo group = new AutoexecCombopGroupVo();
    group.setConfig("{\"executeConfig\":{\"isPresetExecuteConfig\":1,\"whenToSpecify\":\"now\"}}");
    c.setCombopGroupList(Collections.singletonList(group));
    rejected(() -> service.validateConfig(c));
  }

  /** 固定目标、参数目标、过滤器或上游出参均不得进入快捷入口。 */
  @Test
  public void alternateTargetSourcesAreRejected() {
    for (String mode : Arrays.asList("now", "runtimeparam")) {
      AutoexecCombopVersionConfigVo c = config();
      c.getExecuteConfig().setWhenToSpecify(mode);
      rejected(() -> service.validateConfig(c));
    }
    AutoexecCombopVersionConfigVo c = config();
    AutoexecCombopExecuteNodeConfigVo nodes = new AutoexecCombopExecuteNodeConfigVo();
    nodes.setPreOutputList(Collections.singletonList("other.host"));
    c.getExecuteConfig().setExecuteNodeConfig(nodes);
    rejected(() -> service.validateConfig(c));
    nodes.setPreOutputList(null);
    JSONObject filter = new JSONObject();
    filter.put("keyword", "all");
    nodes.setFilter(filter);
    rejected(() -> service.validateConfig(c));
  }

  /** 本地阶段或嵌套工具都不能保证只操作当前主机。 */
  @Test
  public void runnerAndNestedCombopAreRejected() {
    AutoexecCombopVersionConfigVo c = config();
    c.getCombopPhaseList().get(0).setExecMode("runner");
    rejected(() -> service.validateConfig(c));
    c.getCombopPhaseList().get(0).setExecMode("target");
    c.getCombopPhaseList()
        .get(0)
        .getConfig()
        .getPhaseOperationList()
        .get(0)
        .setOperationType("combop");
    rejected(() -> service.validateConfig(c));
  }

  /** 预检拒绝预览后版本变化，不能使用新版本静默替换。 */
  @Test
  public void changedVersionIsRejected() {
    rejected(() -> service.validateTarget(1L, 99L, "192.0.2.1"));
  }

  /** 主 IP 账号缺失或重复时均不得继续创建。 */
  @Test
  public void absentOrAmbiguousAccountsAreRejected() throws Exception {
    inject(
        service, "tagentMapper", proxy(TagentMapper.class, (p, m, a) -> Collections.emptyList()));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
    AccountBaseVo account = new AccountBaseVo();
    account.setName("192.0.2.1_3939_tagent");
    inject(
        service,
        "tagentMapper",
        proxy(TagentMapper.class, (p, m, a) -> Arrays.asList(account, account)));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
  }

  /** 账号存在但没有唯一 Tagent 或 Runner 时预览不能通过。 */
  @Test
  public void absentAgentAndRunnerAreRejected() throws Exception {
    AccountBaseVo account = new AccountBaseVo();
    account.setName("192.0.2.1_3939_tagent");
    account.setProtocolPort(3939);
    inject(
        service,
        "tagentMapper",
        proxy(
            TagentMapper.class,
            (p, m, a) -> {
              if (m.getName().startsWith("getAccount")) {
                return Collections.singletonList(account);
              }
              return Collections.emptyList();
            }));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
    neatlogic.framework.tagent.dto.TagentVo agent = new neatlogic.framework.tagent.dto.TagentVo();
    agent.setRunnerId(7L);
    inject(
        service,
        "tagentMapper",
        proxy(
            TagentMapper.class,
            (p, m, a) -> {
              if (m.getName().startsWith("getAccount")) {
                return Collections.singletonList(account);
              }
              return Collections.singletonList(agent);
            }));
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) -> Collections.emptyList()));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
  }

  /** Tagent 认证账户名不是 OS 执行用户；数据库 runnerId 为空不代表没有作业路由。 */
  @Test
  public void tagentCredentialNameDoesNotRestrictExecutionUser() throws Exception {
    configureTagentTarget(false, false);
    configureRunnerCandidates();
    final AutoexecJobVo[] saved = new AutoexecJobVo[1];
    inject(
        service,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              saved[0] = (AutoexecJobVo) a[0];
              saved[0].setId(10L);
              return null;
            }));
    Assert.assertEquals(
        Long.valueOf(10),
        service.createReadyJob(1L, 2L, "diagnose", "192.0.2.1", new JSONObject()));
    Assert.assertEquals("root", saved[0].getExecuteConfig().getExecuteUser().getValue());
  }

  /** 副地址也按 IP 与协议读取 Tagent 凭据，不以凭据名限制操作系统用户。 */
  @Test
  public void secondaryAddressCanUseTagentCredential() throws Exception {
    configureTagentTarget(true, false);
    configureRunnerCandidates();
    service.validateTarget(1L, 2L, "192.0.2.1");
  }

  /** 多条 Tagent 路由或无可用作业 Runner 候选均不得启动。 */
  @Test
  public void ambiguousTagentRouteAndMissingRunnerCandidatesAreRejected() throws Exception {
    configureTagentTarget(false, true);
    configureRunnerCandidates();
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
    configureTagentTarget(false, false);
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) -> Collections.emptyList()));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
  }

  /** 目标阶段逐一使用版本配置的模式与标签，多 Runner 池合法且不会读取数据库历史 Runner。 */
  @Test
  public void runnerRoutesPreservePhaseModesAndExecutionPolicy() {
    AutoexecCombopVersionConfigVo c = config();
    c.getExecuteConfig().setRunnerGroup(new ParamMappingVo("runnerGroup", "constant", "group-a"));
    c.getExecuteConfig()
        .setRunnerGroupTag(new ParamMappingVo("runnerGroupTag", "constant", "[\"tag-a\"]"));
    AutoexecCombopPhaseVo second = new AutoexecCombopPhaseVo();
    second.setExecMode("runner_target");
    second.setConfig(new AutoexecCombopPhaseConfigVo());
    c.setCombopPhaseList(Arrays.asList(c.getCombopPhaseList().get(0), second));
    List<String> modes = new ArrayList<>();
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) -> {
                  Assert.assertEquals("getRunnerMapList", m.getName());
                  AutoexecJobVo preview = (AutoexecJobVo) a[0];
                  modes.add(preview.getExecutePhase().getExecMode());
                  Assert.assertEquals(
                      "192.0.2.1", preview.getExecutePhase().getCurrentNode().getHost());
                  Assert.assertSame(
                      c.getExecuteConfig().getRunnerGroup(), preview.getRunnerGroup());
                  Assert.assertSame(
                      c.getExecuteConfig().getRunnerGroupTag(), preview.getRunnerGroupTag());
                  neatlogic.framework.dto.runner.RunnerMapVo one =
                      new neatlogic.framework.dto.runner.RunnerMapVo();
                  one.setRunnerMapId(1001L);
                  one.setUrl("http://runner-one.test");
                  neatlogic.framework.dto.runner.RunnerMapVo two =
                      new neatlogic.framework.dto.runner.RunnerMapVo();
                  two.setRunnerMapId(2002L);
                  two.setUrl("http://runner-two.test");
                  return Arrays.asList(one, two);
                }));
    service.validateRunnerRoutes(c, "192.0.2.1");
    Assert.assertEquals(Arrays.asList("target", "runner_target"), modes);
  }

  /** 路由池条目没有可访问地址时仍然阻止执行。 */
  @Test
  public void runnerCandidateWithoutUrlIsRejected() throws Exception {
    configureTagentTarget(false, false);
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) ->
                    Collections.singletonList(new neatlogic.framework.dto.runner.RunnerMapVo())));
    rejected(() -> service.validateTarget(1L, 2L, "192.0.2.1"));
  }

  /** 实际作业路由的 target 分支只读取规则和网段，不创建作业或写阶段绑定。 */
  @Test
  public void actualTargetRoutingIsReadOnly() throws Exception {
    neatlogic.framework.dto.runner.RunnerGroupVo group =
        new neatlogic.framework.dto.runner.RunnerGroupVo();
    group.setId(1L);
    neatlogic.framework.dto.runner.RunnerMapVo candidate =
        new neatlogic.framework.dto.runner.RunnerMapVo();
    candidate.setRunnerMapId(1001L);
    candidate.setUrl("http://runner.test");
    group.setRunnerMapList(Collections.singletonList(candidate));
    neatlogic.framework.dto.runner.GroupNetworkVo network =
        new neatlogic.framework.dto.runner.GroupNetworkVo();
    network.setGroupId(1L);
    network.setNetworkIp("192.0.2.0");
    network.setMask(24);
    List<String> reads = new ArrayList<>();
    neatlogic.framework.dao.mapper.runner.RunnerMapper routes =
        proxy(
            neatlogic.framework.dao.mapper.runner.RunnerMapper.class,
            (p, m, a) -> {
              reads.add(m.getName());
              if ("getAllRunnerGroupList".equals(m.getName())) {
                return Collections.singletonList(group);
              }
              if ("getAllNetworkMask".equals(m.getName())) {
                return Collections.singletonList(network);
              }
              if ("getRunnerMapGroupById".equals(m.getName())) {
                return group;
              }
              throw new AssertionError("预检不应调用：" + m.getName());
            });
    neatlogic.module.autoexec.job.source.action.AutoexecJobSourceTypeHandler handler =
        new neatlogic.module.autoexec.job.source.action.AutoexecJobSourceTypeHandler();
    Field mapper =
        neatlogic.module.autoexec.job.source.action.AutoexecJobSourceTypeHandler.class
            .getDeclaredField("runnerMapper");
    mapper.setAccessible(true);
    mapper.set(handler, routes);
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(JobSourceType.AUTOEXEC.getValue(), handler);
    service.validateRunnerRoutes(config(), "192.0.2.1");
    Assert.assertEquals(
        Arrays.asList("getAllRunnerGroupList", "getAllNetworkMask", "getRunnerMapGroupById"),
        reads);
  }

  /** 固定不同的 Tagent、RunnerMap 标识，Mapper 替身禁止以数据库 runnerId 查询 Runner。 */
  private void configureTagentTarget(boolean secondary, boolean ambiguous) throws Exception {
    AccountBaseVo account = new AccountBaseVo();
    account.setName("192.0.2.1_3939_tagent");
    account.setProtocolPort(3939);
    neatlogic.framework.tagent.dto.TagentVo agent = new neatlogic.framework.tagent.dto.TagentVo();
    agent.setId(20L);
    inject(
        service,
        "tagentMapper",
        proxy(
            TagentMapper.class,
            (p, m, a) -> {
              if (m.getName().equals("getAccountListByMainIpListAndProtocolId") && secondary) {
                return Collections.emptyList();
              }
              if (m.getName().startsWith("getAccount")) {
                return Collections.singletonList(account);
              }
              Assert.assertEquals("getTagentByIpOrTagentIpAndPort", m.getName());
              Assert.assertEquals("192.0.2.1", a[0]);
              Assert.assertEquals(Integer.valueOf(3939), a[1]);
              if (ambiguous) {
                return Arrays.asList(agent, agent);
              }
              return Collections.singletonList(agent);
            }));
  }

  /** 仅允许既有路由契约读取目标节点候选；任何其他调用都属于预检副作用。 */
  private void configureRunnerCandidates() {
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) -> {
                  Assert.assertEquals("getRunnerMapList", m.getName());
                  AutoexecJobVo preview = (AutoexecJobVo) a[0];
                  Assert.assertEquals("target", preview.getExecutePhase().getExecMode());
                  Assert.assertEquals(
                      "192.0.2.1", preview.getExecutePhase().getCurrentNode().getHost());
                  neatlogic.framework.dto.runner.RunnerMapVo candidate =
                      new neatlogic.framework.dto.runner.RunnerMapVo();
                  candidate.setRunnerMapId(1001L);
                  candidate.setUrl("http://runner.test:8080");
                  return Collections.singletonList(candidate);
                }));
  }

  /** 协议从账户模块读取，不能用普通 SSH 伪装成 Tagent。 */
  @Test
  @SuppressWarnings("unchecked")
  public void nonTagentProtocolIsRejected() throws Exception {
    Class<neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper> type =
        neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper.class;
    Field f =
        neatlogic.framework.crossover.CrossoverServiceFactory.class.getDeclaredField("apiMap");
    f.setAccessible(true);
    Map<Object, Object> apis = (Map<Object, Object>) f.get(null);
    Object previous = apis.get(type);
    try {
      apis.put(
          type,
          proxy(
              type,
              (p, m, a) ->
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo("ssh")));
      AutoexecQuickExecutionServiceImpl real = new AutoexecQuickExecutionServiceImpl();
      rejected(() -> real.validateProtocol(1L));
    } finally {
      if (previous == null) {
        apis.remove(type);
      } else {
        apis.put(type, previous);
      }
    }
  }

  /** 创建只调用校验落库，不调用启动或定时触发，目标不含服务端口。 */
  @Test
  public void createOnlyPersistsManualSingleTarget() throws Exception {
    TestService prepared =
        new TestService() {
          @Override
          public void validateTarget(Long id, Long version, String host) {}
        };
    final AutoexecJobVo[] saved = new AutoexecJobVo[1];
    inject(
        prepared,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              saved[0] = (AutoexecJobVo) a[0];
              saved[0].setId(10L);
              return null;
            }));
    Assert.assertEquals(
        Long.valueOf(10),
        prepared.createReadyJob(1L, 2L, "diagnose", "192.0.2.1", new JSONObject()));
    Assert.assertEquals("manual", saved[0].getTriggerType());
    Assert.assertEquals("combop", saved[0].getSource());
    Assert.assertEquals(
        1, saved[0].getExecuteConfig().getExecuteNodeConfig().getInputNodeList().size());
    Assert.assertEquals(
        "192.0.2.1",
        saved[0].getExecuteConfig().getExecuteNodeConfig().getInputNodeList().get(0).getIp());
    Assert.assertNull(
        saved[0].getExecuteConfig().getExecuteNodeConfig().getInputNodeList().get(0).getPort());
  }

  /** 租户关闭普通参数验证也不能绕过快捷执行的必填约束。 */
  @Test
  public void requiredParameterIsAlwaysValidated() {
    AutoexecCombopVersionConfigVo c = config();
    neatlogic.framework.autoexec.dto.AutoexecParamVo p =
        new neatlogic.framework.autoexec.dto.AutoexecParamVo();
    p.setKey("servicePort");
    p.setType("text");
    p.setName("端口");
    p.setIsRequired(1);
    c.setRuntimeParamList(Collections.singletonList(p));
    rejected(() -> service.validateParams(c, new JSONObject()));
  }

  /** 已启动或终态作业再次提交只返回原状态，不能重跑。 */
  @Test
  public void completedJobIsNotStartedAgain() throws Exception {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setId(10L);
    job.setOperationId(1L);
    job.setSource("combop");
    job.setExecUser("operator-1");
    job.setStatus("completed");
    inject(service, "jobMapper", proxy(AutoexecJobMapper.class, (p, m, a) -> job));
    JSONObject result = service.startJob(10L);
    Assert.assertEquals("10", result.getString("jobId"));
    Assert.assertEquals("completed", result.getString("status"));
  }

  /** 即使可查看作业，也不能通过快捷入口代替原执行人启动。 */
  @Test
  public void otherExecutorCannotStartJob() throws Exception {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setId(10L);
    job.setOperationId(1L);
    job.setSource("combop");
    job.setExecUser("other-operator");
    job.setStatus("ready");
    inject(service, "jobMapper", proxy(AutoexecJobMapper.class, (p, m, a) -> job));
    rejected(() -> service.startJob(10L));
  }

  /** 参数 schema 的敏感默认值不能从工具对象旁路泄漏，真实版本数据仍保留。 */
  @Test
  public void describeRemovesPasswordValuesWithoutMutatingVersion() {
    AutoexecCombopVersionVo version = new AutoexecCombopVersionVo();
    version.setId(2L);
    version.setConfig(config());
    neatlogic.framework.autoexec.dto.AutoexecParamVo password =
        new neatlogic.framework.autoexec.dto.AutoexecParamVo();
    password.setId(20L);
    password.setKey("credential");
    password.setType("password");
    password.setValue("secret-value");
    password.setDefaultValue("secret-default");
    password.setConfig("{\"defaultValue\":\"secret-config-default\"}");
    version.getConfig().setRuntimeParamList(Collections.singletonList(password));
    TestService prepared =
        new TestService() {
          @Override
          protected AutoexecCombopVersionVo active(Long id) {
            return version;
          }
        };
    JSONObject result = prepared.describe(1L);
    Assert.assertFalse(result.toJSONString().contains("secret-"));
    JSONObject schema = result.getJSONArray("runtimeParamList").getJSONObject(0);
    Assert.assertFalse(schema.containsKey("value"));
    Assert.assertFalse(schema.containsKey("defaultValue"));
    Assert.assertFalse(schema.getJSONObject("config").containsKey("defaultValue"));
    Assert.assertEquals("secret-value", password.getValue());
    Assert.assertEquals("secret-default", password.getDefaultValue());
    Assert.assertEquals("secret-config-default", password.getConfig().getDefaultValue());
  }

  /** 类型提供的空值规则覆盖集合和文件，不能把空容器视为必填已填写。 */
  @Test
  public void requiredCollectionAndFileParametersRejectEmptyValues() {
    for (String type :
        Arrays.asList("multiselect", "checkbox", "node", "userselect", "runnergrouptag", "file")) {
      AutoexecCombopVersionConfigVo c = config();
      neatlogic.framework.autoexec.dto.AutoexecParamVo p =
          new neatlogic.framework.autoexec.dto.AutoexecParamVo();
      p.setKey("target");
      p.setName("目标");
      p.setType(type);
      p.setIsRequired(1);
      c.setRuntimeParamList(Collections.singletonList(p));
      JSONObject values = new JSONObject();
      if ("file".equals(type)) {
        JSONObject file = new JSONObject();
        file.put("fileIdList", new com.alibaba.fastjson.JSONArray());
        values.put("target", file);
      } else {
        values.put("target", new com.alibaba.fastjson.JSONArray());
      }
      rejected(() -> service.validateParams(c, values));
    }
  }

  /** 可选 JSON 参数存在时同样必须是有效 JSON，失败不包含原始正文。 */
  @Test
  public void optionalJsonStillRejectsMalformedContent() {
    AutoexecCombopVersionConfigVo c = config();
    neatlogic.framework.autoexec.dto.AutoexecParamVo p =
        new neatlogic.framework.autoexec.dto.AutoexecParamVo();
    p.setKey("content");
    p.setName("内容");
    p.setType("json");
    p.setIsRequired(0);
    c.setRuntimeParamList(Collections.singletonList(p));
    JSONObject values = new JSONObject();
    values.put("content", "{broken");
    rejected(() -> service.validateParams(c, values));
  }

  /** 生产无权限检查失败时不能调用 Mapper 或执行服务。 */
  @Test
  public void basePermissionFailureStopsQueries() {
    AutoexecQuickExecutionServiceImpl denied =
        new AutoexecQuickExecutionServiceImpl() {
          @Override
          protected void checkBase() {
            throw new IllegalStateException("denied");
          }
        };
    try {
      denied.getVisibleJobs(Collections.singletonList(1L));
      Assert.fail();
    } catch (IllegalStateException expected) {
      Assert.assertEquals("denied", expected.getMessage());
    }
  }

  /** 构造合法最小配置，测试只替换正在验证的约束。 */
  private static AutoexecCombopVersionConfigVo config() {
    AutoexecCombopExecuteConfigVo execute = new AutoexecCombopExecuteConfigVo();
    execute.setWhenToSpecify("runtime");
    execute.setProtocolId(1L);
    execute.setExecuteUser(new ParamMappingVo("executeUser", "constant", "root"));
    AutoexecCombopPhaseOperationVo op = new AutoexecCombopPhaseOperationVo();
    op.setOperationType("script");
    AutoexecCombopPhaseConfigVo pc = new AutoexecCombopPhaseConfigVo();
    pc.setPhaseOperationList(Collections.singletonList(op));
    AutoexecCombopPhaseVo phase = new AutoexecCombopPhaseVo();
    phase.setExecMode("target");
    phase.setConfig(pc);
    AutoexecCombopVersionConfigVo c = new AutoexecCombopVersionConfigVo();
    c.setExecuteConfig(execute);
    c.setCombopPhaseList(Collections.singletonList(phase));
    return c;
  }

  /** 替换外部模块发现，保留目标与创建业务校验。 */
  private static class TestService extends AutoexecQuickExecutionServiceImpl {
    @Override
    protected void checkBase() {}

    @Override
    protected void validateProtocol(Long id) {}

    @Override
    protected AutoexecCombopVersionVo active(Long id) {
      AutoexecCombopVersionVo version = new AutoexecCombopVersionVo();
      version.setId(2L);
      version.setConfig(config());
      return version;
    }
  }

  /** 断言约束失败，不接受 NPE 等意外异常作为通过。 */
  private static void rejected(Runnable action) {
    try {
      action.run();
      Assert.fail("应拒绝不安全配置");
    } catch (ParamIrregularException expected) {
    }
  }

  /** 测试代码通过反射注入替身，不给生产类增加装配入口。 */
  private static void inject(Object target, String name, Object value) throws Exception {
    Field field = AutoexecQuickExecutionServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** 创建窄接口替身，任何非预期调用会由测试断言捕获。 */
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
