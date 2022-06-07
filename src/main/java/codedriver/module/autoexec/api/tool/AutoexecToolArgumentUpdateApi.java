/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecToolArgumentUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/argument/update";
    }

    @Override
    public String getName() {
        return "工具argument结构转换";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
    })
    @Description(desc = "工具argument结构转换")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AutoexecToolVo> allTool = autoexecToolMapper.getAllTool();
        for (AutoexecToolVo toolVo : allTool) {
            JSONObject config = toolVo.getConfig();
            if (config != null) {
                JSONObject argument = config.getJSONObject("argument");
                if (argument != null) {
                    config.put("argument", new AutoexecParamVo(argument));
                    toolVo.setConfigStr(config.toJSONString());
                    autoexecToolMapper.updateConfig(toolVo);
                }
            }
        }
        return null;
    }


}
