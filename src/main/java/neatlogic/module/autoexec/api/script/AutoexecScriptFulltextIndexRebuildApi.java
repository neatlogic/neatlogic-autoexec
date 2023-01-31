/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
@AuthAction(action = AUTOEXEC_BASE.class)
public class AutoexecScriptFulltextIndexRebuildApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "/autoexec/script/version/fulltext/index/rebuild";
    }

    @Override
    public String getName() {
        return "重建自定义工具版本索引";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "versionIdList", type = ApiParamType.JSONARRAY, desc = "版本号idList")})
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray versionIdArray = jsonObj.getJSONArray("versionIdList");
        List<Long> versionIdList;
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
        if (handler != null) {
            if (CollectionUtils.isNotEmpty(versionIdArray)) {
                versionIdList = versionIdArray.toJavaList(Long.class);
            } else {
                versionIdList = autoexecScriptMapper.getVersionIdList();
            }
            for (Long versionIdObj : versionIdList) {
                handler.createIndex(versionIdObj);
            }
        }
        return null;
    }

}
