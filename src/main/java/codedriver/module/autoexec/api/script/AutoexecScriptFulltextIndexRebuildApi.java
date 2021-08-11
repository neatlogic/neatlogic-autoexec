package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
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
        IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getComponent(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
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
