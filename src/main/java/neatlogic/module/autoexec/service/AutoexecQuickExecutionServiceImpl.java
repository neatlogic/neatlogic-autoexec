package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.crossover.IAutoexecQuickExecutionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecQuickJobRequestVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountBaseVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.TagMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.TagVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecQuickExecutionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/** 可选模块复用的单主机 Tagent 执行适配，拒绝阶段私有目标及嵌套组合工具。 */
@Service
public class AutoexecQuickExecutionServiceImpl implements IAutoexecQuickExecutionCrossoverService {
  @Resource private AutoexecCombopMapper combopMapper;
  @Resource private AutoexecCombopVersionMapper versionMapper;
  @Resource private AutoexecJobMapper jobMapper;
  @Resource private AutoexecCombopService combopService;
  @Resource private AutoexecJobActionService actionService;
  @Resource private AutoexecService autoexecService;
  @Resource private TagentMapper tagentMapper;
  @Resource private UserMapper userMapper;
  @Resource private RunnerMapper runnerMapper;
  @Resource private TagMapper tagMapper;
  @Resource private AutoexecQuickExecutionMapper quickExecutionMapper;

  /** 新版作业配置能力可用，供可选集成模块在滚动升级时探测。 */
  @Override
  public int getJobConfigVersion() {
    return 2;
  }

  /** 基础权限不能由调用方的功能权限替代。 */
  protected void checkBase() {
    if (!AuthActionChecker.check(AUTOEXEC_BASE.class)) {
      throw invalid("autoexec permission");
    }
  }

  /** 复用组合工具所有者、管理权限和对象授权规则。 */
  protected AutoexecCombopVersionVo active(Long id) {
    checkBase();
    AutoexecCombopVo tool = combopMapper.getAutoexecCombopById(id);
    if (tool == null
        || !Objects.equals(tool.getIsActive(), 1)
        || !combopService.checkOperableButton(tool, CombopAuthorityAction.EXECUTE)) {
      throw invalid("combop unavailable");
    }
    AutoexecCombopVersionVo version = versionMapper.getAutoexecCombopActiveVersionByCombopId(id);
    if (version == null) {
      throw invalid("active version");
    }
    validateConfig(version.getConfig());
    return version;
  }

  /** 首期仅接受全局运行时目标，阶段必须继承，避免过滤器及上游输出扩大目标。 */
  protected void validateConfig(AutoexecCombopVersionConfigVo config) {
    validateConfigStructure(config);
    if (config.getExecuteConfig().getProtocolId() != null) {
      validateProtocol(config.getExecuteConfig().getProtocolId());
    }
  }

  /** 纯配置约束由列表批量筛选和执行前完整校验共用，不逐工具访问数据库。 */
  protected void validateConfigStructure(AutoexecCombopVersionConfigVo config) {
    if (config == null
        || config.getCombopPhaseList() == null
        || config.getCombopPhaseList().isEmpty()) {
      throw invalid("combop phases");
    }
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    if (execute == null) {
      throw invalid("execution config");
    }
    String mode = execute.getWhenToSpecify();
    AutoexecCombopExecuteNodeConfigVo nodes = execute.getExecuteNodeConfig();
    if (mode != null
        && !CombopNodeSpecify.RUNTIME.getValue().equals(mode)
        && !CombopNodeSpecify.RUNTIMEPARAM.getValue().equals(mode)) {
      throw invalid("runtime target");
    }
    if (CombopNodeSpecify.RUNTIMEPARAM.getValue().equals(mode)) {
      if (nodes == null || nodes.getParamList() == null || nodes.getParamList().isEmpty()) {
        throw invalid("target parameter");
      }
      Set<String> nodeKeys = new HashSet<>();
      if (config.getRuntimeParamList() != null) {
        for (AutoexecParamVo parameter : config.getRuntimeParamList()) {
          if (ParamType.NODE.getValue().equals(parameter.getType())) {
            nodeKeys.add(parameter.getKey());
          }
        }
      }
      if (!nodeKeys.containsAll(nodes.getParamList())) {
        throw invalid("target parameter");
      }
    } else if (nodes != null && nodes.getParamList() != null && !nodes.getParamList().isEmpty()) {
      throw invalid("target parameter");
    }
    if (nodes != null
        && (nodes.getInputNodeList() != null && !nodes.getInputNodeList().isEmpty()
            || nodes.getSelectNodeList() != null && !nodes.getSelectNodeList().isEmpty()
            || nodes.getFilter() != null && !nodes.getFilter().isEmpty()
            || nodes.getOtherFilter() != null && !nodes.getOtherFilter().isEmpty()
            || nodes.getPreOutputList() != null && !nodes.getPreOutputList().isEmpty())) {
      throw invalid("runtime target");
    }
    validateExecutionReferences(config);
    // 灰度分组也能覆盖执行目标，因此与阶段覆盖采用同一拒绝策略。
    if (config.getCombopGroupList() != null) {
      for (AutoexecCombopGroupVo group : config.getCombopGroupList()) {
        if (group.getConfig() != null
            && hasGroupExecutionOverride(group.getConfig().getExecuteConfig())) {
          throw invalid("group execution override");
        }
      }
    }
    for (AutoexecCombopPhaseVo phase : config.getCombopPhaseList()) {
      if (!ExecMode.TARGET.getValue().equals(phase.getExecMode())
          && !ExecMode.RUNNER_TARGET.getValue().equals(phase.getExecMode())) {
        throw invalid("target phase");
      }
      if (phase.getConfig() == null
          || hasPhaseExecutionOverride(phase.getConfig().getExecuteConfig())) {
        throw invalid("phase execution override");
      }
      if (phase.getConfig().getPhaseOperationList() == null) {
        throw invalid("phase operations");
      }
      validateOperations(phase.getConfig().getPhaseOperationList());
    }
  }

  /** 阶段执行配置只有预设开关为 1 时生效，兼容编辑器保存的空对象及关闭后的残留值。 */
  protected boolean hasPhaseExecutionOverride(AutoexecCombopExecuteConfigVo execute) {
    if (execute == null) {
      return false;
    }
    Integer preset = execute.getIsPresetExecuteConfig();
    if (preset == null || Integer.valueOf(0).equals(preset)) {
      return false;
    }
    return true;
  }

  /** 灰度组不使用阶段的预设开关，只有完全继承的占位配置可以放行。 */
  protected boolean hasGroupExecutionOverride(AutoexecCombopExecuteConfigVo execute) {
    if (execute == null) {
      return false;
    }
    if (execute.getProtocolId() != null
        || StringUtils.isNotBlank(execute.getProtocol())
        || execute.getProtocolPort() != null
        || execute.getExecuteUser() != null
            && execute.getExecuteUser().getValue() != null
            && StringUtils.isNotBlank(String.valueOf(execute.getExecuteUser().getValue()))
        || execute.getExecuteNodeConfig() != null && !execute.getExecuteNodeConfig().isNull()
        || execute.getRunnerGroup() != null
        || execute.getRunnerGroupTag() != null
        || StringUtils.isNotBlank(execute.getWhenToSpecify())
        || execute.getRoundCount() != null
        || execute.getParallelCount() != null
        || StringUtils.isNotBlank(execute.getParallelPolicy())
        || execute.getPreCondition() != null && !execute.getPreCondition().isEmpty()
        || execute.getWhitelist() != null && !execute.getWhitelist().isEmpty()
        || execute.getBlacklist() != null && !execute.getBlacklist().isEmpty()
        || execute.getIsPresetExecuteConfig() != null
            && !Integer.valueOf(0).equals(execute.getIsPresetExecuteConfig())) {
      return true;
    }
    return false;
  }

