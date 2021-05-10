/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.operate;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.common.dto.ValueTextVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScriptOperateBuilder {

    List<ValueTextVo> operateList; // 操作列表

    String userUuid; // 当前用户uuid

    String status; // 脚本版本状态

    public ScriptOperateBuilder(String userUuid, String status) {
        operateList = new ArrayList<>();
        this.userUuid = userUuid;
        this.status = status;
    }

    public ScriptOperateBuilder(String userUuid) {
        operateList = new ArrayList<>();
        this.userUuid = userUuid;
    }

    public List<ValueTextVo> build() {
        return operateList;
    }

    public ScriptOperateBuilder setDelete() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())) {
            operateList.add(new ValueTextVo("delete", "删除"));
        }
        return this;
    }

    public ScriptOperateBuilder setCopy() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("copy", "复制"));
        }
        return this;
    }

    public ScriptOperateBuilder setTest() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("test", "测试"));
        }
        return this;
    }

    public ScriptOperateBuilder setCompare() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_USE.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("compare", "版本对比"));
        }
        return this;
    }

    public ScriptOperateBuilder setValidate() {
        // 拥有脚本审核或维护权限，且处于编辑中、已驳回、待审核状态才能校验
        if ((AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName()))
                && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)
                || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status))
                || Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
            operateList.add(new ValueTextVo("validate", "校验"));
        }
        return this;
    }

    public ScriptOperateBuilder setSave() {
        // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能保存
        if ((AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName()))
                && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)
                || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status))) {
            operateList.add(new ValueTextVo("save", "保存"));
        }
        return this;
    }

    public ScriptOperateBuilder setSubmit() {
        // 拥有脚本审核或维护权限，且处于编辑中、已驳回状态才能提交审核
        if ((AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName()))
                && (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)
                || Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status))) {
            operateList.add(new ValueTextVo("submit", "提交审核"));
        }
        return this;
    }

    public ScriptOperateBuilder setPass() {
        // 拥有脚本审核权限，且处于待审核状态才能通过
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
            operateList.add(new ValueTextVo("pass", "通过"));
        }
        return this;
    }

    public ScriptOperateBuilder setReject() {
        // 拥有脚本审核权限，且处于待审核状态才能驳回
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                && Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
            operateList.add(new ValueTextVo("reject", "驳回"));
        }
        return this;
    }

    public ScriptOperateBuilder setGenerateToCombop() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_COMBOP_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("generateToCombop", "发布为组合工具"));
        }
        return this;
    }

    public ScriptOperateBuilder setExport() {
        if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_USE.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_REVIEW.class.getSimpleName())
                || AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            operateList.add(new ValueTextVo("export", "导出"));
        }
        return this;
    }

    public ScriptOperateBuilder setAll() {
        setDelete().setCopy().setCompare().setTest().setValidate().setSave().setSubmit().setPass().setReject();
        return this;
    }


}
