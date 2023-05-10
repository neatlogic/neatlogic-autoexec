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

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class BatchDeleteAutoexecScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/batchdelete";
    }

    @Override
    public String getName() {
        return "批量删除脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "脚本ID列表", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "批量删除脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> failedList = new ArrayList<>();
        List<Long> idList = jsonObj.getJSONArray("idList").toJavaList(Long.class);
        for (Long id : idList) {
            TransactionStatus tx = TransactionUtil.openTx();
            AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptLockById(id);
            try {
                autoexecScriptService.deleteScriptById(id);
                TransactionUtil.commitTx(tx);
            } catch (Exception ex) {
                TransactionUtil.rollbackTx(tx);
                if (scriptVo != null) {
                    failedList.add(scriptVo.getName());
                }
            }
        }
        return failedList;
    }


}