  /** 控制块中的子操作递归检查，避免嵌套组合工具藏在循环或条件分支内。 */
  protected void validateOperations(List<AutoexecCombopPhaseOperationVo> operations) {
    for (AutoexecCombopPhaseOperationVo operation : operations) {
      if (!CombopOperationType.SCRIPT.getValue().equals(operation.getOperationType())
          && !CombopOperationType.TOOL.getValue().equals(operation.getOperationType())) {
        throw invalid("nested operation");
      }
      if (operation.getConfig() != null && operation.getConfig().getOperations() != null) {
        validateOperations(operation.getConfig().getOperations());
      }
    }
  }

  /** 从现有账户协议注册信息判断 Tagent，不信任配置中的协议展示文本。 */
  protected void validateProtocol(Long protocolId) {
    IResourceAccountCrossoverMapper accounts =
        CrossoverServiceFactory.tryToGetApi(IResourceAccountCrossoverMapper.class);
    if (accounts == null) {
      throw invalid("account capability");
    }
    AccountProtocolVo protocol = accounts.getAccountProtocolVoByProtocolId(protocolId);
    if (protocol == null
        || protocol.getName() == null
        || !("tagent".equals(protocol.getName()) || protocol.getName().startsWith("tagent."))) {
      throw invalid("Tagent protocol");
    }
  }

  /** 用稳定字段提供预览，连接凭据不离开自动化模块。 */
  @Override
  public JSONObject describe(Long combopId) {
    AutoexecCombopVersionVo version = active(combopId);
    JSONObject result = new JSONObject();
    result.put("id", String.valueOf(combopId));
    result.put("combopId", String.valueOf(combopId));
    result.put("name", version.getName());
    result.put("versionId", String.valueOf(version.getId()));
    AutoexecCombopVersionConfigVo config = version.getConfig();
    JSONArray runtime = safeRuntimeParams(config.getRuntimeParamList());
    Set<String> targetKeys = targetParamKeys(config);
    for (Object item : runtime) {
      JSONObject definition = (JSONObject) item;
      definition.put("locked", targetKeys.contains(definition.getString("key")));
    }
    result.put("runtimeParamList", runtime);
    JSONArray executeDefinitions = executionDefinitions(config);
    result.put("executeParamList", executeDefinitions);
    JSONObject fixedValues = new JSONObject();
    for (Object item : executeDefinitions) {
      JSONObject definition = (JSONObject) item;
      if (definition.getBooleanValue("isFixed")) {
        fixedValues.put(definition.getString("key"), definition.get("value"));
      }
    }
    putExecutionDisplay(result, version, fixedValues);
    result.put("targetParamKeys", targetKeys);
    putScenarioMetadata(result, config);
    return result;
  }

  /** 场景和阶段元数据直接复用当前版本，不重复读取执行器组与标签名称。 */
  private void putScenarioMetadata(JSONObject result, AutoexecCombopVersionConfigVo config) {
    JSONArray scenarios = new JSONArray();
    if (config.getScenarioList() != null) {
      for (AutoexecCombopScenarioVo scenario : config.getScenarioList()) {
        JSONObject row = new JSONObject();
        row.put("scenarioId", String.valueOf(scenario.getScenarioId()));
        row.put("scenarioName", scenario.getScenarioName());
        row.put("combopPhaseNameList", scenario.getCombopPhaseNameList());
        scenarios.add(row);
      }
    }
    result.put("scenarioList", scenarios);
    if (config.getDefaultScenarioId() != null) {
      result.put("defaultScenarioId", String.valueOf(config.getDefaultScenarioId()));
    }
    JSONArray phases = new JSONArray();
    for (AutoexecCombopPhaseVo phase : config.getCombopPhaseList()) {
      JSONObject row = new JSONObject();
      row.put("name", phase.getName());
      row.put("execMode", phase.getExecMode());
      phases.add(row);
    }
    result.put("phaseList", phases);
  }

  /** 仅清洗用于展示的独立副本，不改变后续创建作业使用的参数默认值。 */
  protected JSONArray safeRuntimeParams(List<AutoexecParamVo> parameters) {
    JSONArray result = JSON.parseArray(JSON.toJSONString(parameters));
    if (result == null) {
      return new JSONArray();
    }
    for (int index = 0; index < result.size(); index++) {
      JSONObject parameter = result.getJSONObject(index);
      if (ParamType.PASSWORD.getValue().equalsIgnoreCase(parameter.getString("type"))
          || parameter.getBooleanValue("isSecret")
          || parameter.getBooleanValue("isSensitive")) {
        clearSensitiveValues(parameter);
      }
    }
    return result;
  }

  /** 敏感定义中的顶层和嵌套默认值都不能通过参数配置旁路返回。 */
  private void clearSensitiveValues(Object definition) {
    if (definition instanceof JSONObject) {
      JSONObject object = (JSONObject) definition;
      for (String key :
          Arrays.asList("value", "defaultValue", "default", "defaultValueStr", "configStr")) {
        object.remove(key);
      }
      for (Object child : object.values()) {
        clearSensitiveValues(child);
      }
    } else if (definition instanceof JSONArray) {
      for (Object child : (JSONArray) definition) {
        clearSensitiveValues(child);
      }
    }
  }

  /** 权限与激活版本由 SQL 批量筛选，配置准入后准确计数，仅保留当前页摘要。 */
  @Override
  public JSONObject listTools(JSONObject request) {
    checkBase();
    int current = 1;
    int size = 20;
    if (request.getInteger("currentPage") != null) {
      current = request.getInteger("currentPage");
    }
    if (request.getInteger("pageSize") != null) {
      size = request.getInteger("pageSize");
    }
    if (current < 1 || size < 1 || size > 100) {
      throw invalid("pagination");
    }
    List<Long> defaults = Collections.emptyList();
    if (request.getJSONArray("defaultValue") != null) {
      defaults =
          new ArrayList<>(
              new LinkedHashSet<>(request.getJSONArray("defaultValue").toJavaList(Long.class)));
    }
    AuthenticationInfoVo auth = UserContext.get().getAuthenticationInfoVo();
    boolean all = hasAllToolAuthority();
    String keyword = request.getString("keyword");
    if (!defaults.isEmpty()) {
      // 选中值回显沿用标准组合工具列表行为，不受当前搜索关键字影响。
      keyword = null;
    }
    int candidates = quickExecutionMapper.countCandidates(auth, all, keyword, defaults);
    Set<Long> protocols = Collections.emptySet();
    if (candidates > 0) {
      protocols = tagentProtocolIds();
    }
    long offset = (long) (current - 1) * size;
    int eligible = 0;
    int examined = 0;
    Long cursor = null;
    List<JSONObject> rows = new ArrayList<>();
    // 回显只查询选中编号，按请求顺序返回；普通搜索采用稳定的编号倒序游标。
    Map<Long, JSONObject> selected = new HashMap<>();
    while (examined < candidates && !protocols.isEmpty()) {
      List<AutoexecCombopVersionVo> batch =
          quickExecutionMapper.searchCandidates(auth, all, keyword, defaults, cursor, 200);
      if (batch.isEmpty()) {
        break;
      }
      examined += batch.size();
      for (AutoexecCombopVersionVo version : batch) {
        AutoexecCombopVersionConfigVo config = version.getConfig();
        try {
          validateConfigStructure(config);
        } catch (ParamIrregularException ex) {
          continue;
        }
        if (config.getExecuteConfig().getProtocolId() != null
            && !protocols.contains(config.getExecuteConfig().getProtocolId())) {
          continue;
        }
        if (!defaults.isEmpty()) {
          selected.put(version.getCombopId(), toolSummary(version));
        } else if (eligible >= offset && rows.size() < size) {
          rows.add(toolSummary(version));
        }
        eligible++;
      }
      cursor = batch.get(batch.size() - 1).getCombopId();
      if (batch.size() < 200) {
        break;
      }
    }
    if (!defaults.isEmpty()) {
      for (Long id : defaults) {
        if (selected.containsKey(id)) {
          rows.add(selected.get(id));
        }
      }
    }
    JSONObject result = new JSONObject();
    result.put("pageCount", (eligible + size - 1) / size);
    result.put("rowNum", eligible);
    result.put("currentPage", current);
    result.put("pageSize", size);
    result.put("tbodyList", rows);
    return result;
  }

