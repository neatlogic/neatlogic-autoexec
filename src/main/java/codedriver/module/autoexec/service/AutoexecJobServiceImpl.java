/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.AutoexecJobParamContentVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Override
    public void saveAutoexecCombopJob(AutoexecCombopVo combopVo, String source, Integer threadCount, JSONArray jobParamList) {
        AutoexecCombopConfigVo config = combopVo.getConfig();
        AutoexecJobVo jobVo = new AutoexecJobVo(combopVo, CombopOperationType.COMBOP.getValue(), source, threadCount,jobParamList);
        autoexecJobMapper.insertJob(jobVo);
        autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(jobVo.getParamHash(),jobVo.getParamStr()));
        //TODO 保存流水线执行目标
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.get(i);
            AutoexecJobPhaseVo jobPhaseVo = new AutoexecJobPhaseVo(autoexecCombopPhaseVo, i, jobVo.getId());
            autoexecJobMapper.insertJobPhase(jobPhaseVo);
            AutoexecCombopPhaseConfigVo phaseConfigVo = autoexecCombopPhaseVo.getConfig();
            List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : combopPhaseOperationList) {
                String operationType = autoexecCombopPhaseOperationVo.getOperationType();
                Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                AutoexecJobPhaseOperationVo operationVo = null;
                if (CombopOperationType.SCRIPT.getValue().equalsIgnoreCase(operationType)) {
                    AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
                    AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
                    List<AutoexecScriptLineVo> scriptLineVoList = autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
                    operationVo = new AutoexecJobPhaseOperationVo(autoexecCombopPhaseOperationVo, jobPhaseVo, scriptVo, scriptVersionVo, scriptLineVoList);
                    autoexecJobMapper.insertJobPhaseOperation(operationVo);
                    autoexecJobMapper.insertJobParamContent(new AutoexecJobParamContentVo(operationVo.getScriptHash(), operationVo.getScript()));
                }
            }
        }
    }

}
