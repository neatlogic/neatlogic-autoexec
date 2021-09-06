/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.stephandler.component;

import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.constvalue.ProcessUserType;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.framework.restful.core.MyApiComponent;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.module.autoexec.api.job.action.AutoexecJobFromCombopCreateApi;
import codedriver.module.autoexec.constvalue.AutoexecProcessStepHandlerType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author linbq
 * @since 2021/9/2 14:22
 **/
@Service
public class AutoexecProcessComponent extends ProcessStepHandlerBase {
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

                MyApiComponent restComponent = (MyApiComponent)PrivateApiComponentFactory.getInstance(AutoexecJobFromCombopCreateApi.class.getName());
                if (restComponent != null) {
                    try {
                        JSONObject result = (JSONObject) restComponent.myDoService(paramObj);
                        Long jobId = result.getLong("jobId");
                    } catch (Exception e) {

                    }
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