  /** 管理权限只决定候选 SQL 范围，不能绕过工具准入和后续执行鉴权。 */
  protected boolean hasAllToolAuthority() {
    return AuthActionChecker.check(AUTOEXEC_MODIFY.class);
  }

  /** 账户协议为小规模元数据，每个列表请求只读取一次，不缓存用户权限或工具配置。 */
  protected Set<Long> tagentProtocolIds() {
    IResourceAccountCrossoverMapper accounts =
        CrossoverServiceFactory.tryToGetApi(IResourceAccountCrossoverMapper.class);
    if (accounts == null) {
      throw invalid("account capability");
    }
    Set<Long> result = new HashSet<>();
    for (AccountProtocolVo protocol : accounts.getAllAccountProtocolList()) {
      if ("tagent".equals(protocol.getName())
          || protocol.getName() != null && protocol.getName().startsWith("tagent.")) {
        result.add(protocol.getId());
      }
    }
    return result;
  }

  /** 列表不加载运行参数；选中工具后由 describe 返回脱敏参数定义。 */
  private JSONObject toolSummary(AutoexecCombopVersionVo version) {
    JSONObject row = new JSONObject();
    row.put("id", String.valueOf(version.getCombopId()));
    row.put("combopId", String.valueOf(version.getCombopId()));
    row.put("versionId", String.valueOf(version.getId()));
    row.put("name", version.getName());
    return row;
  }

  /** 按现有主 IP 优先规则预检凭据，并保证地址唯一对应 Tagent 与 Runner。 */
  @Override
  public void validateTarget(Long combopId, Long versionId, String host) {
    AutoexecCombopVersionVo version = active(combopId);
    if (!Objects.equals(versionId, version.getId())) {
      throw invalid("version changed");
    }
    validateResolvedTarget(version.getConfig(), host);
  }

  /** 有效配置已解析后才检查协议、连接凭据及实际路由。 */
  protected void validateResolvedTarget(AutoexecCombopVersionConfigVo config, String host) {
    validateProtocol(config.getExecuteConfig().getProtocolId());
    if (StringUtils.isBlank(host)
        || !host.equals(host.trim())
        || "0.0.0.0".equals(host)
        || "127.0.0.1".equals(host)
        || "::".equals(host)
        || "::1".equals(host)) {
      throw invalid("host");
    }
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    List<AccountBaseVo> accounts =
        tagentMapper.getAccountListByMainIpListAndProtocolId(
            Collections.singletonList(host), execute.getProtocolId());
    if (accounts == null || accounts.isEmpty()) {
      accounts =
          tagentMapper.getAccountListByIncludeIpListAndProtocolId(
              Collections.singletonList(host), execute.getProtocolId());
    }
    // Tagent 账户保存连接凭据，名称不代表执行脚本时使用的操作系统用户。
    if (accounts == null || accounts.size() != 1) {
      throw invalid("unique Tagent account");
    }
    List<TagentVo> agents =
        tagentMapper.getTagentByIpOrTagentIpAndPort(host, accounts.get(0).getProtocolPort());
    if (agents == null || agents.size() != 1) {
      throw invalid("unique Tagent route");
    }
    validateRunnerRoutes(config, host);
  }

  /** 与实际目标作业共用规则、标签和网段路由，不读取 Tagent 数据库中的历史 Runner 标识。 */
  protected void validateRunnerRoutes(AutoexecCombopVersionConfigVo config, String host) {
    IAutoexecJobSourceTypeHandler handler =
        AutoexecJobSourceTypeHandlerFactory.getAction(JobSourceType.AUTOEXEC.getValue());
    if (handler == null) {
      throw invalid("Tagent runner");
    }
    AutoexecJobVo preview = new AutoexecJobVo();
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    preview.setRunnerGroup(execute.getRunnerGroup());
    preview.setRunnerGroupTag(execute.getRunnerGroupTag());
    // 这里只读取参数默认值供路由标签解析，不修改激活版本或创建任何作业数据。
    List<AutoexecParamVo> parameters = new ArrayList<>();
    if (config.getRuntimeParamList() != null) {
      parameters =
          JSON.parseArray(JSON.toJSONString(config.getRuntimeParamList()), AutoexecParamVo.class);
      for (AutoexecParamVo parameter : parameters) {
        if (parameter.getValue() == null) {
          parameter.setValue(parameter.getDefaultValue());
        }
      }
    }
    preview.setRunTimeParamList(parameters);
    for (AutoexecCombopPhaseVo phase : config.getCombopPhaseList()) {
      AutoexecJobPhaseVo previewPhase = new AutoexecJobPhaseVo();
      previewPhase.setExecMode(phase.getExecMode());
      AutoexecJobNodeVo node = new AutoexecJobNodeVo();
      node.setHost(host);
      previewPhase.setCurrentNode(node);
      preview.setExecutePhase(previewPhase);
      // 准入已限定为 target/runner_target；既有契约的这两个分支只查询候选，不更新阶段。
      List<RunnerMapVo> candidates = handler.getRunnerMapList(preview, phase.getConfig());
      boolean available = false;
      if (candidates != null) {
        for (RunnerMapVo candidate : candidates) {
          if (candidate != null && StringUtils.isNotBlank(candidate.getUrl())) {
            available = true;
            break;
          }
        }
      }
      if (!available) {
        throw invalid("Tagent runner");
      }
    }
  }

  /** 必须处于调用方事务；创建与来源关联一起提交后才能调用启动。 */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Long createReadyJob(
      Long combopId, Long versionId, String name, String host, JSONObject param) {
    validateTarget(combopId, versionId, host);
    AutoexecCombopVersionVo version = active(combopId);
    if (!Objects.equals(versionId, version.getId())) {
      throw invalid("version changed");
    }
    validateParams(version.getConfig(), param);
    AutoexecCombopExecuteConfigVo execute =
        JSON.parseObject(
            JSON.toJSONString(version.getConfig().getExecuteConfig()),
            AutoexecCombopExecuteConfigVo.class);
    AutoexecNodeVo node = new AutoexecNodeVo();
    node.setIp(host);
    AutoexecCombopExecuteNodeConfigVo nodes = new AutoexecCombopExecuteNodeConfigVo();
    nodes.setInputNodeList(Collections.singletonList(node));
    execute.setExecuteNodeConfig(nodes);
    AutoexecJobVo job = new AutoexecJobVo();
    job.setOperationId(combopId);
    job.setOperationType(CombopOperationType.COMBOP.getValue());
    job.setSource(JobSource.COMBOP.getValue());
    job.setCombopVersionId(versionId);
    job.setInvokeId(versionId);
    job.setRouteId(versionId.toString());
    job.setName(name);
    job.setPlanStartTime(new Date());
    job.setTriggerType(JobTriggerType.MANUAL.getValue());
    job.setExecuteConfig(execute);
    job.setWhenToSpecify(CombopNodeSpecify.RUNTIME.getValue());
    job.setParam(param);
    job.setActionParam(new JSONObject());
    try {
      actionService.validateAndCreateJobFromCombop(job);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("创建快捷作业失败", ex);
    }
    return job.getId();
  }

  /** 快捷调用始终验证运行参数，不受租户关闭通用作业参数检查的影响。 */
  protected void validateParams(AutoexecCombopVersionConfigVo config, JSONObject values) {
    if (config.getRuntimeParamList() == null) {
      return;
    }
    for (AutoexecParamVo parameter : config.getRuntimeParamList()) {
      ParamType type = ParamType.getParamType(parameter.getType());
      if (type == null) {
        throw invalid(parameter.getKey());
      }
      Object value = parameter.getDefaultValue();
      if (values != null && values.containsKey(parameter.getKey())) {
        value = values.get(parameter.getKey());
      }
      boolean empty = type.isValueEmpty(value, parameter.getKey(), parameter.getName());
      if (Objects.equals(parameter.getIsRequired(), 1) && empty) {
        throw invalid(parameter.getKey());
      }
      // JSON 类型的现有空值判断不校验语法，提交前使用统一 JSON 解析器拒绝无效正文。
      if (!empty && type == ParamType.JSON && value instanceof String) {
        try {
          JSON.parse((String) value);
        } catch (RuntimeException ex) {
          throw invalid(parameter.getKey());
        }
      }
      if (!autoexecService.validateTextTypeParamValue(parameter, value)) {
        throw invalid(parameter.getKey());
      }
    }
  }

