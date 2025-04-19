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

import com.alibaba.fastjson.JSON;
import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyTriggerType;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobUserType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.notify.dto.NotifyReceiverVo;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.autoexec.message.handler.AutoexecJobMessageHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.*;

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
        AutoexecJobNotifyTriggerType trigger = AutoexecJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
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
        boolean flag = false;
        StringBuilder notifyAuditMessageStringBuilder = new StringBuilder();
        try {
            AutoexecJobNotifyTriggerType notifyTriggerType = AutoexecJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
            if (notifyTriggerType != null) {
                notifyAuditMessageStringBuilder.append("触发点为 ").append(notifyTriggerType.getTrigger()).append("(").append(notifyTriggerType.getText()).append(")");
                AutoexecJobVo jobInfo = autoexecJobMapper.getJobInfo(jobVo.getId());
                if (jobInfo != null) {
                    notifyAuditMessageStringBuilder
                            .append(" 作业")
                            .append(jobInfo.getName())
                            .append("(")
                            .append(jobVo.getId())
                            .append(")")
                            .append("的状态为")
                            .append(jobVo.getStatus());
                    notifyAuditMessageStringBuilder.append(" 执行用户").append(jobInfo.getExecUser());
                    if (Objects.equals(jobInfo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
                        Long operationId = jobInfo.getOperationId();
                        if (operationId != null) {
                            notifyAuditMessageStringBuilder.append(" operation_id为").append(operationId);
                            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(operationId);
                            if (combopVo != null) {
                                notifyAuditMessageStringBuilder.append(" 找到组合工具").append(combopVo.getName()).append("(").append(combopVo.getId()).append(")");
                                AutoexecCombopConfigVo config = combopVo.getConfig();
                                if (config != null) {
                                    InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = config.getInvokeNotifyPolicyConfig();
                                    if (invokeNotifyPolicyConfigVo != null) {
                                        // 触发点被排除，不用发送邮件
                                        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
                                        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(notifyTriggerType.getTrigger())) {
                                            notifyAuditMessageStringBuilder.append(" 通知策略设置触发时机中排除了触发点").append(notifyTriggerType.getTrigger()).append("(").append(notifyTriggerType.getText()).append(")");
                                        } else {
                                            NotifyPolicyVo notifyPolicyVo = null;
                                            if (invokeNotifyPolicyConfigVo.getIsCustom() == 1) {
                                                notifyAuditMessageStringBuilder.append(" 设置通知策略ID为 ");
                                                if (invokeNotifyPolicyConfigVo.getPolicyId() != null) {
                                                    notifyAuditMessageStringBuilder.append(invokeNotifyPolicyConfigVo.getPolicyId());
                                                    notifyPolicyVo = notifyMapper.getNotifyPolicyById(invokeNotifyPolicyConfigVo.getPolicyId());
                                                    if (notifyPolicyVo == null) {
                                                        notifyAuditMessageStringBuilder.append("，但是该通知策略不存在");
                                                    } else {
                                                        notifyAuditMessageStringBuilder.append("，找到默认通知策略").append(notifyPolicyVo.getName()).append("(").append(notifyPolicyVo.getId()).append(")");
                                                    }
                                                } else {
                                                    notifyAuditMessageStringBuilder.append("null");
                                                }
                                            } else {
                                                notifyAuditMessageStringBuilder.append(" 没有设置通知策略 ");
                                                if (invokeNotifyPolicyConfigVo.getHandler() != null) {
                                                    notifyAuditMessageStringBuilder.append(" 通过通知策略handler=").append(invokeNotifyPolicyConfigVo.getHandler());
                                                    notifyPolicyVo = notifyMapper.getDefaultNotifyPolicyByHandler(invokeNotifyPolicyConfigVo.getHandler());
                                                    if (notifyPolicyVo == null) {
                                                        notifyAuditMessageStringBuilder.append(" ，找不到默认通知策略");
                                                    } else {
                                                        notifyAuditMessageStringBuilder.append(" ，找到默认通知策略 ").append(notifyPolicyVo.getName()).append("(").append(notifyPolicyVo.getId()).append(")");
                                                    }
                                                } else {
                                                    notifyAuditMessageStringBuilder.append(" 由于通知策略handler为null，无法找到默认通知策略");
                                                }
                                            }
                                            if (notifyPolicyVo != null) {
                                                if (notifyPolicyVo.getConfig() != null) {
                                                    Map<String, List<NotifyReceiverVo>> receiverMap = new HashMap<>();
                                                    if (!Objects.equals(jobInfo.getExecUser(), SystemUser.SYSTEM.getUserUuid())) {
                                                        receiverMap.computeIfAbsent(JobUserType.EXEC_USER.getValue(), k -> new ArrayList<>())
                                                                .add(new NotifyReceiverVo(GroupSearch.USER.getValue(), jobInfo.getExecUser()));
                                                    }
                                                    flag = true;
                                                    NotifyPolicyUtil.execute(
                                                            notifyPolicyVo.getHandler(),
                                                            notifyTriggerType,
                                                            AutoexecJobMessageHandler.class,
                                                            notifyPolicyVo,
                                                            null,
                                                            null,
                                                            receiverMap,
                                                            jobInfo,
                                                            null,
                                                            notifyAuditMessageStringBuilder.toString());
                                                } else {
                                                    notifyAuditMessageStringBuilder.append(" 通知策略config为null，不触发通知");
                                                }
                                            }
                                        }
                                    } else {
                                        notifyAuditMessageStringBuilder.append(" 该组合工具的config为").append(JSON.toJSONString(config)).append("，没有通知策略信息").append("，无法触发通知");
                                    }
                                } else {
                                    notifyAuditMessageStringBuilder.append(" 该组合工具的config为null，无法找到通知策略信息").append("，无法触发通知");
                                }
                            } else {
                                notifyAuditMessageStringBuilder.append(" 在autoexec_combop表中找不到id为").append(operationId).append("的数据").append("，无法触发通知");
                            }
                        } else {
                            notifyAuditMessageStringBuilder.append(" 该作业的operation_id为null，无法找到组合工具").append("，无法触发通知");
                        }
                    } else {
                        notifyAuditMessageStringBuilder.append(" 该作业operation_type不是").append(CombopOperationType.COMBOP.getValue()).append("类型").append("，无法触发通知");
                    }
                } else {
                    notifyAuditMessageStringBuilder
                            .append(" 作业")
                            .append(jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY)
                            .append("(")
                            .append(jobVo.getId())
                            .append(")")
                            .append("的状态为")
                            .append(jobVo.getStatus());
                    notifyAuditMessageStringBuilder.append(" 在autoexec_job表中找不到该作业的数据").append("，无法触发通知");
                }
            } else {
                notifyAuditMessageStringBuilder
                        .append("根据作业")
                        .append(jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY)
                        .append("(")
                        .append(jobVo.getId())
                        .append(")")
                        .append("的状态")
                        .append(jobVo.getStatus())
                        .append("，找不到触发点")
                        .append("，无法触发通知");
            }
        } catch (Exception ex) {
            logger.error("自动化作业：" + jobVo.getId() + "-" + (jobVo.getName() != null ? jobVo.getName() : StringUtils.EMPTY) + "通知失败");
            logger.error(ex.getMessage(), ex);
        } finally {
            if (!flag) {
                Logger notifyAuditLogger = LoggerFactory.getLogger("notifyAudit");
                notifyAuditLogger.info("\n" + notifyAuditMessageStringBuilder);
            }
        }
    }
}
