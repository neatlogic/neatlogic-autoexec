/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.source.AutoexecJobSourceFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author lvzk
 * @since 2021/4/21 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobSourceListApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取作业来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({

    })
    @Output({
    })
    @Description(desc = "获取作业来源")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray sourceArray = new JSONArray();
        Map<String, String> sourceMap = AutoexecJobSourceFactory.getSourceValueMap();
        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            sourceArray.add(new JSONObject() {
                {
                    put("value", entry.getKey());
                    put("text", entry.getValue());
                }
            });
        }
        return sourceArray;
    }

    @Override
    public String getToken() {
        return "autoexec/job/source/list";
    }
}