  /** 行锁下重验权限，仅 READY 作业可首次启动，其余状态幂等返回。 */
  @Override
  @Transactional
  public JSONObject startJob(Long jobId) {
    checkBase();
    AutoexecJobVo job = jobMapper.getJobLockByJobId(jobId);
    if (job == null
        || !JobSource.COMBOP.getValue().equals(job.getSource())
        || !Objects.equals(job.getExecUser(), UserContext.get().getUserUuid())) {
      throw invalid("job executor");
    }
    AutoexecCombopVersionVo version = active(job.getOperationId());
    if (JobStatus.READY.getValue().equals(job.getStatus())) {
      AutoexecJobVo invoke = jobMapper.getJobInfoWithInvoke(jobId);
      if (invoke == null || !Objects.equals(invoke.getInvokeId(), version.getId())) {
        throw invalid("version changed");
      }
      AutoexecCombopConfigVo snapshot =
          JSON.parseObject(
              jobMapper.getJobContent(job.getConfigHash()).getContent(),
              AutoexecCombopConfigVo.class);
      AutoexecCombopExecuteNodeConfigVo nodes = snapshot.getExecuteConfig().getExecuteNodeConfig();
      if (nodes == null
          || nodes.getInputNodeList() == null
          || nodes.getInputNodeList().size() != 1
          || nodes.getSelectNodeList() != null && !nodes.getSelectNodeList().isEmpty()
          || nodes.getFilter() != null && !nodes.getFilter().isEmpty()
          || nodes.getOtherFilter() != null && !nodes.getOtherFilter().isEmpty()
          || nodes.getParamList() != null && !nodes.getParamList().isEmpty()
          || nodes.getPreOutputList() != null && !nodes.getPreOutputList().isEmpty()) {
        throw invalid("job target");
      }
      AutoexecCombopVersionConfigVo effective =
          JSON.parseObject(JSON.toJSONString(snapshot), AutoexecCombopVersionConfigVo.class);
      validateResolvedTarget(effective, nodes.getInputNodeList().get(0).getIp());
      job.setActionParam(new JSONObject());
      try {
        actionService.getJobDetailAndFireJob(job);
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IllegalStateException("启动快捷作业失败", ex);
      }
    }
    JSONObject result = new JSONObject();
    result.put("jobId", String.valueOf(jobId));
    result.put("status", job.getStatus());
    return result;
  }

  /** 当前宿主机目标参数由服务端锁定，不能接受来自执行窗口的映射覆盖。 */
  protected Set<String> targetParamKeys(AutoexecCombopVersionConfigVo config) {
    Set<String> keys = new LinkedHashSet<>();
    AutoexecCombopExecuteNodeConfigVo nodes = config.getExecuteConfig().getExecuteNodeConfig();
    if (nodes != null && nodes.getParamList() != null) {
      keys.addAll(nodes.getParamList());
    }
    return keys;
  }

  /** 执行字段和摘要不能引用密码参数，否则真实值会经用户名或 Runner 标签间接暴露。 */
  private void validateExecutionReferences(AutoexecCombopVersionConfigVo config) {
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    if (config.getRuntimeParamList() == null) {
      return;
    }
    for (ParamMappingVo mapping :
        Arrays.asList(
            execute.getExecuteUser(), execute.getRunnerGroup(), execute.getRunnerGroupTag())) {
      if (mapping == null
          || !ParamMappingMode.RUNTIME_PARAM.getValue().equals(mapping.getMappingMode())
          || mapping.getValue() == null) {
        continue;
      }
      for (AutoexecParamVo parameter : config.getRuntimeParamList()) {
        if (parameter.getKey().equals(String.valueOf(mapping.getValue()))
            && sensitive(JSON.parseObject(JSON.toJSONString(parameter)))) {
          throw invalid("sensitive execution reference");
        }
      }
    }
  }

  /** 固定执行字段显示只读值；运行参数引用保留来源信息供解析器使用。 */
  protected JSONArray executionDefinitions(AutoexecCombopVersionConfigVo config) {
    validateExecutionReferences(config);
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    JSONArray result = new JSONArray();
    result.add(
        executionDefinition(
            "executeNodeConfig",
            "node",
            "autoexec.quicktool.field.executeNodeConfig",
            null,
            true,
            true,
            true));
    result.add(
        executionDefinition(
            "protocolId",
            "select",
            "autoexec.quicktool.field.protocolId",
            execute.getProtocolId(),
            execute.getProtocolId() != null,
            true,
            false));
    result.add(
        executionMappingDefinition(
            "executeUser",
            "text",
            "autoexec.quicktool.field.executeUser",
            execute.getExecuteUser(),
            true));
    result.add(
        executionMappingDefinition(
            "runnerGroup",
            "runnergroup",
            "autoexec.quicktool.field.runnerGroup",
            execute.getRunnerGroup(),
            false));
    result.add(
        executionMappingDefinition(
            "runnerGroupTag",
            "runnergrouptag",
            "autoexec.quicktool.field.runnerGroupTag",
            execute.getRunnerGroupTag(),
            false));
    result.add(
        executionDefinition(
            "parallelPolicy",
            "select",
            "autoexec.quicktool.field.parallelPolicy",
            execute.getParallelPolicy(),
            StringUtils.isNotBlank(execute.getParallelPolicy()),
            false,
            false));
    result.add(
        executionDefinition(
            "roundCount",
            "text",
            "autoexec.quicktool.field.roundCount",
            execute.getRoundCount(),
            execute.getRoundCount() != null,
            false,
            false));
    result.add(
        executionDefinition(
            "parallelCount",
            "text",
            "autoexec.quicktool.field.parallelCount",
            execute.getParallelCount(),
            execute.getParallelCount() != null,
            false,
            false));
    return result;
  }

  /** 执行参数定义沿用既有类型组件，协议选项由服务端读取，避免额外 CMDB 接口权限。 */
  private JSONObject executionDefinition(
      String key,
      String type,
      String name,
      Object value,
      boolean fixed,
      boolean required,
      boolean locked) {
    JSONObject definition = new JSONObject();
    definition.put("key", key);
    definition.put("type", type);
    definition.put("name", $.t(name));
    definition.put("description", "");
    definition.put("isRequired", 0);
    if (required) {
      definition.put("isRequired", 1);
    }
    definition.put("isFixed", fixed);
    definition.put("locked", locked);
    JSONObject config = new JSONObject();
    if ("protocolId".equals(key)) {
      JSONArray protocols = new JSONArray();
      IResourceAccountCrossoverMapper accounts =
          CrossoverServiceFactory.tryToGetApi(IResourceAccountCrossoverMapper.class);
      if (accounts != null) {
        List<AccountProtocolVo> available = accounts.getAllAccountProtocolList();
        if (available != null) {
          for (AccountProtocolVo protocol : available) {
            String protocolName = protocol.getName();
            if ("tagent".equals(protocolName)
                || protocolName != null && protocolName.startsWith("tagent.")) {
              JSONObject option = new JSONObject();
              option.put("value", String.valueOf(protocol.getId()));
              option.put("text", protocolName);
              protocols.add(option);
            }
          }
        }
      }
      config.put("dataList", protocols);
    } else if ("parallelPolicy".equals(key)) {
      JSONArray options = new JSONArray();
      for (AutoexecParallelPolicy policy : AutoexecParallelPolicy.values()) {
        JSONObject option = new JSONObject();
        option.put("value", policy.getValue());
        option.put("text", policy.getName());
        options.add(option);
      }
      config.put("dataList", options);
    }
    definition.put("config", config);
    if (value instanceof Long) {
      value = String.valueOf(value);
    }
    if (value != null) {
      definition.put("value", value);
    }
    return definition;
  }

