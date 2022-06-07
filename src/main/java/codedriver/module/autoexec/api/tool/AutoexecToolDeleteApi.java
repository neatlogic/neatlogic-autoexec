/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecToolDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/delete";
    }

    @Override
    public String getName() {
        return "删除内置工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nameList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "工具名称列表"),
    })
    @Output({
    })
    @Description(desc = "删除内置工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> nameList = jsonObj.getJSONArray("nameList").toJavaList(String.class);
        List<AutoexecToolVo> toolList = autoexecToolMapper.getToolByNameList(nameList);
        List<Long> canDeleteToolIdList = new ArrayList<>();
        Map<String, String> canNotDeleteTool = new HashMap<>();
        for (AutoexecToolVo toolVo : toolList) {
            List<AutoexecCombopVo> referenceList = autoexecToolMapper.getReferenceListByToolId(toolVo.getId());
            if (referenceList.size() > 0) {
                canNotDeleteTool.put(toolVo.getName(), referenceList.stream().map(AutoexecCombopVo::getName).collect(Collectors.joining("、")));
            } else {
                canDeleteToolIdList.add(toolVo.getId());
            }
        }
        if (canDeleteToolIdList.size() > 0) {
            autoexecToolMapper.deleteToolByIdList(canDeleteToolIdList);
        }
        if (!canNotDeleteTool.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("以下工具已被组合工具引用，无法删除\n");
            for (Map.Entry<String, String> entry : canNotDeleteTool.entrySet()) {
                sb.append(entry.getKey()).append("(关联的组合工具：").append(entry.getValue()).append(")\n");
            }
            return sb.toString();
        }
        return null;
    }

}
