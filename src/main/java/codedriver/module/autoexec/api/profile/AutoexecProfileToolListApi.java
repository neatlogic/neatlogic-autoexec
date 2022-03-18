package codedriver.module.autoexec.api.profile;

import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/3/18 10:08 上午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecProfileToolListApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/tool/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecProfileVo[].class, desc = "工具profile列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "工具库工具、自定义工具下拉列表list接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        return null;
    }
}
