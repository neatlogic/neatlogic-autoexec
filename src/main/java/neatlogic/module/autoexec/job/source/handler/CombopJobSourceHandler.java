/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CombopJobSourceHandler implements IAutoexecJobSource {

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String getValue() {
        return JobSource.COMBOP.getValue();
    }

    @Override
    public String getText() {
        return JobSource.COMBOP.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            return null;
        }
        List<Long> idList = new ArrayList<>();
        for (String str : uniqueKeyList) {
            idList.add(Long.valueOf(str));
        }
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        List<AutoexecCombopVersionVo> list = autoexecCombopVersionMapper.getAutoexecCombopVersionListByIdList(idList);
        for (AutoexecCombopVersionVo versionVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", versionVo.getCombopId());
            config.put("versionId", versionVo.getId());
            resultList.add(new AutoexecJobRouteVo(versionVo.getId(), versionVo.getName(), config));
        }
        return resultList;
    }
}
