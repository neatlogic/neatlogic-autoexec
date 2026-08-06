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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationBaseVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//@Service
@Deprecated
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAutoexecCombopPhaseOperationDescriptionFieldApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/combop/phase/operation/description/update";
    }

    @Override
    public String getName() {
        return "nmaa.updateautoexeccombopphaseoperationdescriptionfieldapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Description(desc = "nmaa.updateautoexeccombopphaseoperationdescriptionfieldapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resultList = new JSONArray();
        AutoexecCombopSearchVo searchVo = new AutoexecCombopSearchVo();
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<AutoexecCombopVo> autoexecCombopList = new ArrayList<>();
                List<Long> idList = autoexecCombopMapper.getAutoexecCombopIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(idList);
                    autoexecCombopList.sort(Comparator.comparingInt(o -> idList.indexOf(o.getId())));
                }
                for (AutoexecCombopVo autoexecCombopVo : autoexecCombopList) {
                    autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVo.getId());
                    AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
                    if (config != null) {
                        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
                        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                            boolean flag = false;
                            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                                AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
                                if (phaseConfig != null) {
                                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                                        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                                            if (StringUtils.isNotBlank(phaseOperationVo.getDescription())) {
                                                continue;
                                            }
                                            AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecService.getAutoexecOperationBaseVoByIdAndType(combopPhaseVo.getName(), phaseOperationVo, false);
                                            if (autoexecOperationBaseVo == null) {
                                                continue;
                                            }
                                            String description = autoexecOperationBaseVo.getDescription();
                                            if (StringUtils.isBlank(description)) {
                                                continue;
                                            }
                                            phaseOperationVo.setDescription(description);
                                            phaseOperationVo.setOperation(null);
                                            flag = true;
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                autoexecCombopVo.setConfig(config);
                                autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
                                JSONObject obj = new JSONObject();
                                obj.put("id", autoexecCombopVo.getId());
                                obj.put("name", autoexecCombopVo.getName());
                                resultList.add(obj);
                            }
                        }
                    }
                }
            }
        }
        return resultList;
    }
}
