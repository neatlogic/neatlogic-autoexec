/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
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
