/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.stephandler.component;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobProcessTaskStepVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobThreadCountException;
import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.constvalue.ProcessUserType;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.module.autoexec.constvalue.AutoexecProcessStepHandlerType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.schedule.plugin.AutoexecJobStatusMonitorJob;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author linbq
 * @since 2021/9/2 14:22
 **/
@Service
public class AutoexecProcessComponent extends ProcessStepHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(AutoexecProcessComponent.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private AutoexecJobService autoexecJobService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getHandler() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getHandler();
    }

    @Override
    public JSONObject getChartConfig() {
        return new JSONObject() {
            {
                this.put("icon", "tsfont-zidonghua");
                this.put("shape", "L-rectangle:R-rectangle");
                this.put("width", 68);
                this.put("height", 40);
            }
        };
    }

    @Override
    public String getType() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        return ProcessStepMode.MT;
    }

    @Override
    public String getName() {
        return AutoexecProcessStepHandlerType.AUTOEXEC.getName();
    }

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Boolean isAllowStart() {
        return true;
    }

    @Override
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        String configHash = currentProcessTaskStepVo.getConfigHash();
        if (StringUtils.isBlank(configHash)) {
            currentProcessTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
            configHash = currentProcessTaskStepVo.getConfigHash();
        }
        String config = selectContentByHashMapper.getProcessTaskStepConfigByHash(configHash);
        if (StringUtils.isNotBlank(config)) {
            JSONObject autoexecConfig = (JSONObject)JSONPath.read(config, "autoexecConfig");
            if (MapUtils.isNotEmpty(autoexecConfig)) {
                JSONObject paramObj = new JSONObject();
                Long combopId = autoexecConfig.getLong("autoexecCombopId");
                paramObj.put("combopId", combopId);
                JSONArray runtimeParamList = autoexecConfig.getJSONArray("runtimeParamList");
                if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                    JSONObject param = new JSONObject();
                    for (int i =0; i < runtimeParamList.size(); i++) {
                        JSONObject runtimeParamObj = runtimeParamList.getJSONObject(i);
                        if (MapUtils.isNotEmpty(runtimeParamObj)) {
                            String key = runtimeParamObj.getString("key");
                            if (StringUtils.isNotBlank(key)) {
                                String value = runtimeParamObj.getString("value");
                                if (StringUtils.isNotBlank(value)) {
                                    String mappingMode = runtimeParamObj.getString("mappingMode");
                                    param.put(key, parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                                } else {
                                    param.put(key, value);
                                }
                            }
                        }
                    }
                    paramObj.put("param", param);
                }
                JSONArray executeParamList = autoexecConfig.getJSONArray("executeParamList");
                if (CollectionUtils.isNotEmpty(executeParamList)) {
                    JSONObject executeConfig = new JSONObject();
                    for (int i = 0; i < executeParamList.size(); i++) {
                        JSONObject executeParamObj = executeParamList.getJSONObject(i);
                        if (MapUtils.isNotEmpty(executeParamObj)) {
                            String key = executeParamObj.getString("key");
                            String value = executeParamObj.getString("value");
                            String mappingMode = executeParamObj.getString("mappingMode");
                            if ("protocol".equals(key)) {
                                if (StringUtils.isNotBlank(value)) {
                                    executeConfig.put("protocol", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                                } else {
                                    executeConfig.put("protocol", value);
                                }
                            } else if ("executeUser".equals(key)) {
                                if (StringUtils.isNotBlank(value)) {
                                    executeConfig.put("executeUser", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                                } else {
                                    executeConfig.put("executeUser", value);
                                }
                            } else if ("executeNodeConfig".equals(key)) {
                                if (StringUtils.isNotBlank(value)) {
                                    executeConfig.put("executeNodeConfig", parseMappingValue(currentProcessTaskStepVo, mappingMode, value));
                                } else {
                                    executeConfig.put("executeNodeConfig", value);
                                }
                            }
                        }
                    }
                    paramObj.put("executeConfig", executeConfig);
                }
                paramObj.put("source", "itsm");
                paramObj.put("threadCount", 1);

                try {
                    Long autoexecJobId = createJob(paramObj);
                    if (autoexecJobId != null) {
                        IJob jobHandler = SchedulerManager.getHandler(AutoexecJobStatusMonitorJob.class.getName());
                        if(jobHandler == null) {
                            throw new ScheduleHandlerNotFoundException(AutoexecJobStatusMonitorJob.class.getName());
                        }
                        AutoexecJobProcessTaskStepVo autoexecJobProcessTaskStepVo = new AutoexecJobProcessTaskStepVo();
                        autoexecJobProcessTaskStepVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                        autoexecJobProcessTaskStepVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                        autoexecJobProcessTaskStepVo.setAutoexecJobId(autoexecJobId);
                        autoexecJobProcessTaskStepVo.setNeedMonitorStatus(1);
                        autoexecJobMapper.insertAutoexecJobProcessTaskStep(autoexecJobProcessTaskStepVo);
                        JobObject.Builder jobObjectBuilder = new JobObject
                                .Builder(autoexecJobId.toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid())
                                .addData("autoexecJobId", autoexecJobId);
                        JobObject jobObject = jobObjectBuilder.build();
                        jobHandler.reloadJob(jobObject);
                    }
                } catch (Exception e) {
                    //TODO 如果创建作业时抛异常
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return 0;
    }

    private Object parseMappingValue(ProcessTaskStepVo currentProcessTaskStepVo, String mappingMode, String value) {
        if ("form".equals(mappingMode)) {
            List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataVoList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataVoList) {
                if(Objects.equals(value, attributeDataVo.getAttributeUuid())) {
                    return attributeDataVo.getDataObj();
                }
            }
            return null;
        } else if ("prestepexportparam".equals(mappingMode)) {
            //TODO linbq 上游出参，暂不支持
        }
        return value;
    }

    private Long createJob(JSONObject jsonObj) {
        Long combopId = jsonObj.getLong("combopId");
        Integer threadCount = jsonObj.getInteger("threadCount");
        JSONObject paramJson = jsonObj.getJSONObject("param");
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        //作业执行权限校验
//        autoexecCombopService.setOperableButtonList(combopVo);
//        if(combopVo.getExecutable() != 1){
//            throw new AutoexecCombopCannotExecuteException(combopVo.getName());
//        }
        //设置作业执行节点
        if(combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")){
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        autoexecCombopService.verifyAutoexecCombopConfig(combopVo);
        //TODO 校验执行参数

        //并发数必须是2的n次方
        if ((threadCount & (threadCount - 1)) != 0) {
            throw new AutoexecJobThreadCountException();
        }
        AutoexecJobVo jobVo = autoexecJobService.saveAutoexecCombopJob(combopVo, jsonObj.getString("source"), threadCount, paramJson);
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setCurrentPhaseSort(0);
        autoexecJobActionService.fire(jobVo);
        return jobVo.getId();
    }
    @Override
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, Set<ProcessTaskStepWorkerVo> workerSet) throws ProcessTaskException {
        String configHash = currentProcessTaskStepVo.getConfigHash();
        if (StringUtils.isBlank(configHash)) {
            currentProcessTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
            configHash = currentProcessTaskStepVo.getConfigHash();
        }
        String stepConfig = selectContentByHashMapper.getProcessTaskStepConfigByHash(configHash);
        String defaultWorker = (String) JSONPath.read(stepConfig, "workerPolicyConfig.defaultWorker");
        String[] split = defaultWorker.split("#");
        workerSet.add(new ProcessTaskStepWorkerVo(currentProcessTaskStepVo.getProcessTaskId(),
                currentProcessTaskStepVo.getId(), split[0], split[1], ProcessUserType.MAJOR.getValue()));
        return 1;
    }

    @Override
    protected int myHang(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myStart(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myComplete(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRecover(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int mySaveDraft(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myStartProcess(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected Set<ProcessTaskStepVo> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepVo> nextStepList, Long nextStepId) throws ProcessTaskException {
        return null;
    }

    @Override
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }
}
