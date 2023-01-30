/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
