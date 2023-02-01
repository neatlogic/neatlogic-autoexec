/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
            AutoexecCombopConfigVo combopConfig = autoexecCombopVo.getConfig();
            if (combopConfig == null) {
                return new ArrayList<>();
            }
            List<AutoexecCombopPhaseVo> combopPhaseList = combopConfig.getCombopPhaseList();
            if (CollectionUtils.isEmpty(combopPhaseList)) {
                return new ArrayList<>();
            }
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                AutoexecCombopPhaseConfigVo phaseConfigVo = combopPhaseVo.getConfig();
                if (phaseConfigVo == null) {
                    continue;
                }
                List<AutoexecCombopPhaseOperationVo> combopPhaseOperationList = phaseConfigVo.getPhaseOperationList();
                if (CollectionUtils.isEmpty(combopPhaseOperationList)) {
                    continue;
                }
                for (AutoexecCombopPhaseOperationVo combopPhaseOperationVo : combopPhaseOperationList) {
                    if (combopPhaseOperationVo == null) {
                        continue;
                    }
                    AutoexecCombopPhaseOperationConfigVo operationConfigVo = combopPhaseOperationVo.getConfig();
                    if (operationConfigVo == null) {
                        continue;
                    }
                    Long profileId = operationConfigVo.getProfileId();
                    if (profileId != null) {
                        profileIdSet.add(profileId);
                    }
                    List<AutoexecCombopPhaseOperationVo> ifList = operationConfigVo.getIfList();
                    if (CollectionUtils.isNotEmpty(ifList)) {
                        for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                            if (operationVo == null) {
                                continue;
                            }
                            AutoexecCombopPhaseOperationConfigVo operationConfig = operationVo.getConfig();
                            if (operationConfig == null) {
                                continue;
                            }
                            if (operationConfig.getProfileId() != null) {
                                profileIdSet.add(operationConfig.getProfileId());
                            }
                        }
                    }
                    List<AutoexecCombopPhaseOperationVo> elseList = operationConfigVo.getElseList();
                    if (CollectionUtils.isNotEmpty(elseList)) {
                        for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                            if (operationVo == null) {
                                continue;
                            }
                            AutoexecCombopPhaseOperationConfigVo operationConfig = operationVo.getConfig();
                            if (operationConfig == null) {
                                continue;
                            }
                            if (operationConfig.getProfileId() != null) {
                                profileIdSet.add(operationConfig.getProfileId());
                            }
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isEmpty(profileIdSet)) {
            return new ArrayList<>();
        }
        List<AutoexecProfileVo> profileList = autoexecProfileService.getProfileVoListByIdList(new ArrayList<>(profileIdSet));
        return profileList;
    }
}
