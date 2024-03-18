/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.operate;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.ScriptAndToolOperate;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.OperateVo;
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
            OperateVo vo = new OperateVo(ScriptAndToolOperate.DELETE.getValue(), ScriptAndToolOperate.DELETE.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
//            } else if (autoexecScriptMapper.getReferenceCountByScriptId(id) > 0) {
            } else if (DependencyManager.getDependencyCount(AutoexecFromType.SCRIPT, id) > 0) {
                vo.setDisabled(1);
                vo.setDisabledReason("当前自定义工具已被组合工具引用，无法删除");
            }
            return vo;
        });

        operateMap.put(ScriptAndToolOperate.VERSION_DELETE, (id) -> {
            // 只剩一个版本或当前版本处于激活状态时，不可删除
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.VERSION_DELETE.getValue(), ScriptAndToolOperate.VERSION_DELETE.getText());
                int versionCount = autoexecScriptMapper.getVersionCountByScriptId(version.getScriptId());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (versionCount <= 1 || Objects.equals(version.getIsActive(), 1)) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("只剩一个版本或当前版本处于激活状态时，不可删除");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.COPY, (id) -> {
            OperateVo vo = new OperateVo(ScriptAndToolOperate.COPY.getValue(), ScriptAndToolOperate.COPY.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
            }
            return vo;
        });

        operateMap.put(ScriptAndToolOperate.TEST, (id) -> {
            OperateVo vo = new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
            }
            return vo;
        });

        operateMap.put(ScriptAndToolOperate.COMPARE, (id) -> {
            OperateVo vo = new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
            }
            return vo;
        });

        operateMap.put(ScriptAndToolOperate.VALIDATE, (id) -> {
            // 拥有脚本审核或维护权限，且处于草稿、已驳回状态才能校验
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.VALIDATE.getValue(), ScriptAndToolOperate.VALIDATE.getText());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (!Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        && !Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("版本处于草稿或已驳回状态才能校验");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SAVE, (id) -> {
            // 拥有脚本审核或维护权限，且处于草稿、已驳回状态才能保存
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.SAVE.getValue(), ScriptAndToolOperate.SAVE.getText());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (!Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        && !Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("版本处于草稿或已驳回状态才能保存");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SUBMIT, (id) -> {
            // 拥有脚本审核或维护权限，且处于草稿、已驳回状态才能提交审核
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.SUBMIT.getValue(), ScriptAndToolOperate.SUBMIT.getText());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (!Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        && !Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("版本处于草稿或已驳回状态才能提交审核");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.PASS, (id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能通过
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.PASS.getValue(), ScriptAndToolOperate.PASS.getText());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (!Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("版本处于待审核状态才能审核通过");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.REJECT, (id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能驳回
            AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
            if (version != null) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.REJECT.getValue(), ScriptAndToolOperate.REJECT.getText());
                if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("无权限，请联系管理员");
                } else if (!Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("版本处于待审核状态才能驳回");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.GENERATETOCOMBOP, (id) -> {
            OperateVo vo = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
            } else {
                int hasBeenGeneratedToCombop = autoexecScriptMapper.checkScriptHasBeenGeneratedToCombop(id);
                Integer currentVersion = autoexecScriptMapper.getActiveVersionNumberByScriptId(id);
                AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
                if (hasBeenGeneratedToCombop > 0) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("已发布为组合工具");
                } else if (currentVersion == null) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("当前自定义工具未有激活版本，无法发布为组合工具");
                } else if (scriptVo != null && scriptVo.getIsLib() == 1) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("当前自定义工具是库文件，无法发布为组合工具");
                }
            }
            return vo;
        });

        operateMap.put(ScriptAndToolOperate.EXPORT, (id) -> {
            OperateVo vo = new OperateVo(ScriptAndToolOperate.EXPORT.getValue(), ScriptAndToolOperate.EXPORT.getText());
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                vo.setDisabled(1);
                vo.setDisabledReason("无权限，请联系管理员");
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
        Boolean hasCombopAddAuth = AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), AUTOEXEC_COMBOP_ADD.class.getSimpleName());
        // 查询脚本是否被组合工具引用
