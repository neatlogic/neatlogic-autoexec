/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.exception.NotifyPolicyNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 更新组合工具配置信息接口
 *
 * @author: linbq
 * @since: 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private NotifyMapper notifyMapper;

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口唯一标识，也是访问URI
     */
    @Override
    public String getToken() {
        return "autoexec/combop/update";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "更新组合工具配置信息";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 额外配置
     */
    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "uk", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "唯一名"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "notifyPolicyId", type = ApiParamType.LONG, desc = "通知策略id"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Description(desc = "更新组合工具配置信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVo autoexecCombopVo = JSON.toJavaObject(jsonObj, AutoexecCombopVo.class);
        JSONObject config = autoexecCombopVo.getConfig();
        /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
        autoexecCombopService.verifyAutoexecCombopConfig(config);

        Long combopId = autoexecCombopVo.getId();
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            new AutoexecCombopNameRepeatException(autoexecCombopVo.getName());
        }
        if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) != null) {
            new AutoexecCombopUkRepeatException(autoexecCombopVo.getUk());
        }
        if (autoexecCombopVo.getNotifyPolicyId() != null) {
            if (notifyMapper.checkNotifyPolicyIsExists(autoexecCombopVo.getNotifyPolicyId()) == 0) {
                throw new NotifyPolicyNotFoundException(autoexecCombopVo.getNotifyPolicyId().toString());
            }
        }
        List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(combopId);

        if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
            autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
        }
        autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(combopId);

        JSONArray combopPhaseList = config.getJSONArray("combopPhaseList");
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.getObject(i, AutoexecCombopPhaseVo.class);
            if (autoexecCombopPhaseVo != null) {
                autoexecCombopPhaseVo.setCombopId(combopId);
                autoexecCombopPhaseVo.setSort(i);
                JSONObject phaseConfig = autoexecCombopPhaseVo.getConfig();
                JSONArray phaseOperationList = phaseConfig.getJSONArray("phaseOperationList");
                Long combopPhaseId = autoexecCombopPhaseVo.getId();
                for (int j = 0; j < phaseOperationList.size(); j++) {
                    AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo = phaseOperationList.getObject(j, AutoexecCombopPhaseOperationVo.class);
                    if (autoexecCombopPhaseOperationVo != null) {
                        autoexecCombopPhaseOperationVo.setSort(i);
                        autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
                        autoexecCombopMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
                    }
                }
                autoexecCombopMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
                JSONArray phaseNodeList = phaseConfig.getJSONArray("phaseNodeList");
                if (CollectionUtils.isNotEmpty(phaseNodeList)) {
                    //TODO linbq
                }
            }
        }
        JSONArray combopNodeList = config.getJSONArray("combopNodeList");
        if (CollectionUtils.isNotEmpty(combopNodeList)) {
            //TODO linbq
        }

        autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
        return null;
    }
}
