/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.exception.AutoexecJobProxyConnectAuthException;
import codedriver.framework.autoexec.exception.AutoexecJobProxyConnectRefusedException;
import codedriver.framework.dto.RestVo;
import codedriver.framework.integration.authentication.costvalue.AuthenticateType;
import codedriver.framework.util.RestUtil;
import codedriver.module.autoexec.config.AutoexecConfig;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/27 11:30
 **/
@Service
public class AutoexecJobActionServiceImpl implements AutoexecJobActionService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecJobActionServiceImpl.class);

    @Resource
    AutoexecJobAuthActionManager autoexecJobAuthActionManager;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    /**
     * 第一次执行/重跑/继续作业
     *
     * @param jobVo 作业
     * @param type   重跑redo，第一次跑 first, 继续跑 goon
     */
    @Override
    public void fire(AutoexecJobVo jobVo, String type) {
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        autoexecJobAuthActionManager.setAutoexecJobAction(jobVo);
        if (jobVo.getIsCanJobExec() == 1) {
            jobVo.setStatus(JobStatus.RUNNING.getValue());
            autoexecJobMapper.updateJobStatus(jobVo);
            for(AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
                jobPhase.setStatus(JobPhaseStatus.WAITING.getValue());
                autoexecJobMapper.updateJobPhaseStatus(jobPhase);
            }
            JSONObject paramJson = new JSONObject();
            getFireParamJson(paramJson,jobVo);
            String url = AutoexecConfig.PROXY_URL() + "/job/exec";
            RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), AutoexecConfig.PROXY_BASIC_USER_NAME(), AutoexecConfig.PROXY_BASIC_PASSWORD(), paramJson);
            String result = RestUtil.sendRequest(restVo);
            JSONObject resultJson = null;
            try {
                resultJson = JSONObject.parseObject(result);
            }catch (Exception ex){
                logger.error(ex.getMessage(),ex);
                throw new AutoexecJobProxyConnectRefusedException(restVo.getUrl() + " " + result);
            }
            if(!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))){
                throw new AutoexecJobProxyConnectAuthException(resultJson.getString("Message"));
            }
        }
    }

    /**
     * 拼装给proxy的param
     * @param paramJson 返回param值
     * @param jobVo 作业
     */
    private void getFireParamJson(JSONObject paramJson,AutoexecJobVo jobVo){
        paramJson.put("jobId", jobVo.getId());
        paramJson.put("preJobId", null); //给后续ITSM对接使用
        paramJson.put("parallel",jobVo.getThreadCount());
        paramJson.put("passThroughEnv", null); //回调需要的返回的参数
        JSONArray paramArray = jobVo.getParam();
        JSONObject argJson = new JSONObject(){{
            for(Object paramObj : paramArray ){
                JSONObject paramTmp = JSONObject.parseObject(paramObj.toString());
                put(paramTmp.getString("key"),paramTmp.getString("value"));
            }
        }};
        paramJson.put("arg",argJson);
        JSONArray runFlowArray = new JSONArray();
        paramJson.put("runFlow",new JSONArray(){{
            for(AutoexecJobPhaseVo jobPhase : jobVo.getPhaseList()) {
                add(new JSONObject(){{
                    put(jobPhase.getName(),new JSONArray(){{
                        for(AutoexecJobPhaseOperationVo operationVo : jobPhase.getOperationList()){
                            add(new JSONObject(){{
                                put("opId",operationVo.getName()+"_"+operationVo.getId());
                                put("opName",operationVo.getName());
                                put("opType",operationVo.getExecMode());
                                put("failIgnore",operationVo.getFailIgnore());
                                JSONObject param = operationVo.getParam();
                                put("arg",new JSONObject(){{
                                    for(Object arg : param.getJSONArray("inputParamList")){
                                        JSONObject argJson  = JSONObject.parseObject(arg.toString());
                                        put(argJson.getString("key"),argJson.getString("value"));
                                    }
                                }});
                                put("desc",new JSONObject(){{
                                    for(Object arg : param.getJSONArray("inputParamList")) {
                                        JSONObject argJson  = JSONObject.parseObject(arg.toString());
                                        put(argJson.getString("key"),argJson.getString("description"));
                                    }
                                }});
                                put("output",new JSONObject(){{
                                    for(Object arg : param.getJSONArray("outputParamList")){
                                        JSONObject argJson  = JSONObject.parseObject(arg.toString());
                                        put(argJson.getString("key"),argJson.getString("value"));
                                    }
                                }});

                            }});
                        }
                    }});
                }});
            }
        }});



    }

    /**
     * 暂停作业
     *
     * @param jobVo 作业
     */
    @Override
    public void pause(AutoexecJobVo jobVo) {

    }

    /**
     * 中止作业
     *
     * @param jobVo 作业
     */
    @Override
    public void stop(AutoexecJobVo jobVo) {

    }

    /**
     * 重置作业节点
     *
     * @param jobPhaseNode 重置作业节点
     */
    @Override
    public void reset(AutoexecJobPhaseNodeVo jobPhaseNode) {

    }

    /**
     * 忽略作业节点
     *
     * @param jobPhase 作业剧本
     */
    @Override
    public void ignore(AutoexecJobPhaseVo jobPhase) {

    }

    /**
     * 实时获取作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param position     日志位置
     * @param path         日志path
     * @return 日志内容
     */
    @Override
    public AutoexecJobLogVo logTail(AutoexecJobPhaseNodeVo jobPhaseNode, Integer position, String path) {
        return null;
    }

    /**
     * 下载作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param path         日志path
     */
    @Override
    public void logDownload(AutoexecJobPhaseNodeVo jobPhaseNode, String path) {

    }
}