  /** 非常量执行字段从运行参数读取，字段未配置时由快捷工具映射补充。 */
  private JSONObject executionMappingDefinition(
      String key, String type, String name, ParamMappingVo mapping, boolean required) {
    Object value = null;
    boolean fixed = false;
    if (mapping != null
        && mapping.getValue() != null
        && StringUtils.isNotBlank(String.valueOf(mapping.getValue()))) {
      if (ParamMappingMode.CONSTANT.getValue().equals(mapping.getMappingMode())) {
        value = mapping.getValue();
        fixed = true;
      } else if (!ParamMappingMode.RUNTIME_PARAM.getValue().equals(mapping.getMappingMode())) {
        throw invalid(key);
      }
    }
    JSONObject definition = executionDefinition(key, type, name, value, fixed, required, false);
    if (!fixed && mapping != null && mapping.getValue() != null) {
      definition.put("runtimeParamKey", String.valueOf(mapping.getValue()));
    }
    return definition;
  }

  /** 只校验作业配置结构及已填写常量，不要求运行输入或连接凭据已就绪。 */
  @Override
  public JSONObject validateJobConfig(Long combopId, JSONObject inputConfig) {
    AutoexecCombopVersionVo version = active(combopId);
    JSONObject config = copyObject(inputConfig);
    if (config.getIntValue("schemaVersion") != 2
        || !"single".equals(config.getString("createJobPolicy"))) {
      throw invalid("jobConfig");
    }
    Map<String, JSONObject> runtime =
        definitionMap(safeRuntimeParams(version.getConfig().getRuntimeParamList()));
    for (String key : targetParamKeys(version.getConfig())) {
      runtime.get(key).put("locked", true);
    }
    validateMappings(config.getJSONArray("paramMappingList"), runtime);
    validateMappings(
        config.getJSONArray("executeParamMappingList"),
        definitionMap(executionDefinitions(version.getConfig())));
    JSONObject scenario = config.getJSONObject("scenarioMapping");
    if (scenario != null) {
      validateSource(scenario);
      if ("fixed".equals(scenario.getString("source"))) {
        resolveScenario(version.getConfig(), scenario, new JSONObject(), null);
      }
    }
    String prefix = config.getString("jobNamePrefix");
    if (StringUtils.isNotBlank(prefix)
        && !"executeNodeConfig".equals(prefix)
        && !runtime.containsKey(prefix)
        && !definitionMap(executionDefinitions(version.getConfig())).containsKey(prefix)) {
      throw invalid("jobNamePrefix");
    }
    if (runtime.containsKey(prefix) && sensitive(runtime.get(prefix))) {
      throw invalid("jobNamePrefix");
    }
    return config;
  }

  /** 映射只接受已注册字段，固定值按原参数类型转换并即时校验。 */
  private void validateMappings(JSONArray mappings, Map<String, JSONObject> definitions) {
    if (mappings == null) {
      return;
    }
    Set<String> keys = new HashSet<>();
    for (Object item : mappings) {
      if (!(item instanceof JSONObject)) {
        throw invalid("mapping");
      }
      JSONObject mapping = (JSONObject) item;
      String key = mapping.getString("key");
      JSONObject definition = definitions.get(key);
      if (definition == null || !keys.add(key)) {
        throw invalid("mapping key");
      }
      validateSource(mapping);
      if (definition.getString("runtimeParamKey") != null) {
        throw invalid("derived execution parameter");
      }
      if (definition.getBooleanValue("locked")) {
        if (!"context".equals(mapping.getString("source"))
            || !"currentNode".equals(mapping.getString("value"))) {
          throw invalid("locked target");
        }
      } else if (definition.getBooleanValue("isFixed")) {
        throw invalid("fixed execution parameter");
      }
      if ("node".equals(definition.getString("type"))
          && "context".equals(mapping.getString("source"))
          && !"currentNode".equals(mapping.getString("value"))) {
        throw invalid("node context");
      }
      if (sensitive(definition) && !"input".equals(mapping.getString("source"))) {
        throw invalid("sensitive parameter");
      }
      if ("fixed".equals(mapping.getString("source"))) {
        Object value = normalizeValue(definition, mapping.get("value"));
        validateDefinition(definition, value, true);
        if ("protocolId".equals(key)) {
          validateProtocol(longValue(value, key));
        }
      }
    }
  }

  /** 来源是有限契约；上下文只允许读取服务端已有字段，不执行表达式。 */
  private void validateSource(JSONObject mapping) {
    if (!Arrays.asList("fixed", "context", "input").contains(mapping.getString("source"))) {
      throw invalid("mapping source");
    }
  }

  /** 解析后的公开摘要去除敏感值，内部用于创建的真实值不经过回显。 */
  @Override
  public JSONObject resolveJob(AutoexecQuickJobRequestVo request) {
    JSONObject result = resolveInternal(request);
    result.remove("_param");
    result.remove("_config");
    return result;
  }

