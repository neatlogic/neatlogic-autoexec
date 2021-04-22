/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ChangeType;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionCompareApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/version/compare";
    }

    @Override
    public String getName() {
        return "脚本版本对比";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "当前版本ID"),
            @Param(name = "targetVersionId", type = ApiParamType.LONG, isRequired = true, desc = "目标版本ID"),
    })
    @Output({
            @Param(name = "currentVersion", explode = AutoexecScriptVo[].class, desc = "当前版本脚本"),
            @Param(name = "targetVersion", explode = AutoexecScriptVo[].class, desc = "目标版本脚本"),
    })
    @Description(desc = "脚本版本对比")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // todo 参数和解析器单独对比，内容用LCS对比
        JSONObject result = new JSONObject();
        Long currentVersionId = jsonObj.getLong("currentVersionId");
        Long targetVersionId = jsonObj.getLong("targetVersionId");
        AutoexecScriptVersionVo currentVersion = autoexecScriptService.getScriptVersionDetailByVersionId(currentVersionId);
        AutoexecScriptVersionVo targetVersion = autoexecScriptService.getScriptVersionDetailByVersionId(targetVersionId);
        result.put("currentVersion", currentVersion);
        result.put("targetVersion", targetVersion);
        if (!Objects.equals(currentVersionId, targetVersionId)) {
            compareScriptVersion(currentVersion, targetVersion);
        }
        return null;
    }

    private void compareScriptVersion(AutoexecScriptVersionVo currentVersion, AutoexecScriptVersionVo targetVersion) {
        List<AutoexecScriptVersionParamVo> currentInputParamList = new ArrayList<>();
        List<AutoexecScriptVersionParamVo> targetInputParamList = new ArrayList<>();
        List<AutoexecScriptVersionParamVo> currentOutputParamList = new ArrayList<>();
        List<AutoexecScriptVersionParamVo> targetOutputParamList = new ArrayList<>();
        currentInputParamList.addAll(currentVersion.getInputParamList());
        currentOutputParamList.addAll((currentVersion.getOutputParamList()));
        targetInputParamList.addAll(targetVersion.getInputParamList());
        targetOutputParamList.addAll((targetVersion.getOutputParamList()));
        // todo 参数对比，是否需要精确到字段域，比如判断究竟是参数名有变化，还是类型有变化
        /**
         * 1、两边都为空
         * 2、一边为空，标记另一边参数全部为insert
         * 3、两边不为空，数量不等，相等部分对比，不等部分标记为delete或insert
         * 4、两边不为空，数量相等
         */
        if (currentInputParamList.size() != targetInputParamList.size()) {
            if (CollectionUtils.isEmpty(currentInputParamList)) {
                targetInputParamList.stream().forEach(o -> o.setChangeType(ChangeType.INSERT.getValue()));
            } else if (CollectionUtils.isEmpty(targetInputParamList)) {
                currentInputParamList.stream().forEach(o -> o.setChangeType(ChangeType.INSERT.getValue()));
            } else {
                if (currentInputParamList.size() > targetInputParamList.size()) {
                    // 对比数量相等部分的参数
                    compareInputParamList(currentInputParamList, targetInputParamList);
                    // 将多余部分的参数标记为delete
                    // todo 注意测试边界
                    for (int i = targetInputParamList.size() + 1; i < currentInputParamList.size(); i++) {
                        currentInputParamList.get(i).setChangeType(ChangeType.DELETE.getValue());
                    }


                } else if (currentInputParamList.size() < targetInputParamList.size()) {
                    for (int i = 0; i < currentInputParamList.size(); i++) {
                        boolean hasChange = false;
                        AutoexecScriptVersionParamVo beforeNextParam = currentInputParamList.get(i);
                        AutoexecScriptVersionParamVo afterNextParam = targetInputParamList.get(i);
                        if ((!Objects.equals(beforeNextParam.getKey(), afterNextParam.getKey()))
                                || (!Objects.equals(beforeNextParam.getDefaultValue(), afterNextParam.getDefaultValue()))
                                || (!Objects.equals(beforeNextParam.getType(), afterNextParam.getType()))
                                || (!Objects.equals(beforeNextParam.getMode(), afterNextParam.getMode()))
                                || (!Objects.equals(beforeNextParam.getIsRequired(), afterNextParam.getIsRequired()))
                                || (!Objects.equals(beforeNextParam.getDescription(), afterNextParam.getDescription()))
                        ) {
                            hasChange = true;
                        }
                        if (hasChange) {
                            beforeNextParam.setChangeType(ChangeType.UPDATE.getValue());
                            afterNextParam.setChangeType(ChangeType.UPDATE.getValue());
                        }
                    }
                    // 将多余部分的参数标记为insert
                    for (int i = currentInputParamList.size() + 1; i < targetInputParamList.size(); i++) {
                        targetInputParamList.get(i).setChangeType(ChangeType.INSERT.getValue());
                    }
                }
            }

        }


    }

    private void compareInputParamList(List<AutoexecScriptVersionParamVo> currentInputParamList, List<AutoexecScriptVersionParamVo> targetInputParamList) {
        int count = targetInputParamList.size();
        for (int i = 0; i < count; i++) {
            boolean hasChange = false;
            AutoexecScriptVersionParamVo beforeNextParam = currentInputParamList.get(i);
            AutoexecScriptVersionParamVo afterNextParam = targetInputParamList.get(i);
            if ((!Objects.equals(beforeNextParam.getKey(), afterNextParam.getKey()))
                    || (!Objects.equals(beforeNextParam.getDefaultValue(), afterNextParam.getDefaultValue()))
                    || (!Objects.equals(beforeNextParam.getType(), afterNextParam.getType()))
                    || (!Objects.equals(beforeNextParam.getMode(), afterNextParam.getMode()))
                    || (!Objects.equals(beforeNextParam.getIsRequired(), afterNextParam.getIsRequired()))
                    || (!Objects.equals(beforeNextParam.getDescription(), afterNextParam.getDescription()))
            ) {
                hasChange = true;
            }
            if (hasChange) {
                beforeNextParam.setChangeType(ChangeType.UPDATE.getValue());
                afterNextParam.setChangeType(ChangeType.UPDATE.getValue());
            }
        }
    }


}
