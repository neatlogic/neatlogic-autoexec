/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
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
