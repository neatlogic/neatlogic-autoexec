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

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.List;

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
        List<IAutoexecJobSource> list = AutoexecJobSourceFactory.getEnumInstanceList();
        for (IAutoexecJobSource jobSource : list) {
            sourceArray.add(new JSONObject() {
                {
                    put("value", jobSource.getValue());
                    put("text", jobSource.getText());
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
