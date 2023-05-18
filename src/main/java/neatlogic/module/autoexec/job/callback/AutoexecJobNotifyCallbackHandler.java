/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.job.callback;

import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyTriggerType;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.autoexec.message.handler.AutoexecJobMessageHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2022/11/14 17:40
 **/
@Component
public class AutoexecJobNotifyCallbackHandler extends AutoexecJobCallbackBase {

    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNotifyCallbackHandler.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getHandler() {
        return AutoexecJobNotifyCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        AutoexecJobNotifyTriggerType trigger = AutoexecJobNotifyTriggerType.getTrigger(jobVo.getStatus());
        if (trigger != null) {
            AutoexecJobVo jobInfo;
            // 开启一个新事务来查询父事务提交前的作业状态，如果新事务查出来的状态与当前jobVo的状态不同，则表示该状态未通知过
            TransactionStatus tx = TransactionUtil.openNewTx();
            try {
                jobInfo = autoexecJobMapper.getJobInfo(jobVo.getId());
            } finally {
                if (tx != null) {
                    TransactionUtil.commitTx(tx);
                }
            }
            if (jobInfo != null && Objects.equals(jobInfo.getOperationType(), CombopOperationType.COMBOP.getValue()) && !Objects.equals(jobVo.getStatus(), jobInfo.getStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        AutoexecJobNotifyTriggerType trigger = AutoexecJobNotifyTriggerType.getTrigger(jobVo.getStatus());
        if (trigger == null) {
            return;
        }
        AutoexecJobVo jobInfo = autoexecJobMapper.getJobInfo(jobVo.getId());
        if (jobInfo == null) {
            return;
        }
        if (!Objects.equals(jobInfo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            return;
        }
        Long operationId = jobInfo.getOperationId();
        if (operationId == null) {
            return;
        }
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(operationId);
        if (combopVo == null) {
            return;
        }
        AutoexecCombopConfigVo config = combopVo.getConfig();
        if (config == null) {
            return;
        }
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(config.getInvokeNotifyPolicyConfig());
        if (invokeNotifyPolicyConfigVo == null) {
            return;
        }
        // 触发点被排除，不用发送邮件
        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(trigger.getTrigger())) {
            return;
        }
        Long notifyPolicyId = invokeNotifyPolicyConfigVo.getPolicyId();
        if (notifyPolicyId == null) {
            return;
        }
        NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyById(notifyPolicyId);
        if (notifyPolicyVo == null || notifyPolicyVo.getConfig() == null) {
            return;
        }
        try {
            String notifyAuditMessage = jobInfo.getId() + "-" + jobInfo.getName();
            NotifyPolicyUtil.execute(notifyPolicyVo.getHandler(), trigger, AutoexecJobMessageHandler.class
                    , notifyPolicyVo, null, null, null
                    , jobInfo, null, notifyAuditMessage);
        } catch (Exception ex) {
            logger.error("自动化作业：" + jobInfo.getId() + "-" + jobInfo.getName() + "通知失败");
            logger.error(ex.getMessage(), ex);
        }
    }
}
