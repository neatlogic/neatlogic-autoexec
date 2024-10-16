/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 查询组合工具预置参数集列表
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopProfileListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecProfileService autoexecProfileService;

    @Override
    public String getToken() {
        return "autoexec/combop/profile/list";
    }

    @Override
    public String getName() {
        return "查询组合工具预置参数集列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "主键id"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值")
    })
    @Output({
            @Param(explode = AutoexecProfileVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询组合工具预置参数集列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Set<Long> profileIdSet = new HashSet<>();
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> profileIdList = defaultValue.toJavaList(Long.class);
            profileIdSet.addAll(profileIdList);
        } else {
            Long combopId = jsonObj.getLong("combopId");
            if (combopId == null) {
                throw new ParamNotExistsException("combopId", "defaultValue");
            }
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (autoexecCombopVo == null) {
                throw new AutoexecCombopNotFoundException(combopId);
            }
            Long versionId = jsonObj.getLong("versionId");
            if (versionId == null) {
                versionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopId);
            }
            if (versionId == null) {
                return new ArrayList<>();
            }
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(versionId);
            }
            AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
            if (config == null) {
                return new ArrayList<>();
            }
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            if (CollectionUtils.isEmpty(combopPhaseList)) {
                return new ArrayList<>();
            }
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo == null) {
                    continue;
                }
                getProfileIdSet(phaseConfigVo.getPhaseOperationList(), profileIdSet);
            }
        }

        if (CollectionUtils.isEmpty(profileIdSet)) {
            return new ArrayList<>();
        }
        return autoexecProfileService.getProfileVoListByIdList(new ArrayList<>(profileIdSet));
    }

    /**
     * 获取预置参数集id set
     *
     * @param operationVos 工具列表
     * @param profileIdSet 预置参数及id列表
     */
    private void getProfileIdSet(List<AutoexecCombopPhaseOperationVo> operationVos, Set<Long> profileIdSet) {
        if (CollectionUtils.isNotEmpty(operationVos)) {
            for (AutoexecCombopPhaseOperationVo operationVo : operationVos) {
                if (operationVo == null || operationVo.getConfig() == null) {
                    continue;
                }
                AutoexecCombopPhaseOperationConfigVo operationConfigVo = operationVo.getConfig();
                Long profileId = operationConfigVo.getProfileId();
                if (profileId != null) {
                    profileIdSet.add(profileId);
                }
                getProfileIdSet(operationConfigVo.getIfList(), profileIdSet);
                getProfileIdSet(operationConfigVo.getElseList(), profileIdSet);
                getProfileIdSet(operationConfigVo.getOperations(), profileIdSet);
            }
        }
    }
}
