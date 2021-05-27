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
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiFunction;

@Component
public class ScriptOperateManager {

    private static Map<ScriptAndToolOperate, BiFunction<String, Long, OperateVo>> operateMap = new HashMap<>();

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @PostConstruct
    public void init() {

        operateMap.put(ScriptAndToolOperate.DELETE, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                OperateVo vo = new OperateVo(ScriptAndToolOperate.DELETE.getValue(), ScriptAndToolOperate.DELETE.getText());
                if (autoexecScriptMapper.getReferenceCountByScriptId(id) > 0) {
                    vo.setDisabled(1);
                    vo.setDisabledReason("已被组合工具引用");
                }
                return vo;
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.VERSIONDELETE, (userUuid, id) -> {
            // 只剩一个版本或当前版本处于激活状态时，不可删除
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
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

        operateMap.put(ScriptAndToolOperate.COPY, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.COPY.getValue(), ScriptAndToolOperate.COPY.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.TEST, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.COMPARE, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText());
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.VALIDATE, (userUuid, id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能校验
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.VALIDATE.getValue(), ScriptAndToolOperate.VALIDATE.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SAVE, (userUuid, id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能保存
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.SAVE.getValue(), ScriptAndToolOperate.SAVE.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.SUBMIT, (userUuid, id) -> {
            // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能提交审核
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), version.getStatus())
                        || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), version.getStatus()))) {
                    return new OperateVo(ScriptAndToolOperate.SUBMIT.getValue(), ScriptAndToolOperate.SUBMIT.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.PASS, (userUuid, id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能通过
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    return new OperateVo(ScriptAndToolOperate.PASS.getValue(), ScriptAndToolOperate.PASS.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.REJECT, (userUuid, id) -> {
            // 拥有脚本审核权限，且处于待审核状态才能驳回
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(id);
                if (version != null && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                    return new OperateVo(ScriptAndToolOperate.REJECT.getValue(), ScriptAndToolOperate.REJECT.getText());
                }
            }
            return null;
        });

        operateMap.put(ScriptAndToolOperate.GENERATETOCOMBOP, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_COMBOP_MODIFY.class.getSimpleName())) {
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

        operateMap.put(ScriptAndToolOperate.EXPORT, (userUuid, id) -> {
            if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName())) {
                return new OperateVo(ScriptAndToolOperate.EXPORT.getValue(), ScriptAndToolOperate.EXPORT.getText());
            }
            return null;
        });

    }

    public Map<ScriptAndToolOperate, BiFunction<String, Long, OperateVo>> getOperateMap() {
        return operateMap;
    }

    public class Builder {
        List<OperateVo> operateList; // 操作列表

        String userUuid; // 当前用户uuid

        public Builder() {
            operateList = new ArrayList<>();
            this.userUuid = UserContext.get().getUserUuid();
        }

        public List<OperateVo> build() {
            return operateList;
        }

        public Builder setDelete(Long scriptId) {
            if (scriptId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.DELETE).apply(userUuid, scriptId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setVersionDelete(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.VERSIONDELETE).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setCopy() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.COPY).apply(userUuid, null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setTest() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.TEST).apply(userUuid, null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setCompare() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.COMPARE).apply(userUuid, null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

        public Builder setValidate(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.VALIDATE).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setSave(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.SAVE).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setSubmit(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.SUBMIT).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setPass(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.PASS).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setReject(Long versionId) {
            if (versionId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.REJECT).apply(userUuid, versionId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setGenerateToCombop(Long scriptId) {
            if (scriptId != null) {
                OperateVo vo = getOperateMap().get(ScriptAndToolOperate.GENERATETOCOMBOP).apply(userUuid, scriptId);
                if (vo != null) {
                    operateList.add(vo);
                }
            }
            return this;
        }

        public Builder setExport() {
            OperateVo vo = getOperateMap().get(ScriptAndToolOperate.EXPORT).apply(userUuid, null);
            if (vo != null) {
                operateList.add(vo);
            }
            return this;
        }

    }

}