//        List<AutoexecScriptVo> referenceCountList = autoexecScriptMapper.getReferenceCountListByScriptIdList(idList);
        // 查询脚本是否已经被发布为组合工具
        List<Long> hasBeenGeneratedToCombopList = autoexecScriptMapper.checkScriptListHasBeenGeneratedToCombop(idList);
        // 查询脚本当前激活版本号
        List<AutoexecScriptVo> activeVersionNumberList = autoexecScriptMapper.getActiveVersionNumberListByScriptIdList(idList);
        // 查询脚本是否为库文件
        List<Long> isLibScriptIdList = autoexecScriptMapper.getIsLibScriptIdByScriptIdList(idList);
//        Map<Long, Boolean> referenceCountMap = new HashMap<>();
//        Map<Long, Boolean> hasBeenGeneratedToCombopMap = new HashMap<>();
        Map<Long, Boolean> hasActiveVersionMap = new HashMap<>();
//        if (CollectionUtils.isNotEmpty(referenceCountList)) {
//            referenceCountList.stream().forEach(o -> referenceCountMap.put(o.getId(), o.getReferenceCount() > 0 ? true : false));
//        }
//        if (CollectionUtils.isNotEmpty(hasBeenGeneratedToCombopList)) {
//            hasBeenGeneratedToCombopList.stream().forEach(o -> hasBeenGeneratedToCombopMap.put(o.getId(), o.getHasBeenGeneratedToCombop() > 0 ? true : false));
//        }
        if (CollectionUtils.isNotEmpty(activeVersionNumberList)) {
            activeVersionNumberList.stream().forEach(o -> hasActiveVersionMap.put(o.getId(), o.getCurrentVersion() != null ? true : false));
        }
        for (Long id : idList) {
            List<OperateVo> operateList = new ArrayList<>();
            OperateVo generateToCombop = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
            OperateVo copy = new OperateVo(ScriptAndToolOperate.COPY.getValue(), ScriptAndToolOperate.COPY.getText());
            OperateVo export = new OperateVo(ScriptAndToolOperate.EXPORT.getValue(), ScriptAndToolOperate.EXPORT.getText());
            OperateVo delete = new OperateVo(ScriptAndToolOperate.DELETE.getValue(), ScriptAndToolOperate.DELETE.getText());
            operateList.add(generateToCombop);
            operateList.add(copy);
            operateList.add(export);
            operateList.add(delete);
            if (hasCombopAddAuth) {
//                if (MapUtils.isNotEmpty(hasBeenGeneratedToCombopMap) && Objects.equals(hasBeenGeneratedToCombopMap.get(id), true)) {
                if (hasBeenGeneratedToCombopList.contains(id)) {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("已发布为组合工具");
                } else if (MapUtils.isNotEmpty(hasActiveVersionMap) && !Objects.equals(hasActiveVersionMap.get(id), true)) {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("当前自定义工具未有激活版本，无法发布为组合工具");
                } else if (isLibScriptIdList.contains(id)) {
                    generateToCombop.setDisabled(1);
                    generateToCombop.setDisabledReason("当前自定义工具是库文件，无法发布为组合工具");
                }
            } else {
                generateToCombop.setDisabled(1);
                generateToCombop.setDisabledReason("无权限，请联系管理员");
            }
            if (!hasModifyAuth) {
                copy.setDisabled(1);
                copy.setDisabledReason("无权限，请联系管理员");
            }
            if (!hasSearchAuth) {
                export.setDisabled(1);
                export.setDisabledReason("无权限，请联系管理员");
            }
            if (hasManageAuth) {
//                if (MapUtils.isNotEmpty(referenceCountMap) && Objects.equals(referenceCountMap.get(id), true)) {
                if (DependencyManager.getDependencyCount(AutoexecFromType.SCRIPT, id) > 0) {
                    delete.setDisabled(1);
                    delete.setDisabledReason("当前自定义工具已被组合工具引用，无法删除");
                }
            } else {
                delete.setDisabled(1);
                delete.setDisabledReason("无权限，请联系管理员");
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

        public Builder() {
            operateList = new ArrayList<>();
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
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.VERSION_DELETE).apply(versionId);
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
