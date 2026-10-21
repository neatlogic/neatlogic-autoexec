package neatlogic.module.autoexec.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import neatlogic.framework.autoexec.crossover.IAutoexecScriptServiceCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.*;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.dependency.AutoexecScript2ScriptDependencyHandler;
import neatlogic.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 脚本库公共门面，跨模块调用复用原业务服务和事务，不依赖 Skill。 */
@Service
public class AutoexecScriptCrossoverService implements IAutoexecScriptServiceCrossoverService {
    private static final Logger logger = LoggerFactory.getLogger(AutoexecScriptCrossoverService.class);
    @Resource private AutoexecScriptService scriptService;
    @Resource private AutoexecService autoexecService;
    @Resource private AutoexecScriptMapper scriptMapper;
    @Resource private AutoexecToolMapper toolMapper;
    @Resource private AutoexecTypeMapper typeMapper;
    @Resource private AutoexecRiskMapper riskMapper;
    @Resource private AutoexecProfileMapper profileMapper;

    /** 原依赖跳转链接继续由脚本模块生成。 */
    @Override public DependencyInfoVo getScriptDependencyPageUrl(Map<String, Object> map, Long id, String group, String path) {
        return scriptService.getScriptDependencyPageUrl(map, id, group);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVo getScriptBaseInfoById(Long id) {
        return scriptMapper.getScriptBaseInfoById(id);
    }

    /** 委托原脚本库实现。 */
    @Override public List<AutoexecScriptVo> getAutoexecScriptByNameList(List<String> names) {
        return scriptMapper.getAutoexecScriptByNameList(names);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVo getScriptLockById(Long id) {
        return scriptMapper.getScriptLockById(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVersionVo getActiveVersionByScriptId(Long id) {
        return scriptMapper.getActiveVersionByScriptId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVersionVo getActiveVersionLockByScriptId(Long id) {
        return scriptMapper.getActiveVersionLockByScriptId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public List<AutoexecScriptVersionParamVo> getParamListByVersionId(Long id) {
        return scriptMapper.getParamListByVersionId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptArgumentVo getArgumentByVersionId(Long id) {
        return scriptMapper.getArgumentByVersionId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public List<Long> getLibScriptIdListByVersionId(Long id) {
        return scriptMapper.getLibScriptIdListByVersionId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecToolVo getToolById(Long id) {
        return toolMapper.getToolById(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecToolVo getToolByName(String name) {
        return toolMapper.getToolByName(name);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long id) {
        return scriptService.getScriptVersionDetailByVersionId(id);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVo resolveScriptByName(String name, String catalogPath) {
        return scriptService.resolveScriptByName(name, catalogPath);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecScriptVo resolveScriptByName(String name, String catalogPath, List<AutoexecScriptVo> candidates) {
        return scriptService.resolveScriptByName(name, catalogPath, candidates);
    }

    /** 委托原脚本库实现。 */
    @Override public Long createCatalogByCatalogPath(String catalogPath) {
        return scriptService.createCatalogByCatalogPath(catalogPath);
    }

    /** 委托原脚本库实现。 */
    @Override public AutoexecProfileVo getProfileVoByName(String name) {
        return profileMapper.getProfileVoByName(name);
    }

    /** 分类只按已有记录解析。 */
    @Override public Long resolveTypeId(String name) {
        Long id = typeMapper.getTypeIdByName(name);
        return id != null && typeMapper.getTypeById(id) != null ? id : null;
    }

    /** 操作级别只按已有记录解析。 */
    @Override public Long resolveRiskId(String name) {
        Long id = riskMapper.getRiskIdByName(name);
        return id != null && riskMapper.getAutoexecRiskById(id) != null ? id : null;
    }

    /** 沿用原创建、参数校验、依赖和审核流程，外层 Skill 事务失败时一同回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importScriptVersion(AutoexecScriptVo script, AutoexecScriptVersionVo version,
                                    boolean insert, Long profileId, List<Long> libraries) throws Exception {
        scriptService.validateScriptBaseInfo(script);
        scriptService.persistScriptBaseInfo(script, insert);
        if (profileId != null) {
            profileMapper.insertAutoexecProfileOperation(profileId, Collections.singletonList(script.getId()),
                    "script", System.currentTimeMillis());
            script.setDefaultProfileId(profileId);
            scriptService.persistScriptBaseInfo(script, false);
        }
        version.setScriptId(script.getId());
        scriptMapper.insertScriptVersion(version);
        if (version.getParamList() != null) {
            autoexecService.validateParamList(version.getParamList());
            scriptService.saveParamList(version.getId(), version.getParamList());
        }
        if (version.getArgument() != null) {
            autoexecService.validateArgument(version.getArgument());
            version.getArgument().setScriptVersionId(version.getId());
            scriptMapper.insertScriptVersionArgument(version.getArgument());
        }
        if (!"package".equals(version.getParser()))
            scriptService.saveLineList(script.getId(), version.getId(), version.getLineList());
        if (!libraries.isEmpty()) {
            scriptMapper.insertScriptVersionUseLib(version.getId(), libraries);
            for (Long id : libraries) DependencyManager.insert(AutoexecScript2ScriptDependencyHandler.class, id, version.getId());
        }
        scriptService.reviewVersion(version, "pass", null);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 原索引只在事务提交后更新。 */
            @Override public void afterCommit() {
                try {
                    IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                    if (handler != null) handler.createIndex(version.getId());
                } catch (Exception ex) {
                    logger.error("索引导入脚本版本失败，versionId={}", version.getId(), ex);
                }
            }
        });
    }
}
