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
        return "更新组合工具阶段操作的描述字段值";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Description(desc = "更新组合工具阶段操作的描述字段值")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resultList = new JSONArray();
        AutoexecCombopVo searchVo = new AutoexecCombopVo();
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<AutoexecCombopVo> autoexecCombopList = autoexecCombopMapper.getAutoexecCombopList(searchVo);
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
