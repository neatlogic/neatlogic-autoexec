package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobSourceType;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
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

/** 验证 Tagent 账户、协议和各阶段执行路由的预检。 */
public class AutoexecJobTagentRouteTest {
  private Object previousContext;
  private AutoexecJobServiceImpl service;
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
    service =
        new AutoexecJobServiceImpl() {
          @Override
          protected void validateProtocol(Long protocolId) {}
        };
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

  /** 未登录的用户不能触发作业列表查询。 */
  @Test
  public void visibleJobsRequireBaseAuthority() {
    UserContext.init((UserContext) null);
    rejected(() -> service.getVisibleJobs(Collections.singletonList(1L)));
  }

  /** 主 IP 账号缺失或重复时均不得继续创建。 */
  @Test
  public void absentOrAmbiguousAccountsAreRejected() throws Exception {
    inject(
        service, "tagentMapper", proxy(TagentMapper.class, (p, m, a) -> Collections.emptyList()));
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
    AccountBaseVo account = new AccountBaseVo();
    account.setName("192.0.2.1_3939_tagent");
    inject(
        service,
        "tagentMapper",
        proxy(TagentMapper.class, (p, m, a) -> Arrays.asList(account, account)));
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
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
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
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
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
  }

  /** OS 执行用户和连接凭据名允许不同。 */
  @Test
  public void credentialAndExecutionUserMayDiffer() throws Exception {
    configureTagentTarget(false, false);
    configureRunnerCandidates();
    service.validateTagentRoutes(
        config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1")));
  }

  /** 副地址也按 IP 与协议读取 Tagent 凭据，不以凭据名限制操作系统用户。 */
  @Test
  public void secondaryAddressCanUseTagentCredential() throws Exception {
    configureTagentTarget(true, false);
    configureRunnerCandidates();
    service.validateTagentRoutes(
        config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1")));
  }

  /** 多条 Tagent 路由或无可用作业 Runner 候选均不得启动。 */
  @Test
  public void ambiguousTagentRouteAndMissingRunnerCandidatesAreRejected() throws Exception {
    configureTagentTarget(false, true);
    configureRunnerCandidates();
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
    configureTagentTarget(false, false);
    neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory
        .getSourceTypeMap()
        .put(
            JobSourceType.AUTOEXEC.getValue(),
            proxy(
                neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler.class,
                (p, m, a) -> Collections.emptyList()));
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
  }

  /** 目标阶段逐一使用版本配置的模式与标签，多 Runner 池合法且不会读取数据库历史 Runner。 */
  @Test
  public void runnerRoutesPreservePhaseModesAndExecutionPolicy() throws Exception {
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
    configureTagentTarget(false, false);
    service.validateTagentRoutes(c, Collections.singletonList(new AutoexecNodeVo("192.0.2.1")));
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
    rejected(
        () ->
            service.validateTagentRoutes(
                config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1"))));
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
    configureTagentTarget(false, false);
    service.validateTagentRoutes(
        config(), Collections.singletonList(new AutoexecNodeVo("192.0.2.1")));
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
    Field field = AutoexecJobServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** 创建窄接口替身，任何非预期调用会由测试断言捕获。 */
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
