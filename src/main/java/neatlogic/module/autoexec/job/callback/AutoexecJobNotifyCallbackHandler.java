/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
