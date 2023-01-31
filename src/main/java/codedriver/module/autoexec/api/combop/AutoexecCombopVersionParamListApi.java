/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import codedriver.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 查询组合工具顶层参数列表接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopVersionParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/version/param/list";
    }

    @Override
    public String getName() {
        return "查询组合工具版本作业参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopVersionId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具版本ID")
    })
    @Output({
            @Param(explode = AutoexecParamVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询组合工具版本作业参数列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopVersionId = jsonObj.getLong("combopVersionId");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(combopVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(combopVersionId);
        }
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        List<AutoexecParamVo> autoexecParamVoList = config.getRuntimeParamList();
//        for (AutoexecParamVo autoexecParamVo : autoexecParamVoList) {
//            autoexecService.mergeConfig(autoexecParamVo);
//        }
        return autoexecParamVoList;
    }
}
