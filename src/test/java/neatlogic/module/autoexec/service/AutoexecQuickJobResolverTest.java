package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecQuickJobRequestVo;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.ICrossoverService;
import neatlogic.framework.crossover.IFileCrossoverService;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.util.SpringContextUtil;
import neatlogic.module.autoexec.script.paramtype.*;
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

/** 真实类型处理器矩阵及单主机目标锁定回归，不访问数据库或执行 Runner。 */
public class AutoexecQuickJobResolverTest {
  private Map<String, IScriptParamType> registry;
  private Map<String, IScriptParamType> oldRegistry;
  private Map<Class<? extends ICrossoverService>, ICrossoverService> crossovers;
  private Map<Class<? extends ICrossoverService>, ICrossoverService> oldCrossovers;
  private Object oldContext;
  private Resolver service;
  private AutoexecCombopVersionVo version;

  /** 注册全部真实参数类型，并注入只读账户和文件替身。 */
  @Before
  @SuppressWarnings("unchecked")
  public void setup() throws Exception {
    Field ctx = SpringContextUtil.class.getDeclaredField("ctx");
    ctx.setAccessible(true);
    oldContext = ctx.get(null);
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    JSONObject localLabels = quickToolLabels("en");
    for (String key : localLabels.keySet()) {
      for (Locale locale :
          Arrays.asList(
              Locale.getDefault(),
              Locale.ENGLISH,
              Locale.US,
              Locale.UK,
              Locale.SIMPLIFIED_CHINESE,
              Locale.TRADITIONAL_CHINESE,
              new Locale("zh", "HK"))) {
        messages.addMessage("autoexec.quicktool.field." + key, locale, localLabels.getString(key));
      }
    }
    StaticApplicationContext app = new StaticApplicationContext();
    app.getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
    new SpringContextUtil().setApplicationContext(app);
    Field types = ScriptParamTypeFactory.class.getDeclaredField("scriptParamTypeMap");
    types.setAccessible(true);
    registry = (Map<String, IScriptParamType>) types.get(null);
    oldRegistry = new HashMap<>(registry);
    registry.clear();
    for (IScriptParamType handler :
        Arrays.asList(
            new ScriptParamTypeText(),
            new ScriptParamTypePassword(),
            new ScriptParamTypeFile(),
            new ScriptParamTypeDate(),
            new ScriptParamTypeDatetime(),
            new ScriptParamTypeTime(),
            new ScriptParamTypeJson(),
            new ScriptParamTypeSelect(),
            new ScriptParamTypeMultiSelect(),
            new ScriptParamTypeRadio(),
            new ScriptParamTypeCheckbox(),
            new ScriptParamTypeNode(),
            new ScriptParamTypeAccount(),
            new ScriptParamTypeUserSelect(),
            new ScriptParamTypeTextarea(),
            new ScriptParamTypePhase(),
            new ScriptParamTypeSwitch(),
            new ScriptParamTypeFilePath(),
            new ScriptParamTypeRunnerGroup(),
            new ScriptParamTypeRunnerGroupTag())) {
      registry.put(handler.getType(), handler);
    }
    Field apis = CrossoverServiceFactory.class.getDeclaredField("apiMap");
    apis.setAccessible(true);
    crossovers = (Map<Class<? extends ICrossoverService>, ICrossoverService>) apis.get(null);
    oldCrossovers = new HashMap<>(crossovers);
    crossovers.clear();
    crossovers.put(
        IFileCrossoverService.class, proxy(IFileCrossoverService.class, (p, m, a) -> null));
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) -> {
              if (m.getName().equals("getAccountById")) {
                AccountVo account = new AccountVo();
                account.setId((Long) a[0]);
                return account;
              }
              return null;
            }));
    version = new AutoexecCombopVersionVo();
    version.setId(2L);
    version.setCombopId(1L);
    version.setName("诊断");
    AutoexecCombopVersionConfigVo config = new AutoexecCombopVersionConfigVo();
    AutoexecCombopExecuteConfigVo execute = new AutoexecCombopExecuteConfigVo();
    execute.setWhenToSpecify("runtime");
    execute.setProtocolId(11L);
    execute.setExecuteUser(new ParamMappingVo("executeUser", "constant", "root"));
    config.setExecuteConfig(execute);
    AutoexecCombopPhaseVo phase = new AutoexecCombopPhaseVo();
    phase.setName("诊断阶段");
    phase.setExecMode("target");
    AutoexecCombopPhaseConfigVo pc = new AutoexecCombopPhaseConfigVo();
    pc.setPhaseOperationList(new ArrayList<>());
    phase.setConfig(pc);
    config.setCombopPhaseList(Collections.singletonList(phase));
    config.setRuntimeParamList(new ArrayList<>());
    version.setConfig(config);
    service = new Resolver();
    inject(service, "autoexecService", proxy(AutoexecService.class, (p, m, a) -> true));
    inject(
        service,
        "runnerMapper",
        proxy(neatlogic.framework.dao.mapper.runner.RunnerMapper.class, (p, m, a) -> null));
    inject(
        service,
        "tagMapper",
        proxy(
            neatlogic.framework.dao.mapper.TagMapper.class, (p, m, a) -> Collections.emptyList()));
  }

  /** 恢复注册表和消息上下文，避免影响相邻的旧契约安全测试。 */
  @After
  public void cleanup() throws Exception {
    registry.clear();
    registry.putAll(oldRegistry);
    crossovers.clear();
    crossovers.putAll(oldCrossovers);
    Field ctx = SpringContextUtil.class.getDeclaredField("ctx");
    ctx.setAccessible(true);
    ctx.set(null, oldContext);
  }

  /** 所有注册类型与流程组件转换使用同一真实处理器，结果不得多包一层数组或重复加密。 */
  @Test
  public void allRegisteredTypesMatchProcessConversion() {
    Assert.assertEquals(ParamType.values().length, registry.size());
    for (ParamType type : ParamType.values()) {
      Object raw = sample(type.getValue());
      JSONObject definition = definition("value", type.getValue(), 0);
      Object actual = service.normalizeValue(definition, raw);
      JSONArray group = new JSONArray();
      if (raw instanceof JSONArray && type != ParamType.JSON) {
        group.addAll((JSONArray) raw);
      } else {
        group.add(raw);
      }
      Object process = registry.get(type.getValue()).convertDataForProcessComponent(group);
      Assert.assertEquals(type.getValue(), JSON.toJSONString(process), JSON.toJSONString(actual));
      Assert.assertEquals(
          type.getValue(),
          JSON.toJSONString(actual),
          JSON.toJSONString(service.normalizeValue(definition, actual)));
    }
  }

  /** 原生文件结构、JSON 单元素数组和密码必须保持形态，Runner 转换留给既有执行链。 */
  @Test
  public void nativeStructuredValuesAreNotDoubleConverted() {
    Assert.assertEquals(
        "plain", service.normalizeValue(definition("password", "password", 1), "plain"));
    JSONArray json = JSON.parseArray("[{\"value\":1}]");
    Assert.assertEquals(json, service.normalizeValue(definition("json", "json", 1), json));
    JSONObject file = (JSONObject) sample("file");
    Assert.assertEquals(file, service.normalizeValue(definition("file", "file", 1), file));
  }

  /** 配置和草稿预览不因尚未指定协议、用户就检查 Tagent 凭据。 */
  @Test
  public void partialDraftDefersCredentialValidation() {
    version.getConfig().getExecuteConfig().setProtocolId(null);
    version.getConfig().getExecuteConfig().setExecuteUser(null);
    JSONObject result = service.resolveJob(request(false));
    Assert.assertFalse(result.getBooleanValue("complete"));
    Assert.assertEquals(0, service.targetChecks);
    rejected(() -> service.resolveJob(request(true)));
    Assert.assertEquals(0, service.targetChecks);
  }

  /** 运行时执行字段补齐后形成有效配置，完整解析才做目标与路由预检。 */
  @Test
  public void runtimeExecutionMappingsAreResolvedBeforeValidation() {
    version.getConfig().getExecuteConfig().setProtocolId(null);
    version.getConfig().getExecuteConfig().setExecuteUser(null);
    AutoexecQuickJobRequestVo request = request(true);
    request
        .getJobConfig()
        .put(
            "executeParamMappingList",
            JSON.parseArray(
                "[{\"key\":\"protocolId\",\"source\":\"input\"},{\"key\":\"executeUser\",\"source\":\"context\",\"value\":\"osUser\"}]"));
    request.getContext().put("osUser", "root");
    request.setInput(JSON.parseObject("{\"executeParam\":{\"protocolId\":\"11\"}}"));
    JSONObject resolved = service.resolveJob(request);
    Assert.assertEquals("11", resolved.getString("protocolId"));
    Assert.assertEquals("root", resolved.getString("executeUser"));
    Assert.assertTrue(resolved.getBooleanValue("complete"));
    Assert.assertEquals(1, service.targetChecks);
  }

  /** 激活版本的常量字段只读，不能通过执行参数映射覆盖。 */
  @Test
  public void fixedExecutionFieldsCannotBeMapped() {
    AutoexecQuickJobRequestVo request = request(false);
    request
        .getJobConfig()
        .put(
            "executeParamMappingList",
            JSON.parseArray("[{\"key\":\"executeUser\",\"source\":\"fixed\",\"value\":\"evil\"}]"));
    rejected(() -> service.resolveJob(request));
  }

  /** 引用节点运行参数的工具也只绑定当前宿主机，客户端伪造第二个目标被拒绝。 */
  @Test
  public void referencedTargetParametersAreLockedToCurrentHost() {
    AutoexecParamVo target = parameter("nodes", "node", 1);
    version.getConfig().getRuntimeParamList().add(target);
    AutoexecCombopExecuteNodeConfigVo nodes = new AutoexecCombopExecuteNodeConfigVo();
    nodes.setParamList(Collections.singletonList("nodes"));
    version.getConfig().getExecuteConfig().setWhenToSpecify("runtimeparam");
    version.getConfig().getExecuteConfig().setExecuteNodeConfig(nodes);
    AutoexecQuickJobRequestVo request = request(false);
    request.setInput(JSON.parseObject("{\"param\":{\"nodes\":[{\"ip\":\"203.0.113.9\"}]}}"));
    rejected(() -> service.resolveJob(request));
    request.setInput(new JSONObject());
    JSONObject resolved = service.resolveJob(request);
    JSONObject definition = resolved.getJSONArray("paramList").getJSONObject(0);
    Assert.assertTrue(definition.getBooleanValue("locked"));
    Assert.assertEquals(
        "192.0.2.1", definition.getJSONArray("value").getJSONObject(0).getString("ip"));
    request.getContext().put("currentNode", JSON.parseArray("[{\"ip\":\"203.0.113.9\"}]"));
    rejected(() -> service.resolveJob(request));
  }

  /** 未知字段和对非人工来源的覆盖都不得通过嵌套输入绕过配置。 */
  @Test
  public void unknownAndNonInputFieldsAreRejected() {
    AutoexecQuickJobRequestVo request = request(false);
    for (String invalidInput :
        Arrays.asList(
            "{\"param\":{\"unknown\":1}}",
            "{\"executeParam\":{\"executeUser\":\"evil\"}}",
            "{\"executeParam\":{\"executeNodeConfig\":[{\"ip\":\"evil\"}]}}",
            "{\"param\":[]}")) {
      request.setInput(JSON.parseObject(invalidInput));
      rejected(() -> service.resolveJob(request));
    }
    version.getConfig().getRuntimeParamList().add(parameter("contextValue", "text", 0));
    request
        .getJobConfig()
        .put(
            "paramMappingList",
            JSON.parseArray(
                "[{\"key\":\"contextValue\",\"source\":\"context\",\"value\":\"serviceName\"}]"));
    request.setInput(JSON.parseObject("{\"param\":{\"contextValue\":\"evil\"}}"));
    rejected(() -> service.resolveJob(request));
  }

  /** 工具已有运行参数引用时执行字段只能派生，不能新增第二套执行窗口输入。 */
  @Test
  public void executionRuntimeReferenceIsDerivedFromOriginalParameter() {
    version
        .getConfig()
        .getExecuteConfig()
        .setExecuteUser(new ParamMappingVo("executeUser", "runtimeparam", "osUser"));
    version.getConfig().getRuntimeParamList().add(parameter("osUser", "text", 1));
    AutoexecQuickJobRequestVo request = request(true);
    request.setInput(JSON.parseObject("{\"param\":{\"osUser\":\"root\"}}"));
    Assert.assertEquals("root", service.resolveJob(request).getString("executeUser"));
    request.getInput().put("executeParam", JSON.parseObject("{\"executeUser\":\"evil\"}"));
    rejected(() -> service.resolveJob(request));
    request.setInput(new JSONObject());
    request
        .getJobConfig()
        .put(
            "executeParamMappingList",
            JSON.parseArray("[{\"key\":\"executeUser\",\"source\":\"input\"}]"));
    rejected(() -> service.resolveJob(request));
  }

  /** 场景限制在版本定义内，命名前缀复用字段值且不会把密码拼入名称。 */
  @Test
  public void scenarioAndNamePrefixFollowConfiguration() {
    AutoexecCombopScenarioVo scenario = new AutoexecCombopScenarioVo();
    scenario.setScenarioId(12L);
    scenario.setScenarioName("诊断");
    version.getConfig().setScenarioList(Collections.singletonList(scenario));
    version.getConfig().setDefaultScenarioId(12L);
    version.getConfig().getRuntimeParamList().add(parameter("serviceName", "text", 0));
    AutoexecQuickJobRequestVo request = request(false);
    request.getJobConfig().put("jobNamePrefix", "serviceName");
    request
        .getJobConfig()
        .put(
            "paramMappingList",
            JSON.parseArray(
                "[{\"key\":\"serviceName\",\"source\":\"context\",\"value\":\"serviceName\"}]"));
    request.getContext().put("serviceName", "mysql");
    JSONObject result = service.resolveJob(request);
    Assert.assertEquals("mysql_检查", result.getString("jobName"));
    Assert.assertEquals("12", result.getString("scenarioId"));
    request
        .getJobConfig()
        .put("scenarioMapping", JSON.parseObject("{\"source\":\"fixed\",\"value\":\"unknown\"}"));
    rejected(() -> service.resolveJob(request));
  }

  /** 敏感真实值仅进入创建参数，不返回窗口，也不提前加密。 */
  @Test
  public void createReadyUsesResolvedValuesWithoutStarting() throws Exception {
    version.getConfig().getRuntimeParamList().add(parameter("password", "password", 1));
    AutoexecQuickJobRequestVo request = request(true);
    request.setInput(JSON.parseObject("{\"param\":{\"password\":\"plain-secret\"}}"));
    Assert.assertFalse(service.resolveJob(request).toJSONString().contains("plain-secret"));
    final AutoexecJobVo[] created = new AutoexecJobVo[1];
    inject(
        service,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              created[0] = (AutoexecJobVo) a[0];
              created[0].setId(99L);
              return null;
            }));
    Assert.assertEquals(Long.valueOf(99), service.createReadyJobFromConfig(request));
    Assert.assertEquals("plain-secret", created[0].getParam().getString("password"));
    Assert.assertEquals("manual", created[0].getTriggerType());
    Assert.assertEquals(
        "192.0.2.1",
        created[0].getExecuteConfig().getExecuteNodeConfig().getInputNodeList().get(0).getIp());
    Assert.assertNull(created[0].getExecuteConfig().getExecuteNodeConfig().getParamList());
  }

  /** 元数据通过服务端契约只返回 Tagent 协议，不要求浏览器调用 CMDB 权限接口。 */
  @Test
  public void protocolMetadataContainsOnlyTagentStaticOptions() {
    final int[] reads = {0};
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) -> {
              if ("getAccountProtocolVoByProtocolId".equals(m.getName())) {
                return new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(
                    (Long) a[0], "tagent");
              }
              Assert.assertEquals("getAllAccountProtocolList", m.getName());
              reads[0]++;
              neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo tagent =
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(11L, "tagent");
              neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo variant =
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(
                      9007199254740993L, "tagent.secure");
              neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo ssh =
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(12L, "ssh");
              return Arrays.asList(tagent, variant, ssh);
            }));
    JSONObject tool = service.describe(1L);
    JSONObject protocol = null;
    for (Object item : tool.getJSONArray("executeParamList")) {
      JSONObject definition = (JSONObject) item;
      if ("protocolId".equals(definition.getString("key"))) {
        protocol = definition;
      }
    }
    Assert.assertNotNull(protocol);
    Assert.assertEquals(1, reads[0]);
    Assert.assertFalse(protocol.getJSONObject("config").containsKey("dynamicUrl"));
    JSONArray options = protocol.getJSONObject("config").getJSONArray("dataList");
    Assert.assertEquals(2, options.size());
    Assert.assertEquals("11", protocol.getString("value"));
    Assert.assertEquals("11", options.getJSONObject(0).get("value"));
    Assert.assertEquals("tagent", options.getJSONObject(0).getString("text"));
    Assert.assertEquals("9007199254740993", options.getJSONObject(1).get("value"));
  }

  /** 编辑器持久化 input 来源后，缺字段保留普通类型默认值，显式 null 仍表示清空。 */
  @Test
  public void absentInputMappingPreservesTypedDefaults() throws Exception {
    String[] types = {"file", "multiselect", "json", "switch"};
    JSONArray mappings = new JSONArray();
    for (String type : types) {
      AutoexecParamVo parameter = parameter(type, type, 0);
      parameter.setDefaultValue(sample(type));
      version.getConfig().getRuntimeParamList().add(parameter);
      JSONObject mapping = new JSONObject();
      mapping.put("key", type);
      mapping.put("source", "input");
      mappings.add(mapping);
    }
    AutoexecQuickJobRequestVo request = request(false);
    request.getJobConfig().put("paramMappingList", mappings);
    JSONObject draft = service.resolveJob(request);
    Assert.assertTrue(draft.getBooleanValue("complete"));
    for (Object item : draft.getJSONArray("paramList")) {
      JSONObject definition = (JSONObject) item;
      Object expected = service.normalizeValue(definition, sample(definition.getString("type")));
      Assert.assertEquals(JSON.toJSONString(expected), JSON.toJSONString(definition.get("value")));
    }
    final AutoexecJobVo[] created = {null};
    inject(
        service,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              created[0] = (AutoexecJobVo) a[0];
              created[0].setId(102L);
              return null;
            }));
    service.createReadyJobFromConfig(request);
    Assert.assertEquals(
        JSON.toJSONString(sample("file")), JSON.toJSONString(created[0].getParam().get("file")));
    JSONObject provided = new JSONObject();
    provided.put("file", null);
    JSONObject input = new JSONObject();
    input.put("param", provided);
    request.setInput(input);
    JSONObject cleared = service.resolveJob(request);
    Assert.assertNull(cleared.getJSONArray("paramList").getJSONObject(0).get("value"));
    service.createReadyJobFromConfig(request);
    Assert.assertTrue(created[0].getParam().containsKey("file"));
    Assert.assertNull(created[0].getParam().get("file"));
  }

  /** 工具敏感默认值不能替代人工输入；草稿不回显，完整解析必须重新填写。 */
  @Test
  public void sensitiveDefaultIsNeverUsedAsExecutionInput() throws Exception {
    AutoexecParamVo password = parameter("password", "password", 1);
    password.setDefaultValue("secret-tool-default");
    password.setValue("secret-version-value");
    AutoexecParamVo ordinary = parameter("ordinary", "text", 0);
    ordinary.setDefaultValue("normal-default");
    version.getConfig().getRuntimeParamList().add(password);
    version.getConfig().getRuntimeParamList().add(ordinary);
    AutoexecQuickJobRequestVo request = request(false);
    request
        .getJobConfig()
        .put(
            "paramMappingList",
            JSON.parseArray(
                "[{\"key\":\"password\",\"source\":\"input\"},{\"key\":\"ordinary\",\"source\":\"input\"}]"));
    JSONObject draft = service.resolveJob(request);
    Assert.assertFalse(draft.getBooleanValue("complete"));
    Assert.assertFalse(draft.toJSONString().contains("secret-"));
    Assert.assertEquals(
        "normal-default", draft.getJSONArray("paramList").getJSONObject(1).getString("value"));
    request.setComplete(true);
    rejected(() -> service.resolveJob(request));
    rejected(() -> service.createReadyJobFromConfig(request));
    Assert.assertEquals(0, service.targetChecks);
    request.setInput(JSON.parseObject("{\"param\":{\"password\":\"secret-user-input\"}}"));
    Assert.assertFalse(service.resolveJob(request).toJSONString().contains("secret-"));
    final AutoexecJobVo[] created = new AutoexecJobVo[1];
    inject(
        service,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              created[0] = (AutoexecJobVo) a[0];
              created[0].setId(100L);
              return null;
            }));
    Assert.assertEquals(Long.valueOf(100), service.createReadyJobFromConfig(request));
    Assert.assertEquals("secret-user-input", created[0].getParam().getString("password"));
    Assert.assertEquals("normal-default", created[0].getParam().getString("ordinary"));
    Assert.assertEquals("secret-tool-default", password.getDefaultValue());
  }

  /** 密码不能被执行用户或 Runner 摘要间接引用，错误也不能包含真实值。 */
  @Test
  public void sensitiveExecutionReferencesAreRejectedBeforeMetadataAndResolution() {
    AutoexecParamVo password = parameter("credential", "password", 1);
    password.setDefaultValue("secret-value");
    version.getConfig().getRuntimeParamList().add(password);
    AutoexecCombopExecuteConfigVo execute = version.getConfig().getExecuteConfig();
    for (String key : Arrays.asList("executeUser", "runnerGroup", "runnerGroupTag")) {
      execute.setExecuteUser(null);
      execute.setRunnerGroup(null);
      execute.setRunnerGroupTag(null);
      ParamMappingVo reference = new ParamMappingVo(key, "runtimeparam", "credential");
      if ("executeUser".equals(key)) {
        execute.setExecuteUser(reference);
      } else if ("runnerGroup".equals(key)) {
        execute.setRunnerGroup(reference);
      } else {
        execute.setRunnerGroupTag(reference);
      }
      try {
        service.describe(1L);
        Assert.fail();
      } catch (ParamIrregularException expected) {
        Assert.assertFalse(expected.getMessage().contains("secret-value"));
      }
      rejected(() -> service.resolveJob(request(false)));
      rejected(() -> service.createReadyJobFromConfig(request(true)));
    }
  }

  /** 前端根据元数据剔除固定和派生字段后的完整保存与执行载荷能够往返。 */
  @Test
  public void metadataToEditorConfigCanResolveAndCreateSingleJob() throws Exception {
    version.getConfig().getRuntimeParamList().add(parameter("osUser", "text", 1));
    version.getConfig().getRuntimeParamList().add(parameter("nodes", "node", 1));
    version.getConfig().getRuntimeParamList().add(parameter("password", "password", 1));
    AutoexecCombopExecuteConfigVo execute = version.getConfig().getExecuteConfig();
    execute.setExecuteUser(new ParamMappingVo("executeUser", "runtimeparam", "osUser"));
    execute.setRunnerGroup(new ParamMappingVo("runnerGroup", "constant", "1"));
    AutoexecCombopExecuteNodeConfigVo nodes = new AutoexecCombopExecuteNodeConfigVo();
    nodes.setParamList(Collections.singletonList("nodes"));
    execute.setExecuteNodeConfig(nodes);
    execute.setWhenToSpecify("runtimeparam");
    JSONObject metadata = service.describe(1L);
    AutoexecQuickJobRequestVo request = request(false);
    for (String list : Arrays.asList("runtimeParamList", "executeParamList")) {
      JSONArray mappings = new JSONArray();
      for (Object item : metadata.getJSONArray(list)) {
        JSONObject definition = (JSONObject) item;
        if (!definition.getBooleanValue("locked")
            && (definition.getBooleanValue("isFixed")
                || definition.getString("runtimeParamKey") != null)) {
          continue;
        }
        JSONObject mapping = new JSONObject();
        mapping.put("key", definition.getString("key"));
        mapping.put("source", "input");
        if (definition.getBooleanValue("locked")) {
          mapping.put("source", "context");
          mapping.put("value", "currentNode");
        }
        mappings.add(mapping);
      }
      if ("runtimeParamList".equals(list)) {
        request.getJobConfig().put("paramMappingList", mappings);
      } else {
        request.getJobConfig().put("executeParamMappingList", mappings);
      }
    }
    service.validateJobConfig(1L, request.getJobConfig());
    request.setInput(
        JSON.parseObject(
            "{\"param\":{\"osUser\":\"root\",\"password\":\"secret-value\"},\"executeParam\":{\"parallelPolicy\":\"roundCount\",\"roundCount\":\"1\"}}"));
    request.setComplete(true);
    JSONObject result = service.resolveJob(request);
    Assert.assertTrue(result.getBooleanValue("complete"));
    Assert.assertEquals("root", result.getString("executeUser"));
    Assert.assertFalse(result.toJSONString().contains("secret-value"));
    final AutoexecJobVo[] job = new AutoexecJobVo[1];
    inject(
        service,
        "actionService",
        proxy(
            AutoexecJobActionService.class,
            (p, m, a) -> {
              Assert.assertEquals("validateAndCreateJobFromCombop", m.getName());
              job[0] = (AutoexecJobVo) a[0];
              job[0].setId(101L);
              return null;
            }));
    Assert.assertEquals(Long.valueOf(101), service.createReadyJobFromConfig(request));
    Assert.assertEquals(Integer.valueOf(1), job[0].getRoundCount());
    Assert.assertEquals("root", job[0].getExecuteConfig().getExecuteUser().getValue());
    Assert.assertEquals("1", job[0].getRunnerGroup().getValue());
    Assert.assertEquals(
        "192.0.2.1",
        job[0].getExecuteConfig().getExecuteNodeConfig().getInputNodeList().get(0).getIp());
  }

  /** 原生结构不匹配的节点、文件与选择人值不能静默变成空值。 */
  @Test
  public void structuredTypesRejectIncompatibleObjects() {
    for (String type : Arrays.asList("node", "file", "userselect")) {
      rejected(
          () ->
              service.normalizeValue(
                  definition("value", type, 0), JSON.parseObject("{\"arbitrary\":1}")));
    }
  }

  /** 固定场景在保存时就必须属于激活版本，空值仍表示不选择场景。 */
  @Test
  public void fixedScenarioIsValidatedWhenSavingConfiguration() {
    AutoexecCombopScenarioVo scenario = new AutoexecCombopScenarioVo();
    scenario.setScenarioId(12L);
    scenario.setScenarioName("诊断");
    version.getConfig().setScenarioList(Collections.singletonList(scenario));
    AutoexecQuickJobRequestVo request = request(false);
    request
        .getJobConfig()
        .put("scenarioMapping", JSON.parseObject("{\"source\":\"fixed\",\"value\":\"unknown\"}"));
    rejected(() -> service.validateJobConfig(1L, request.getJobConfig()));
    for (Object value : Arrays.asList("12", "诊断", "", null)) {
      request.getJobConfig().getJSONObject("scenarioMapping").put("value", value);
      service.validateJobConfig(1L, request.getJobConfig());
    }
  }

  /** 固定必填值不能保存为空，人工或上下文待填仍可保存，可选固定空不受影响。 */
  @Test
  public void requiredFixedEmptyValuesAreRejectedWhenSavingConfiguration() {
    version.getConfig().getRuntimeParamList().add(parameter("requiredText", "text", 1));
    version.getConfig().getRuntimeParamList().add(parameter("requiredList", "multiselect", 1));
    version.getConfig().getRuntimeParamList().add(parameter("optionalText", "text", 0));
    AutoexecQuickJobRequestVo request = request(false);
    for (String mappings :
        Arrays.asList(
            "[{\"key\":\"requiredText\",\"source\":\"fixed\"}]",
            "[{\"key\":\"requiredText\",\"source\":\"fixed\",\"value\":\"\"}]",
            "[{\"key\":\"requiredList\",\"source\":\"fixed\",\"value\":[]}]")) {
      request.getJobConfig().put("paramMappingList", JSON.parseArray(mappings));
      rejected(() -> service.validateJobConfig(1L, request.getJobConfig()));
    }
    request
        .getJobConfig()
        .put(
            "paramMappingList",
            JSON.parseArray(
                "[{\"key\":\"optionalText\",\"source\":\"fixed\"},{\"key\":\"requiredText\",\"source\":\"input\"},{\"key\":\"requiredList\",\"source\":\"context\",\"value\":\"pending\"}]"));
    service.validateJobConfig(1L, request.getJobConfig());
  }

  /** 可配置的固定协议在保存时也限定 Tagent，不能通过直接 API 保存 SSH 编号。 */
  @Test
  public void fixedProtocolMappingRejectsNonTagentWhenSavingConfiguration() {
    version.getConfig().getExecuteConfig().setProtocolId(null);
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) -> {
              neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo tagent =
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(11L, "tagent");
              neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo ssh =
                  new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(12L, "ssh");
              if ("getAllAccountProtocolList".equals(m.getName())) {
                return Arrays.asList(tagent, ssh);
              }
              Assert.assertEquals("getAccountProtocolVoByProtocolId", m.getName());
              if (Long.valueOf(11).equals(a[0])) {
                return tagent;
              }
              return ssh;
            }));
    AutoexecQuickJobRequestVo request = request(false);
    request
        .getJobConfig()
        .put(
            "executeParamMappingList",
            JSON.parseArray("[{\"key\":\"protocolId\",\"source\":\"fixed\",\"value\":\"12\"}]"));
    rejected(() -> service.validateJobConfig(1L, request.getJobConfig()));
    request
        .getJobConfig()
        .getJSONArray("executeParamMappingList")
        .getJSONObject(0)
        .put("value", "11");
    service.validateJobConfig(1L, request.getJobConfig());
  }

  /** 协议缺失时描述仍可展示，固定用户是只读定义。 */
  @Test
  public void metadataIncludesExecutionScenarioAndTargetContract() {
    JSONObject metadata = service.describe(1L);
    Assert.assertEquals(8, metadata.getJSONArray("executeParamList").size());
    Assert.assertTrue(metadata.containsKey("scenarioList"));
    Assert.assertTrue(metadata.containsKey("phaseList"));
    Assert.assertTrue(metadata.containsKey("targetParamKeys"));
    Assert.assertEquals(2, service.getJobConfigVersion());
  }

  /** 版本与执行策略使用业务展示值，组和多标签名称只读查询，原执行配置不变。 */
  @Test
  public void displayMetadataUsesBusinessNamesWithoutChangingExecution() throws Exception {
    version.setVersion(7);
    AutoexecCombopExecuteConfigVo execute = version.getConfig().getExecuteConfig();
    execute.setRunnerGroup(new ParamMappingVo("runnerGroup", "constant", "2"));
    execute.setRunnerGroupTag(
        new ParamMappingVo("runnerGroupTag", "constant", "[\"100\",\"prod\"]"));
    execute.setParallelPolicy("roundCount");
    execute.setRoundCount(1);
    String original = JSON.toJSONString(execute);
    inject(
        service,
        "runnerMapper",
        proxy(
            neatlogic.framework.dao.mapper.runner.RunnerMapper.class,
            (p, m, a) -> {
              Assert.assertEquals("getRunnerGroupById", m.getName());
              Assert.assertEquals(Long.valueOf(2), a[0]);
              neatlogic.framework.dto.runner.RunnerGroupVo group =
                  new neatlogic.framework.dto.runner.RunnerGroupVo();
              group.setId(2L);
              group.setName("核心执行组");
              return group;
            }));
    final int[] tagReads = {0};
    inject(
        service,
        "tagMapper",
        proxy(
            neatlogic.framework.dao.mapper.TagMapper.class,
            (p, m, a) -> {
              Assert.assertEquals("getTagListByIdOrNameList", m.getName());
              Assert.assertEquals(Arrays.asList("100", "prod"), a[0]);
              tagReads[0]++;
              neatlogic.framework.dto.TagVo first =
                  new neatlogic.framework.dto.TagVo("ops", "runnergroup");
              first.setId(100L);
              neatlogic.framework.dto.TagVo second =
                  new neatlogic.framework.dto.TagVo("prod", "runnergroup");
              second.setId(101L);
              return Arrays.asList(second, first);
            }));
    JSONObject metadata = service.describe(1L);
    Assert.assertEquals("v7", metadata.getString("versionText"));
    Assert.assertEquals("核心执行组", metadata.getString("runnerGroupName"));
    Assert.assertEquals(
        Arrays.asList("ops", "prod"), metadata.getJSONArray("runnerGroupTagNameList"));
    Assert.assertEquals(1, tagReads[0]);
    tagReads[0] = 0;
    JSONObject resolved = service.resolveJob(request(false));
    Assert.assertEquals("v7", resolved.getString("versionText"));
    Assert.assertEquals("核心执行组", resolved.getString("runnerGroupName"));
    Assert.assertEquals(1, tagReads[0]);
    Assert.assertEquals(
        neatlogic.framework.autoexec.constvalue.AutoexecParallelPolicy.ROUND_COUNT.getName(),
        resolved.getString("parallelPolicyText"));
    Assert.assertEquals("roundCount", resolved.getString("parallelPolicy"));
    Assert.assertEquals("2", resolved.getString("runnerGroup"));
    Assert.assertEquals(original, JSON.toJSONString(execute));
    Assert.assertFalse(resolved.toJSONString().contains("passwordCipher"));
  }

  /** 随机分配有本地名称，未知组和标签不以原始编号兜底。 */
  @Test
  public void displayMetadataHandlesRandomUnknownAndUnfilledValues() throws Exception {
    AutoexecCombopExecuteConfigVo execute = version.getConfig().getExecuteConfig();
    execute.setRunnerGroup(new ParamMappingVo("runnerGroup", "constant", "-1"));
    JSONObject random = service.describe(1L);
    Assert.assertEquals(
        neatlogic.framework.util.$.t("autoexec.quicktool.randomRunnerGroup"),
        random.getString("runnerGroupName"));
    Assert.assertFalse("-1".equals(random.getString("runnerGroupName")));
    execute.setRunnerGroup(new ParamMappingVo("runnerGroup", "constant", "99"));
    execute.setRunnerGroupTag(new ParamMappingVo("runnerGroupTag", "constant", "[\"unknown\"]"));
    JSONObject unknown = service.describe(1L);
    Assert.assertNull(unknown.getString("runnerGroupName"));
    Assert.assertTrue(unknown.getJSONArray("runnerGroupTagNameList").isEmpty());
    execute.setRunnerGroup(null);
    execute.setRunnerGroupTag(null);
    JSONObject unfilled = service.describe(1L);
    Assert.assertNull(unfilled.getString("runnerGroupName"));
    Assert.assertNull(unfilled.getString("versionText"));
    Assert.assertNull(unfilled.getString("parallelPolicyText"));
  }

  /** 协议展示要求返回记录编号与选择一致，不能用其他协议或编号作为名称。 */
  @Test
  public void protocolDisplayRequiresMatchingProtocolId() {
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) ->
                new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(12L, "wrong")));
    Assert.assertNull(service.protocolName(11L));
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) ->
                new neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo(11L, "tagent")));
    Assert.assertEquals("tagent", service.protocolName(11L));
    Assert.assertNull(service.protocolName(null));
  }

  /** 后端拥有的字段文案中英文键保持一致，元数据不能返回前端专用翻译键。 */
  @Test
  public void executionLabelsExistInOwnedChineseAndEnglishResources() throws Exception {
    JSONObject zh = quickToolLabels("zh");
    JSONObject en = quickToolLabels("en");
    Set<String> expected =
        new HashSet<>(
            Arrays.asList(
                "executeNodeConfig",
                "protocolId",
                "executeUser",
                "runnerGroup",
                "runnerGroupTag",
                "parallelPolicy",
                "roundCount",
                "parallelCount"));
    Assert.assertEquals(expected, zh.keySet());
    Assert.assertEquals(expected, en.keySet());
    for (String key : expected) {
      Assert.assertFalse(zh.getString(key).trim().isEmpty());
      Assert.assertFalse(en.getString(key).trim().isEmpty());
    }
    Assert.assertEquals("执行目标", zh.getString("executeNodeConfig"));
    Assert.assertEquals("Connection Protocol", en.getString("protocolId"));
    for (Object item : service.describe(1L).getJSONArray("executeParamList")) {
      JSONObject row = (JSONObject) item;
      Assert.assertTrue(expected.contains(row.getString("key")));
      Assert.assertFalse(row.getString("name").startsWith("term."));
      Assert.assertFalse(row.getString("name").startsWith("autoexec.quicktool.field."));
    }
  }

  /** 从模块真实目录打包的语言资源读取文案，不依赖前端资源或测试硬编码键。 */
  private JSONObject quickToolLabels(String language) throws Exception {
    String path = "neatlogic/resources/autoexec/i18n/language_" + language + ".json";
    try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
      Assert.assertNotNull(path, input);
      java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1) {
        bytes.write(buffer, 0, count);
      }
      return JSON.parseObject(
              new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8))
          .getJSONObject("autoexec")
          .getJSONObject("quicktool")
          .getJSONObject("field");
    }
  }

  /** 引擎已生效的并发默认值必须同时回填可编辑定义，避免摘要有值而表单为空。 */
  @Test
  public void effectiveParallelDefaultsAreReturnedInEditableMetadata() {
    JSONObject result = service.resolveJob(request(false));
    Assert.assertEquals("parallel", result.getString("parallelPolicy"));
    Assert.assertEquals("parallel", executionValue(result, "parallelPolicy"));
    Assert.assertEquals(Integer.valueOf(32), result.get("parallelCount"));
    Assert.assertEquals(Integer.valueOf(32), executionValue(result, "parallelCount"));
    Assert.assertNull(result.get("roundCount"));
    Assert.assertNull(executionValue(result, "roundCount"));
  }

  /** 生效数量使用原生整数，非当前策略数量保留人工值，来回切换不会覆盖为 null。 */
  @Test
  public void explicitCountsPreserveInactiveInputWhenSwitchingPolicy() {
    AutoexecQuickJobRequestVo request = request(false);
    request.setInput(
        JSON.parseObject(
            "{\"executeParam\":{\"parallelPolicy\":\"parallel\",\"parallelCount\":5,\"roundCount\":7}}"));
    JSONObject parallel = service.resolveJob(request);
    Assert.assertEquals(Integer.valueOf(5), parallel.get("parallelCount"));
    Assert.assertEquals(Integer.valueOf(5), executionValue(parallel, "parallelCount"));
    Assert.assertNull(parallel.get("roundCount"));
    Assert.assertEquals("7", String.valueOf(executionValue(parallel, "roundCount")));
    request.getInput().getJSONObject("executeParam").put("parallelPolicy", "roundCount");
    JSONObject rounds = service.resolveJob(request);
    Assert.assertEquals(Integer.valueOf(7), rounds.get("roundCount"));
    Assert.assertEquals(Integer.valueOf(7), executionValue(rounds, "roundCount"));
    Assert.assertNull(rounds.get("parallelCount"));
    Assert.assertEquals("5", String.valueOf(executionValue(rounds, "parallelCount")));
  }

  /** 分批默认值为 64，窗口回传已回填的原生值后完整解析保持同样配置。 */
  @Test
  public void roundDefaultRemainsStableAfterFormSubmitsNativeValue() {
    AutoexecQuickJobRequestVo request = request(false);
    request.setInput(JSON.parseObject("{\"executeParam\":{\"parallelPolicy\":\"roundCount\"}}"));
    JSONObject draft = service.resolveJob(request);
    Assert.assertEquals(Integer.valueOf(64), draft.get("roundCount"));
    Assert.assertEquals(Integer.valueOf(64), executionValue(draft, "roundCount"));
    JSONObject form = new JSONObject();
    form.put("parallelPolicy", executionValue(draft, "parallelPolicy"));
    form.put("roundCount", executionValue(draft, "roundCount"));
    request.getInput().put("executeParam", form);
    request.setComplete(true);
    JSONObject complete = service.resolveJob(request);
    Assert.assertTrue(complete.getBooleanValue("complete"));
    Assert.assertEquals(draft.get("roundCount"), complete.get("roundCount"));
    Assert.assertEquals(
        executionValue(draft, "roundCount"), executionValue(complete, "roundCount"));
    Assert.assertNull(complete.get("parallelCount"));
  }

  /** 按稳定字段名读取执行定义，断言真实元数据而不是复制生产默认算法。 */
  private Object executionValue(JSONObject result, String key) {
    for (Object item : result.getJSONArray("executeParamList")) {
      JSONObject definition = (JSONObject) item;
      if (key.equals(definition.getString("key"))) {
        return definition.get("value");
      }
    }
    Assert.fail("缺少执行字段：" + key);
    return null;
  }

  /** 每种输入样例使用原生编辑器/流程已经建立的值结构。 */
  private Object sample(String type) {
    if (Arrays.asList("multiselect", "checkbox", "runnergrouptag").contains(type)) {
      return JSON.parseArray("[\"a\",\"b\"]");
    }
    if ("node".equals(type)) {
      return JSON.parseArray("[{\"ip\":\"192.0.2.1\"}]");
    }
    if ("userselect".equals(type)) {
      return JSON.parseArray("[\"user#01234567890123456789012345678901\"]");
    }
    if ("file".equals(type)) {
      return JSON.parseObject(
          "{\"fileIdList\":[\"1\"],\"fileList\":[{\"id\":\"1\",\"name\":\"test\"}]}");
    }
    if ("json".equals(type)) {
      return JSON.parseObject("{\"test\":1}");
    }
    if ("switch".equals(type)) {
      return Boolean.TRUE;
    }
    if ("account".equals(type)) {
      return "12";
    }
    if ("runnergroup".equals(type)) {
      return "1";
    }
    return "value";
  }

  /** 构造最小新版请求，宿主机上下文与入口由集成模块产生。 */
  private AutoexecQuickJobRequestVo request(boolean complete) {
    AutoexecQuickJobRequestVo request = new AutoexecQuickJobRequestVo();
    request.setCombopId(1L);
    request.setVersionId(2L);
    request.setEntryName("检查");
    request.setHost("192.0.2.1");
    request.setJobConfig(
        JSON.parseObject(
            "{\"schemaVersion\":2,\"createJobPolicy\":\"single\",\"paramMappingList\":[],\"executeParamMappingList\":[]}"));
    request.setContext(JSON.parseObject("{\"currentNode\":[{\"ip\":\"192.0.2.1\"}]}"));
    request.setInput(new JSONObject());
    request.setComplete(complete);
    return request;
  }

  /** 复用标准参数定义，不添加测试专属生产装配入口。 */
  private JSONObject definition(String key, String type, int required) {
    JSONObject p = new JSONObject();
    p.put("key", key);
    p.put("type", type);
    p.put("isRequired", required);
    p.put("name", key);
    return p;
  }

  /** DTO 与元数据使用相同类型字段。 */
  private AutoexecParamVo parameter(String key, String type, int required) {
    AutoexecParamVo p = new AutoexecParamVo();
    p.setId(100L);
    p.setKey(key);
    p.setType(type);
    p.setIsRequired(required);
    p.setName(key);
    return p;
  }

  /** 外部检查使用替身计数，真实映射和参数转换保持生产实现。 */
  private class Resolver extends AutoexecQuickExecutionServiceImpl {
    private int targetChecks;

    @Override
    protected AutoexecCombopVersionVo active(Long id) {
      return version;
    }

    @Override
    protected void validateResolvedTarget(AutoexecCombopVersionConfigVo config, String host) {
      targetChecks++;
    }
  }

  /** 业务约束失败必须是框架参数异常，不能以 NPE 冒充有效拒绝。 */
  private void rejected(Runnable operation) {
    try {
      operation.run();
      Assert.fail("应拒绝无效配置");
    } catch (ParamIrregularException expected) {
    } catch (neatlogic.framework.autoexec.exception.job.JobParamValidException expected) {
    }
  }

  /** 仅在测试中替换依赖。 */
  private void inject(Object target, String name, Object value) throws Exception {
    Field field = AutoexecQuickExecutionServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** 创建只提供当前测试需要行为的接口替身。 */
  private <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