  /** 固定字段不可覆盖，目标锁定当前宿主机，未完成输入在草稿预览中只标记 incomplete。 */
  protected JSONObject resolveInternal(AutoexecQuickJobRequestVo request) {
    AutoexecCombopVersionVo version = active(request.getCombopId());
    if (!Objects.equals(version.getId(), request.getVersionId())) {
      throw invalid("version changed");
    }
    JSONObject jobConfig = validateJobConfig(request.getCombopId(), request.getJobConfig());
    AutoexecCombopVersionConfigVo config =
        JSON.parseObject(
            JSON.toJSONString(version.getConfig()), AutoexecCombopVersionConfigVo.class);
    JSONObject input = copyObject(request.getInput());
    JSONObject context = copyObject(request.getContext());
    JSONArray target = lockedTarget(request.getHost(), context.get("currentNode"));
    Set<String> targets = targetParamKeys(config);
    JSONObject params = new JSONObject();
    JSONArray paramDefinitions = new JSONArray();
    validateInputFields(input, jobConfig, config);
    Map<String, JSONObject> runtimeMappings =
        definitionMap(jobConfig.getJSONArray("paramMappingList"));
    boolean complete = true;
    if (config.getRuntimeParamList() != null) {
      for (AutoexecParamVo parameter : config.getRuntimeParamList()) {
        JSONObject definition = JSON.parseObject(JSON.toJSONString(parameter));
        String key = parameter.getKey();
        JSONObject mapping = runtimeMappings.get(key);
        Object raw = parameter.getDefaultValue();
        String source = "input";
        if (sensitive(definition)) {
          // 密码只能来自本次人工输入，不能隐式使用工具默认值或版本残留值。
          raw = null;
          if (input.getJSONObject("param") != null) {
            raw = input.getJSONObject("param").get(key);
          }
        } else if (mapping != null) {
          source = mapping.getString("source");
          // 人工输入未提交字段时保留普通默认值；显式提交 null 表示清空。
          JSONObject provided = input.getJSONObject("param");
          if (!"input".equals(source) || provided != null && provided.containsKey(key)) {
            raw = mappedValue(mapping, context, provided, key);
          }
        } else if (input.getJSONObject("param") != null
            && input.getJSONObject("param").containsKey(key)) {
          raw = input.getJSONObject("param").get(key);
        }
        boolean locked = targets.contains(key);
        if (locked) {
          raw = target;
          source = "context";
        }
        Object value = normalizeValue(definition, raw);
        boolean valid = validateDefinition(definition, value, request.isComplete());
        complete = complete && valid;
        parameter.setValue(value);
        params.put(key, value);
        definition.put("value", value);
        definition.put("source", source);
        definition.put("locked", locked);
        if (sensitive(definition)) {
          clearSensitiveValues(definition);
        }
        paramDefinitions.add(definition);
      }
    }
    JSONArray execution = executionDefinitions(config);
    Map<String, JSONObject> executionMappings =
        definitionMap(jobConfig.getJSONArray("executeParamMappingList"));
    JSONObject effective = new JSONObject();
    for (Object item : execution) {
      JSONObject definition = (JSONObject) item;
      String key = definition.getString("key");
      Object raw = definition.get("value");
      String source = "fixed";
      if (definition.getBooleanValue("locked")) {
        raw = target;
        source = "context";
      } else if (!definition.getBooleanValue("isFixed")) {
        JSONObject mapping = executionMappings.get(key);
        source = "input";
        if (mapping != null) {
          raw = mappedValue(mapping, context, input.getJSONObject("executeParam"), key);
          source = mapping.getString("source");
        } else if (definition.getString("runtimeParamKey") != null) {
          raw = params.get(definition.getString("runtimeParamKey"));
          source = "runtimeparam";
        } else if (input.getJSONObject("executeParam") != null) {
          raw = input.getJSONObject("executeParam").get(key);
        }
      }
      Object value = normalizeValue(definition, raw);
      boolean valid = validateDefinition(definition, value, request.isComplete());
      complete = complete && valid;
      effective.put(key, value);
      definition.put("source", source);
      definition.put("value", value);
    }
    Long scenario =
        resolveScenario(
            config, jobConfig.getJSONObject("scenarioMapping"), context, input.get("scenario"));
    AutoexecCombopExecuteConfigVo execute = config.getExecuteConfig();
    execute.setProtocolId(longValue(effective.get("protocolId"), "protocolId"));
    execute.setExecuteUser(constant(effective.get("executeUser")));
    execute.setRunnerGroup(constant(effective.get("runnerGroup")));
    execute.setRunnerGroupTag(constant(effective.get("runnerGroupTag")));
    String parallel = effective.getString("parallelPolicy");
    if (StringUtils.isBlank(parallel)) {
      parallel = AutoexecParallelPolicy.PARALLEL.getValue();
    }
    if (!AutoexecParallelPolicy.PARALLEL.getValue().equals(parallel)
        && !AutoexecParallelPolicy.ROUND_COUNT.getValue().equals(parallel)) {
      throw invalid("parallelPolicy");
    }
    execute.setParallelPolicy(parallel);
    Integer count = positiveCount(effective.get("parallelCount"), "parallelCount");
    Integer round = positiveCount(effective.get("roundCount"), "roundCount");
    if (AutoexecParallelPolicy.PARALLEL.getValue().equals(parallel)) {
      if (count == null) {
        count = 32;
      }
      round = null;
    } else {
      if (round == null) {
        round = 64;
      }
      count = null;
    }
    execute.setParallelCount(count);
    execute.setRoundCount(round);
    // 表单显示与已生效策略、数量一致；非当前策略的数量保留人工输入，切换策略时不丢失。
    for (Object item : execution) {
      JSONObject definition = (JSONObject) item;
      String key = definition.getString("key");
      if ("parallelPolicy".equals(key)) {
        definition.put("value", parallel);
      } else if ("parallelCount".equals(key) && count != null) {
        definition.put("value", count);
      } else if ("roundCount".equals(key) && round != null) {
        definition.put("value", round);
      }
    }
    execute.setWhenToSpecify(CombopNodeSpecify.RUNTIME.getValue());
    AutoexecCombopExecuteNodeConfigVo nodes = new AutoexecCombopExecuteNodeConfigVo();
    nodes.setInputNodeList(target.toJavaList(AutoexecNodeVo.class));
    execute.setExecuteNodeConfig(nodes);
    if (request.isComplete()) {
      validateResolvedTarget(config, request.getHost());
      validateParams(config, params);
    }
    String name = jobConfig.getString("jobName");
    if (StringUtils.isBlank(name)) {
      name = request.getEntryName();
    }
    String prefixKey = jobConfig.getString("jobNamePrefix");
    Object prefix = params.get(prefixKey);
    if ("executeNodeConfig".equals(prefixKey)) {
      prefix = request.getHost();
    } else if (effective.containsKey(prefixKey)) {
      prefix = effective.get(prefixKey);
    }
    if (prefix != null && StringUtils.isNotBlank(String.valueOf(prefix))) {
      String text = String.valueOf(prefix);
      if (text.length() > 32) {
        text = text.substring(0, 32);
      }
      name = text + "_" + name;
    }
    JSONObject result = new JSONObject();
    result.put("jobName", name);
    result.put("versionId", String.valueOf(version.getId()));
    if (scenario != null) {
      result.put("scenarioId", String.valueOf(scenario));
    }
    for (String key : Arrays.asList("executeUser", "runnerGroup", "runnerGroupTag")) {
      result.put(key, effective.get(key));
    }
    if (execute.getProtocolId() != null) {
      result.put("protocolId", String.valueOf(execute.getProtocolId()));
    }
    effective.put("parallelPolicy", parallel);
    putExecutionDisplay(result, version, effective);
    result.put("parallelPolicy", parallel);
    result.put("roundCount", round);
    result.put("parallelCount", count);
    putScenarioMetadata(result, config);
    JSONObject scenarioMapping = jobConfig.getJSONObject("scenarioMapping");
    if (scenarioMapping == null) {
      scenarioMapping = new JSONObject();
      scenarioMapping.put("source", "input");
      if (config.getDefaultScenarioId() != null) {
        scenarioMapping.put("source", "fixed");
        scenarioMapping.put("value", String.valueOf(config.getDefaultScenarioId()));
      }
    }
    result.put("scenarioMapping", scenarioMapping);
    if (scenario != null && config.getScenarioList() != null) {
      for (AutoexecCombopScenarioVo item : config.getScenarioList()) {
        if (scenario.equals(item.getScenarioId())) {
          result.put("scenarioName", item.getScenarioName());
        }
      }
    }
    result.put("paramList", paramDefinitions);
    result.put("executeParamList", execution);
    result.put("complete", complete);
    result.put("targetParamKeys", targets);
    result.put("_param", params);
    result.put("_config", config);
    return result;
  }

  /** 执行输入只接受允许人工填写的字段，不能覆盖固定值、上下文或派生执行字段。 */
  private void validateInputFields(
      JSONObject input, JSONObject jobConfig, AutoexecCombopVersionConfigVo config) {
    for (String key : input.keySet()) {
      if (!Arrays.asList("param", "executeParam", "scenario").contains(key)) {
        throw invalid("input field");
      }
    }
    Map<String, JSONObject> parameters =
        definitionMap(safeRuntimeParams(config.getRuntimeParamList()));
    for (String key : targetParamKeys(config)) {
      parameters.get(key).put("locked", true);
    }
    validateInputGroup(
        input.get("param"), parameters, definitionMap(jobConfig.getJSONArray("paramMappingList")));
    validateInputGroup(
        input.get("executeParam"),
        definitionMap(executionDefinitions(config)),
        definitionMap(jobConfig.getJSONArray("executeParamMappingList")));
    JSONObject scenario = jobConfig.getJSONObject("scenarioMapping");
    if (input.containsKey("scenario")
        && scenario != null
        && !"input".equals(scenario.getString("source"))) {
      throw invalid("scenario input");
    }
  }

  /** 框架不验证嵌套字段白名单，所有未知或不可写字段都在服务端拒绝。 */
  private void validateInputGroup(
      Object input, Map<String, JSONObject> definitions, Map<String, JSONObject> mappings) {
    if (input == null) {
      return;
    }
    if (!(input instanceof JSONObject)) {
      throw invalid("input group");
    }
    for (String key : ((JSONObject) input).keySet()) {
      JSONObject definition = definitions.get(key);
      JSONObject mapping = mappings.get(key);
      if (definition == null
          || definition.getBooleanValue("locked")
          || definition.getBooleanValue("isFixed")
          || definition.getString("runtimeParamKey") != null
          || mapping != null && !"input".equals(mapping.getString("source"))) {
        throw invalid("input " + key);
      }
    }
  }

