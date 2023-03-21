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

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceConfigVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import neatlogic.framework.util.I18nUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AutoexecServiceServiceImpl implements AutoexecServiceService {

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String checkConfigExpired(AutoexecServiceVo serviceVo) {
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(serviceVo.getCombopId());
        if (autoexecCombopVo == null) {
            return I18nUtils.getMessage("exception.autoexec.autoexeccombopnotfoundexception", serviceVo.getCombopId());
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionByCombopId(serviceVo.getCombopId());
        if (versionVo == null) {
            return I18nUtils.getMessage("exception.autoexec.autoexeccombopactiveversionnotfoundexception", autoexecCombopVo.getName());
        }
        AutoexecServiceConfigVo serviceConfigVo = serviceVo.getConfig();
        List<ParamMappingVo> runtimeParamMappingList = serviceConfigVo.getRuntimeParamList();
        AutoexecCombopVersionConfigVo versionConfigVo = versionVo.getConfig();
        List<AutoexecParamVo> runtimeParamList = versionConfigVo.getRuntimeParamList();
        List<String> list = new ArrayList<>();
        Map<String, AutoexecParamVo> runtimeParamMap = runtimeParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
        for (ParamMappingVo runtimeParamMapping : runtimeParamMappingList) {
            String key = runtimeParamMapping.getKey();
            AutoexecParamVo runtimeParamVo = runtimeParamMap.remove(key);
            if (runtimeParamVo == null) {
                list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“不存在");
                continue;
            }
            if (!Objects.equals(runtimeParamMapping.getType(), runtimeParamVo.getType())) {
                list.add("作业参数“" + runtimeParamMapping.getName() + "(" + key + ")“类型发生变化，由“" + runtimeParamMapping.getType() + "”变成“" + runtimeParamVo.getType() + "”");
            }
        }
        if (MapUtils.isNotEmpty(runtimeParamMap)) {
            for (Map.Entry<String, AutoexecParamVo> entry : runtimeParamMap.entrySet()) {
                AutoexecParamVo runtimeParamVo = entry.getValue();
                list.add("作业参数“" + runtimeParamVo.getName() + "(" + runtimeParamVo.getKey() + ")“未映射");
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            return String.join("；", list);
        }
        return null;
    }
}
