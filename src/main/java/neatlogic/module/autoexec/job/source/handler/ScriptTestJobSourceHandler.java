/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.job.source.handler;

import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.dto.ValueTextVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScriptTestJobSourceHandler implements IAutoexecJobSource {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getValue() {
        return JobSource.SCRIPT_TEST.getValue();
    }

    @Override
    public String getText() {
        return JobSource.SCRIPT_TEST.getText();
    }

    @Override
    public List<ValueTextVo> getListByIdList(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return null;
        }
        List<ValueTextVo> resultList = new ArrayList<>();
        List<AutoexecScriptVersionVo> list = autoexecScriptMapper.getVersionByVersionIdList(idList);
        for (AutoexecScriptVersionVo versionVo : list) {
            resultList.add(new ValueTextVo(versionVo.getId(), versionVo.getTitle()));
        }
        return resultList;
    }
}