  /** 原生编辑器值和流程字段均经过类型注册表转换，避免提前执行加密或文件 ID 出参转换。 */
  protected Object normalizeValue(JSONObject definition, Object raw) {
    if (raw == null) {
      return null;
    }
    String type = definition.getString("type");
    if (raw instanceof Collection || raw instanceof Map) {
      raw = JSON.parse(JSON.toJSONString(raw));
    }
    IScriptParamType handler = ScriptParamTypeFactory.getHandler(type);
    if (handler == null) {
      throw invalid("parameter type " + type);
    }
    // 数组/对象字符串先还原原生形态，避免多选和 JSON 被二次编码。
    if (raw instanceof String
        && (StringUtils.startsWith((String) raw, "[")
            || StringUtils.startsWith((String) raw, "{"))) {
      try {
        raw = JSON.parse((String) raw);
      } catch (RuntimeException ex) {
        if ("json".equals(type)) {
          throw invalid(definition.getString("key"));
        }
      }
    }
    if ("file".equals(type) && raw instanceof JSONObject) {
      JSONObject file = (JSONObject) raw;
      if (!file.containsKey("id") && !(file.get("fileIdList") instanceof JSONArray)) {
        throw invalid(definition.getString("key"));
      }
    }
    if ("file".equals(type)
        && raw instanceof JSONObject
        && ((JSONObject) raw).get("fileIdList") instanceof JSONArray
        && ((JSONObject) raw).get("fileList") instanceof JSONArray) {
      return JSON.parseObject(JSON.toJSONString(raw));
    }
    if ("node".equals(type) && raw instanceof JSONObject) {
      JSONObject node = (JSONObject) raw;
      if (!node.containsKey("ip")
          && !node.containsKey("inputNodeList")
          && !node.containsKey("selectNodeList")) {
        throw invalid(definition.getString("key"));
      }
    }
    JSONArray values = new JSONArray();
    if (raw instanceof JSONArray && !"json".equals(type)) {
      values.addAll((JSONArray) raw);
    } else {
      values.add(raw);
    }
    Object normalized = handler.convertDataForProcessComponent(values);
    if (Arrays.asList("node", "userselect", "file").contains(type) && !values.isEmpty()) {
      ParamType parameterType = ParamType.getParamType(type);
      if (parameterType.isValueEmpty(
              normalized, definition.getString("key"), definition.getString("name"))
          && !parameterType.isValueEmpty(
              raw, definition.getString("key"), definition.getString("name"))) {
        throw invalid(definition.getString("key"));
      }
    }
    return normalized;
  }

  /** 仅完整解析要求必填，已填写值始终走现有类型空值语义和文本规则。 */
  protected boolean validateDefinition(JSONObject definition, Object value, boolean required) {
    ParamType type = ParamType.getParamType(definition.getString("type"));
    if (type == null) {
      throw invalid(definition.getString("key"));
    }
    boolean empty =
        type.isValueEmpty(value, definition.getString("key"), definition.getString("name"));
    if (definition.getIntValue("isRequired") == 1 && empty) {
      if (required) {
        throw invalid(definition.getString("key"));
      }
      return false;
    }
    AutoexecParamVo parameter = JSON.toJavaObject(definition, AutoexecParamVo.class);
    if (!autoexecService.validateTextTypeParamValue(parameter, value)) {
      throw invalid(definition.getString("key"));
    }
    return true;
  }

  /** 目标只从受信宿主机构造；上下文节点即使附带额外目标也必须拒绝。 */
  private JSONArray lockedTarget(String host, Object contextNode) {
    JSONArray result = new JSONArray();
    if (StringUtils.isBlank(host)) {
      throw invalid("host");
    }
    if (contextNode != null) {
      JSONObject definition = new JSONObject();
      definition.put("key", "currentNode");
      definition.put("type", "node");
      Object converted = normalizeValue(definition, contextNode);
      JSONArray nodes = JSON.parseArray(JSON.toJSONString(converted));
      if (nodes == null
          || nodes.size() != 1
          || !host.equals(nodes.getJSONObject(0).getString("ip"))) {
        throw invalid("locked target");
      }
    }
    JSONObject node = new JSONObject();
    node.put("ip", host);
    result.add(node);
    return result;
  }

  /** 来源字段只读取上下文已有值或本字段运行输入。 */
  private Object mappedValue(JSONObject mapping, JSONObject context, JSONObject input, String key) {
    if ("input".equals(mapping.getString("source"))) {
      if (input != null) {
        return input.get(key);
      }
      return null;
    }
    if ("fixed".equals(mapping.getString("source"))) {
      return mapping.get("value");
    }
    String field = mapping.getString("value");
    Object current = context;
    if (field == null) {
      return null;
    }
    for (String part : field.split("\\.")) {
      if (!(current instanceof JSONObject)) {
        return null;
      }
      current = ((JSONObject) current).get(part);
    }
    return current;
  }

  /** 场景仅接受激活版本已有编号或名称，省略映射时采用版本默认值。 */
  private Long resolveScenario(
      AutoexecCombopVersionConfigVo config, JSONObject mapping, JSONObject context, Object input) {
    Object raw = config.getDefaultScenarioId();
    if (mapping != null) {
      JSONObject values = new JSONObject();
      values.put("scenario", input);
      raw = mappedValue(mapping, context, values, "scenario");
    } else if (input != null) {
      raw = input;
    }
    if (raw instanceof JSONArray) {
      if (((JSONArray) raw).isEmpty()) {
        return null;
      }
      raw = ((JSONArray) raw).get(0);
    }
    if (raw == null || StringUtils.isBlank(String.valueOf(raw))) {
      return null;
    }
    if (config.getScenarioList() != null) {
      for (AutoexecCombopScenarioVo scenario : config.getScenarioList()) {
        if (String.valueOf(scenario.getScenarioId()).equals(String.valueOf(raw))
            || Objects.equals(scenario.getScenarioName(), String.valueOf(raw))) {
          return scenario.getScenarioId();
        }
      }
    }
    throw invalid("scenario");
  }

  /** 固定执行配置保留类型原值，Runner 标签列表仍按既有参数映射结构传递。 */
  private ParamMappingVo constant(Object value) {
    if (value == null) {
      return null;
    }
    ParamMappingVo mapping = new ParamMappingVo();
    mapping.setMappingMode(ParamMappingMode.CONSTANT.getValue());
    if (value instanceof Collection || value instanceof Map) {
      mapping.setValue(JSON.toJSONString(value));
    } else {
      mapping.setValue(value);
    }
    return mapping;
  }

  /** 计数为正整数，类型或范围错误只回传字段名。 */
  private Integer positiveCount(Object value, String key) {
    if (value == null || StringUtils.isBlank(String.valueOf(value))) {
      return null;
    }
    try {
      int number = Integer.parseInt(String.valueOf(value));
      if (number > 0) {
        return number;
      }
    } catch (NumberFormatException ex) {
      throw invalid(key);
    }
    throw invalid(key);
  }

  /** 标识按长整数读取，不将 Snowflake 编号转换成浮点。 */
  private Long longValue(Object value, String key) {
    if (value == null || StringUtils.isBlank(String.valueOf(value))) {
      return null;
    }
    try {
      return Long.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      throw invalid(key);
    }
  }

