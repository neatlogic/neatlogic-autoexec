package neatlogic.module.autoexec.importexport.handler;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.importexport.constvalue.FrameworkImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@Component
public class CombopImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public ImportExportHandlerType getType() {
        return FrameworkImportExportHandlerType.AUTOEXEC_COMBOP;
    }

    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecCombopMapper.getAutoexecCombopByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecCombopVo autoexecCombop = autoexecCombopMapper.getAutoexecCombopByName(importExportVo.getName());
        if (autoexecCombop == null) {
            throw new AutoexecCombopNotFoundException(importExportVo.getName());
        }
        return autoexecCombop.getId();
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecCombopVo autoexecCombop = importExportVo.getData().toJavaObject(AutoexecCombopVo.class);
        AutoexecCombopVo oldAutoexecCombop = autoexecCombopMapper.getAutoexecCombopByName(importExportVo.getName());
        if (oldAutoexecCombop != null) {
            autoexecCombop.setId(oldAutoexecCombop.getId());
        } else {
            if (autoexecCombopMapper.checkAutoexecCombopIsExists(autoexecCombop.getId()) > 0) {
                autoexecCombop.setId(null);
            }
        }
        importHandle(autoexecCombop, primaryChangeList);
        autoexecCombopService.saveAutoexecCombop(autoexecCombop);
        AutoexecCombopVersionVo version = autoexecCombop.getVersionList().get(0);
        version.setCombopId(autoexecCombop.getId());
        autoexecCombopService.saveAutoexecCombopVersion(version);
        autoexecCombopVersionMapper.disableAutoexecCombopVersionByCombopId(version.getCombopId());
        autoexecCombopVersionMapper.enableAutoexecCombopVersionById(version.getId());
        version.setStatus(ScriptVersionStatus.PASSED.getValue());
        version.setReviewer(UserContext.get().getUserUuid());
        autoexecCombopVersionMapper.updateAutoexecCombopVersionStatusById(version);
        return autoexecCombop.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecCombopVo autoexecCombop = autoexecCombopService.getAutoexecCombopById(id);
        if (autoexecCombop == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        if (autoexecCombop.getActiveVersionId() == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombop.getName());
        }
        AutoexecCombopVersionVo version = autoexecCombopVersionMapper.getAutoexecCombopVersionById(autoexecCombop.getActiveVersionId());
        List<AutoexecCombopVersionVo> versionList = new ArrayList<>();
        versionList.add(version);
        autoexecCombop.setVersionList(versionList);
        exportHandle(autoexecCombop, dependencyList, zipOutputStream);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecCombop.getName());
        importExportVo.setDataWithObject(autoexecCombop);
        return importExportVo;
    }

    private void importHandle(AutoexecCombopVo autoexecCombop, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        handleDependency(IMPORT, autoexecCombop, null, null, primaryChangeList);
    }

    private void exportHandle(AutoexecCombopVo autoexecCombop, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        handleDependency(EXPORT, autoexecCombop, dependencyList, zipOutputStream, null);
    }

    private void handleDependency(String action, AutoexecCombopVo autoexecCombop, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (autoexecCombop.getTypeId() != null) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecCombop.getTypeId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    autoexecCombop.setTypeId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecCombop.getTypeId(), dependencyList, zipOutputStream);
            }
        }
        AutoexecCombopConfigVo combopConfig = autoexecCombop.getConfig();
        if (combopConfig != null) {
            InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfig = combopConfig.getInvokeNotifyPolicyConfig();
            if (invokeNotifyPolicyConfig != null && invokeNotifyPolicyConfig.getIsCustom() == 1 && invokeNotifyPolicyConfig.getPolicyId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.NOTIFY_POLICY, invokeNotifyPolicyConfig.getPolicyId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        autoexecCombop.setTypeId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(FrameworkImportExportHandlerType.NOTIFY_POLICY, invokeNotifyPolicyConfig.getPolicyId(), dependencyList, zipOutputStream);
                }
            }
        }
        AutoexecCombopVersionVo version = autoexecCombop.getVersionList().get(0);
        AutoexecCombopVersionConfigVo config = version.getConfig();
        if (config != null) {
            // 阶段
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (AutoexecCombopPhaseVo combopPhase : combopPhaseList) {
                    handleCombopPhase(action, combopPhase, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 阶段组
            List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
            if (CollectionUtils.isNotEmpty(combopGroupList)) {
                for (AutoexecCombopGroupVo combopGroup : combopGroupList) {
                    handleCombopGroup(action, combopGroup, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            // 执行账户
            if (config.getExecuteConfig() != null && config.getExecuteConfig().getProtocolId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        config.getExecuteConfig().setProtocolId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(CmdbImportExportHandlerType.PROTOCOL, config.getExecuteConfig().getProtocolId(), dependencyList, zipOutputStream);
                }
            }
            // 场景
            List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
            if (CollectionUtils.isNotEmpty(scenarioList)) {
                for (AutoexecCombopScenarioVo combopScenario : scenarioList) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, combopScenario.getScenarioId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            combopScenario.setScenarioId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, combopScenario.getScenarioId(), dependencyList, zipOutputStream);
                    }
                }
                if (config.getDefaultScenarioId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO, config.getDefaultScenarioId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            config.setDefaultScenarioId((Long) newPrimaryKey);
                        }
                    }
                }
            }
            // 作业参数
            List<AutoexecParamVo> paramList = config.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(paramList)) {
                List<String> paramTypeList = new ArrayList<>();
                paramTypeList.add(ParamType.SELECT.getValue());
                paramTypeList.add(ParamType.MULTISELECT.getValue());
                paramTypeList.add(ParamType.RADIO.getValue());
                paramTypeList.add(ParamType.CHECKBOX.getValue());
                for (AutoexecParamVo param : paramList) {
                    if (!paramTypeList.contains(param.getType())) {
                        continue;
                    }
                    AutoexecParamConfigVo paramConfig = param.getConfig();
                    if (paramConfig == null) {
                        continue;
                    }
                    if (!Objects.equals(paramConfig.getDataSource(), "matrix")) {
                        continue;
                    }
                    if (paramConfig.getMatrixUuid() == null) {
                        continue;
                    }
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.MATRIX, paramConfig.getMatrixUuid(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            paramConfig.setMatrixUuid((String) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(FrameworkImportExportHandlerType.MATRIX, paramConfig.getMatrixUuid(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
    }

    private void handleCombopGroup(String action, AutoexecCombopGroupVo combopGroup, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (Objects.equals(combopGroup.getPolicy(), AutoexecJobGroupPolicy.ONESHOT.getName())) {
            return;
        }
        AutoexecCombopGroupConfigVo config = combopGroup.getConfig();
        if (config == null) {
            return;
        }
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig == null) {
            return;
        }
        if (!Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
            return;
        }
        if (executeConfig.getProtocolId() != null) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    executeConfig.setProtocolId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList, zipOutputStream);
            }
        }
    }

    private void handleCombopPhase(String action, AutoexecCombopPhaseVo combopPhase, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecCombopPhaseConfigVo config = combopPhase.getConfig();
        if (config == null) {
            return;
        }
        // 执行目标配置
        AutoexecCombopExecuteConfigVo executeConfig = config.getExecuteConfig();
        if (executeConfig != null) {
            if (Objects.equals(executeConfig.getIsPresetExecuteConfig(), 1)) {
                if (executeConfig.getProtocolId() != null) {
                    if (action == IMPORT) {
                        Object newPrimaryKey = getNewPrimaryKey(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), primaryChangeList);
                        if (newPrimaryKey != null) {
                            executeConfig.setProtocolId((Long) newPrimaryKey);
                        }
                    } else if (action == EXPORT) {
                        doExportData(CmdbImportExportHandlerType.PROTOCOL, executeConfig.getProtocolId(), dependencyList, zipOutputStream);
                    }
                }
            }
        }
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = config.getPhaseOperationList();
        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
            handlerCombopPhaseOperation(action, phaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
        }
    }

    private void handlerCombopPhaseOperation(String action, AutoexecCombopPhaseOperationVo phaseOperationVo, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.SCRIPT.getValue())) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    phaseOperationVo.setOperationId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, phaseOperationVo.getOperationId(), dependencyList, zipOutputStream);
            }
        } else if (Objects.equals(phaseOperationVo.getOperationType(), ToolType.TOOL.getValue())) {
            if (action == IMPORT) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    phaseOperationVo.setOperationId((Long) newPrimaryKey);
                }
            } else if (action == EXPORT) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, phaseOperationVo.getOperationId(), dependencyList, zipOutputStream);
            }
        }
        AutoexecCombopPhaseOperationConfigVo phaseOperationConfig = phaseOperationVo.getConfig();
        if (phaseOperationConfig != null) {
            if (phaseOperationConfig.getProfileId() != null) {
                if (action == IMPORT) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        phaseOperationConfig.setProfileId((Long) newPrimaryKey);
                    }
                } else if (action == EXPORT) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, phaseOperationConfig.getProfileId(), dependencyList, zipOutputStream);
                }
            }
            List<ParamMappingVo> paramMappingList = phaseOperationConfig.getParamMappingList();
            if (CollectionUtils.isNotEmpty(paramMappingList)) {
                for (ParamMappingVo paramMappingVo : paramMappingList) {
                    if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        if (action == IMPORT) {
                            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), primaryChangeList);
                            if (newPrimaryKey != null) {
                                paramMappingVo.setValue(newPrimaryKey);
                            }
                        } else if (action == EXPORT) {
                            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList, zipOutputStream);
                        }
                    }
                }
            }
            List<ParamMappingVo> argumentMappingList = phaseOperationConfig.getArgumentMappingList();
            if (CollectionUtils.isNotEmpty(argumentMappingList)) {
                for (ParamMappingVo paramMappingVo : argumentMappingList) {
                    if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                        if (action == IMPORT) {
                            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), primaryChangeList);
                            if (newPrimaryKey != null) {
                                paramMappingVo.setValue(newPrimaryKey);
                            }
                        } else if (action == EXPORT) {
                            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, paramMappingVo.getValue(), dependencyList, zipOutputStream);
                        }
                    }
                }
            }
            List<AutoexecCombopPhaseOperationVo> ifList = phaseOperationConfig.getIfList();
            if (CollectionUtils.isNotEmpty(ifList)) {
                for (AutoexecCombopPhaseOperationVo ifPhaseOperationVo : ifList) {
                    handlerCombopPhaseOperation(action, ifPhaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> elseList = phaseOperationConfig.getElseList();
            if (CollectionUtils.isNotEmpty(elseList)) {
                for (AutoexecCombopPhaseOperationVo elsePhaseOperationVo : elseList) {
                    handlerCombopPhaseOperation(action, elsePhaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
            List<AutoexecCombopPhaseOperationVo> operations = phaseOperationConfig.getOperations();
            if (CollectionUtils.isNotEmpty(operations)) {
                for (AutoexecCombopPhaseOperationVo loopPhaseOperationVo : operations) {
                    handlerCombopPhaseOperation(action, loopPhaseOperationVo, dependencyList, zipOutputStream, primaryChangeList);
                }
            }
        }
    }
}
