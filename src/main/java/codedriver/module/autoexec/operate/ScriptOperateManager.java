/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.operate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptAndToolOperate;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.OperateVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScriptOperateManager {

    private static Map<ScriptAndToolOperate, Function<Long, OperateVo>> operateMap = new HashMap<>();

    private Set<Long> scriptIdSet;

    private static AutoexecScriptMapper autoexecScriptMapper;

    @Autowired
    private ScriptOperateManager(AutoexecScriptMapper _autoexecScriptMapper) {
        this.autoexecScriptMapper = _autoexecScriptMapper;
    }

    @PostConstruct
    public void init() {

        operateMap.put(ScriptAndToolOperate.DELETE, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.DELETE.getValue(), ScriptAndToolOperate.DELETE.getText());
                if (autoexecScriptMapper.getReferenceCountByScriptId(id) > 0) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("已被组合工具引用");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.VERSIONDELETE, (id) -> {
            // 只剩一个版本或当前版本处于激活状态时，不可删除
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null) {
                    int versionCount = autoexecScriptMapper.getVersionCountByScriptId(version.getScriptId());
                    if (versionCount > 1 && !Objects.equals(version.getIsActive(), 1)) {
                        return new OperateVo(ScriptAndToolOperate.VERSIONDELETE.getValue(), ScriptAndToolOperate.VERSIONDELETE.getText());
                    }
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.COPY, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.COPY.getValue(), ScriptAndToolOperate.COPY.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.TEST, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.COMPARE, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.VALIDATE, (id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能校验
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.VALIDATE.getValue(), ScriptAndToolOperate.VALIDATE.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SAVE, (id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能保存
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.SAVE.getValue(), ScriptAndToolOperate.SAVE.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SUBMIT, (id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能提交审核
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.SUBMIT.getValue(), ScriptAndToolOperate.SUBMIT.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.PASS, (id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能通过
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    return new OperateVo(ScriptAndToolOperate.PASS.getValue(), ScriptAndToolOperate.PASS.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.REJECT, (id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能驳回
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    return new OperateVo(ScriptAndToolOperate.REJECT.getValue(), ScriptAndToolOperate.REJECT.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.GENERATETOCOMBOP, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_COMBOP_MODIFY.class.getSimpleName())) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
                int hasBeenGeneratedToCombop = autoexecScriptMapper.checkScriptHasBeenGeneratedToCombop(id);
                Integer currentVersion = autoexecScriptMapper.getActiveVersionNumberByScriptId(id);
                if (hasBeenGeneratedToCombop > 0) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("已经被发布为组合工具");
                } else if (currentVersion == null) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("没有激活版本");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.EXPORT, (id) -> {
            if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.EXPORT.getValue(), ScriptAndToolOperate.EXPORT.getText());
            }
            return null;
        });

    }

    public Map<ScriptAndToolOperate, Function<Long, OperateVo>> getOperateMap() {
        return operateMap;
    }

    /**
     * 批量获取脚本操作权限
     *
     * @return
     */
    public Map<Long, List<OperateVo>> getOperateListMap() {
        Map<Long, List<OperateVo>> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(scriptIdSet)) {
            return resultMap;
        }
        List<Long> idList = scriptIdSet.stream().collect(Collectors.toList());
        Boolean hasManageAuth = AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
        Boolean hasModifyAuth = AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName());
        Boolean hasSearchAuth = AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName());
        // 查询脚本是否被组合工具引用
        List<AutoexecScriptVo> referenceCountList = autoexecScriptMapper.getReferenceCountListByScriptIdList(idList);
        // 查询脚本是否已经被发布为组合工具
        List<AutoexecScriptVo> hasBeenGeneratedToCombopList = autoexecScriptMapper.checkScriptListHasBeenGeneratedToCombop(idList);
        // 查询脚本当前激活版本号
        List<AutoexecScriptVo> activeVersionNumberList = autoexecScriptMapper.getActiveVersionNumberListByScriptIdList(idList);
        Map<Long, Boolean> referenceCountListMap = new HashMap<>();
        Map<Long, Boolean> hasBeenGeneratedToCombopListMap = new HashMap<>();
        Map<Long, Boolean> hasActiveVersionMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(referenceCountList)) {
            referenceCountList.stream().forEach(o -> referenceCountListMap.put(o.getId(), o.getReferenceCount() > 0 ? true : false));
        }
        if (CollectionUtils.isNotEmpty(hasBeenGeneratedToCombopList)) {
            hasBeenGeneratedToCombopList.stream().forEach(o -> hasBeenGeneratedToCombopListMap.put(o.getId(), o.getHasBeenGeneratedToCombop() > 0 ? true : false));
        }
        if (CollectionUtils.isNotEmpty(activeVersionNumberList)) {
            activeVersionNumberList.stream().forEach(o -> hasActiveVersionMap.put(o.getId(), o.getCurrentVersion() != null ? true : false));
        }
        for (Long id : idList) {
            List<OperateVo> operateList = new ArrayList<>();
            if (hasModifyAuth) {
                OperateVo generateToCombop = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
                if (MapUtils.isNotEmpty(hasBeenGeneratedToCombopListMap) && Objects.equals(hasBeenGeneratedToCombopListMap.get(id), true)) {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("已经发布为组合工具");
                } else if (MapUtils.isNotEmpty(hasActiveVersionMap) && !Objects.equals(hasActiveVersionMap.get(id), true)) {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("没有激活版本");
                }
                operateList.add(generateToCombop);
            }
            if (hasSearchAuth) {
                operateList.add(new OperateVo(ScriptAndToolOperate.COPY.getValue(), ScriptAndToolOperate.COPY.getText()));
                operateList.add(new OperateVo(ScriptAndToolOperate.EXPORT.getValue(), ScriptAndToolOperate.EXPORT.getText()));
            }
            if (hasManageAuth) {
                OperateVo delete = new OperateVo(ScriptAndToolOperate.DELETE.getValue(), ScriptAndToolOperate.DELETE.getText());
                if (MapUtils.isNotEmpty(referenceCountListMap) && Objects.equals(referenceCountListMap.get(id), true)) {
                    delete.setDisabled(1);
                    delete.setDisabledReason("已经被组合工具引用");
                }
                operateList.add(delete);
            }
            if (CollectionUtils.isNotEmpty(operateList)) {
                resultMap.put(id, operateList);
            }
        }
        return resultMap;
    }

    public ScriptOperateManager() {
    }

    public ScriptOperateManager(Builder builder) {
        this.scriptIdSet = builder.scriptIdSet;
    }

    public class Builder {
        List<OperateVo> operateList; // 操作列表

        Set<Long> scriptIdSet = new HashSet<>();

        //String userUuid; // 当前用户uuid

        public Builder() {
            operateList = new ArrayList<>();
            //this.userUuid = UserContext.get().getUserUuid();
        }

        public ScriptOperateManager managerBuild() {
            return new ScriptOperateManager(this);
        }

        public Builder addScriptId(Long... scriptIds) {
            if (scriptIds != null) {
                for (Long id : scriptIds) {
                    scriptIdSet.add(id);
                }
            }
            return this;
        }

        public List<OperateVo> build() {
            return operateList;
        }

        public Builder setDelete(Long scriptId) {
            if (scriptId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.DELETE).apply(scriptId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setVersionDelete(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.VERSIONDELETE).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setCopy() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.COPY).apply(null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setTest() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.TEST).apply(null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setCompare() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.COMPARE).apply(null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setValidate(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.VALIDATE).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setSave(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.SAVE).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setSubmit(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.SUBMIT).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setPass(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.PASS).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setReject(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.REJECT).apply(versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setGenerateToCombop(Long scriptId) {
            if (scriptId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.GENERATETOCOMBOP).apply(scriptId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setExport() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.EXPORT).apply(null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

    }

}