  /** 只读摘要使用业务版本与名称，不将数据库编号或执行枚举原值作为展示文本。 */
  private void putExecutionDisplay(
      JSONObject result, AutoexecCombopVersionVo version, JSONObject execution) {
    String versionText = null;
    if (version.getVersion() != null) {
      versionText = "v" + version.getVersion();
    }
    result.put("versionText", versionText);
    result.put("runnerGroupName", runnerGroupName(execution.get("runnerGroup")));
    result.put("runnerGroupTagNameList", runnerTagNames(execution.get("runnerGroupTag")));
    String policy = execution.getString("parallelPolicy");
    String policyText = null;
    if (StringUtils.isNotBlank(policy)) {
      policyText = AutoexecParallelPolicy.getName(policy);
      if (StringUtils.isBlank(policyText)) {
        policyText = null;
      }
    }
    result.put("parallelPolicyText", policyText);
    if (execution.get("protocolId") != null) {
      result.put(
          "protocolName", protocolName(longValue(execution.get("protocolId"), "protocolId")));
    }
  }

  /** 随机分配使用模块文案，具体执行器组只展示查询到的名称，未知编号不回退展示。 */
  private String runnerGroupName(Object value) {
    if (value == null || StringUtils.isBlank(String.valueOf(value))) {
      return null;
    }
    String identifier = String.valueOf(value);
    if ("-1".equals(identifier)) {
      return $.t("autoexec.quicktool.randomRunnerGroup");
    }
    RunnerGroupVo group;
    try {
      group = runnerMapper.getRunnerGroupById(Long.valueOf(identifier));
    } catch (NumberFormatException ex) {
      group = runnerMapper.getRunnerGroupByName(identifier);
    }
    if (group == null || StringUtils.isBlank(group.getName())) {
      return null;
    }
    return group.getName();
  }

  /** 标签编号或名称统一批量解析一次，并按配置顺序返回真实标签名称。 */
  private JSONArray runnerTagNames(Object value) {
    JSONArray result = new JSONArray();
    if (value == null || StringUtils.isBlank(String.valueOf(value))) {
      return result;
    }
    Object nativeValue = value;
    if (value instanceof String && ((String) value).trim().startsWith("[")) {
      try {
        nativeValue = JSON.parseArray((String) value);
      } catch (RuntimeException ex) {
        return result;
      }
    }
    Set<String> requested = new LinkedHashSet<>();
    if (nativeValue instanceof Collection) {
      for (Object item : (Collection<?>) nativeValue) {
        if (item != null) {
          requested.add(String.valueOf(item));
        }
      }
    } else {
      requested.add(String.valueOf(nativeValue));
    }
    if (requested.isEmpty()) {
      return result;
    }
    List<TagVo> tags = tagMapper.getTagListByIdOrNameList(new ArrayList<>(requested));
    Map<String, String> names = new HashMap<>();
    if (tags != null) {
      for (TagVo tag : tags) {
        if (StringUtils.isNotBlank(tag.getName())) {
          names.put(String.valueOf(tag.getId()), tag.getName());
          names.put(tag.getName(), tag.getName());
        }
      }
    }
    Set<String> display = new LinkedHashSet<>();
    for (String identifier : requested) {
      if (names.containsKey(identifier)) {
        display.add(names.get(identifier));
      }
    }
    result.addAll(display);
    return result;
  }

  /** 协议名称取注册信息，缺少输入时草稿不要求协议已配置。 */
  protected String protocolName(Long id) {
    if (id == null) {
      return null;
    }
    IResourceAccountCrossoverMapper accounts =
        CrossoverServiceFactory.tryToGetApi(IResourceAccountCrossoverMapper.class);
    if (accounts == null) {
      return null;
    }
    AccountProtocolVo protocol = accounts.getAccountProtocolVoByProtocolId(id);
    if (protocol == null || !Objects.equals(id, protocol.getId())) {
      return null;
    }
    return protocol.getName();
  }

  /** 以字段 key 构建映射，空列表统一为空映射。 */
  private Map<String, JSONObject> definitionMap(JSONArray list) {
    Map<String, JSONObject> result = new LinkedHashMap<>();
    if (list != null) {
      for (Object item : list) {
        JSONObject row = (JSONObject) item;
        result.put(row.getString("key"), row);
      }
    }
    return result;
  }

  /** 副本用于解析，避免填充值修改存储配置或激活版本缓存。 */
  private JSONObject copyObject(JSONObject source) {
    if (source == null) {
      return new JSONObject();
    }
    return JSON.parseObject(
        JSON.toJSONString(
            source, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
  }

  /** 密码定义只能在内部执行时持有真实值。 */
  private boolean sensitive(JSONObject definition) {
    return "password".equalsIgnoreCase(definition.getString("type"))
        || definition.getBooleanValue("isSecret")
        || definition.getBooleanValue("isSensitive");
  }

  /** 新版完整解析后只创建 READY 作业，调用方负责同事务写入对象关联。 */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Long createReadyJobFromConfig(AutoexecQuickJobRequestVo request) {
    AutoexecQuickJobRequestVo full =
        JSON.parseObject(
            JSON.toJSONString(
                request, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue),
            AutoexecQuickJobRequestVo.class);
    full.setComplete(true);
    JSONObject resolved = resolveInternal(full);
    AutoexecCombopVersionConfigVo config = (AutoexecCombopVersionConfigVo) resolved.get("_config");
    AutoexecJobVo job = new AutoexecJobVo();
    job.setOperationId(request.getCombopId());
    job.setOperationType(CombopOperationType.COMBOP.getValue());
    job.setSource(JobSource.COMBOP.getValue());
    job.setCombopVersionId(request.getVersionId());
    job.setInvokeId(request.getVersionId());
    job.setRouteId(String.valueOf(request.getVersionId()));
    job.setName(resolved.getString("jobName"));
    job.setScenarioId(resolved.getLong("scenarioId"));
    job.setPlanStartTime(new Date());
    job.setTriggerType(JobTriggerType.MANUAL.getValue());
    job.setParam(resolved.getJSONObject("_param"));
    job.setExecuteConfig(config.getExecuteConfig());
    job.setWhenToSpecify(CombopNodeSpecify.RUNTIME.getValue());
    job.setRunnerGroup(config.getExecuteConfig().getRunnerGroup());
    job.setRunnerGroupTag(config.getExecuteConfig().getRunnerGroupTag());
    job.setParallelPolicy(config.getExecuteConfig().getParallelPolicy());
    job.setRoundCount(config.getExecuteConfig().getRoundCount());
    job.setParallelCount(config.getExecuteConfig().getParallelCount());
    job.setActionParam(new JSONObject());
    try {
      actionService.validateAndCreateJobFromCombop(job);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("创建快捷作业失败", ex);
    }
    return job.getId();
  }

  /** 现有作业详情按 AUTOEXEC_BASE 查看，批量查询同样使用该边界。 */
  @Override
  public List<JSONObject> getVisibleJobs(List<Long> jobIds) {
    checkBase();
    List<JSONObject> result = new ArrayList<>();
    if (jobIds == null || jobIds.isEmpty()) {
      return result;
    }
    Map<String, String> names = new HashMap<>();
    for (int offset = 0; offset < jobIds.size(); offset += 200) {
      for (AutoexecJobVo job :
          jobMapper.getJobListByIdList(
              jobIds.subList(offset, Math.min(jobIds.size(), offset + 200)))) {
        JSONObject row = new JSONObject();
        row.put("id", String.valueOf(job.getId()));
        row.put("jobId", String.valueOf(job.getId()));
        row.put("name", job.getName());
        row.put("status", job.getStatus());
        row.put("statusText", JobStatus.getText(job.getStatus()));
        row.put("execUser", job.getExecUser());
        row.put("startTime", job.getStartTime());
        row.put("endTime", job.getEndTime());
        if (!names.containsKey(job.getExecUser())) {
          UserVo user = userMapper.getUserByUuid(job.getExecUser());
          String display = job.getExecUser();
          if (user != null) {
            display = user.getUserName();
          }
          names.put(job.getExecUser(), display);
        }
        row.put("execUserName", names.get(job.getExecUser()));
        result.add(row);
      }
    }
    return result;
  }

  /** 使用框架参数异常提供可国际化的错误外壳，不包含请求正文或秘密。 */
  protected ParamIrregularException invalid(String field) {
    return new ParamIrregularException(field);
  }
}
