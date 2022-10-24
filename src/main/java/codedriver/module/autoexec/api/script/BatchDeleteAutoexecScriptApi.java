/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.transaction.util.TransactionUtil;
import codedriver.module.autoexec.service.AutoexecScriptService;
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
