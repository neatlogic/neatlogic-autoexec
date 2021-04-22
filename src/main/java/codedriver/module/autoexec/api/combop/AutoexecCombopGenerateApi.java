/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 脚本/工具发布生成组合工具接口
 *
 * @author: linbq
 * @since: 2021/4/21 15:20
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecCombopGenerateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口唯一标识，也是访问URI
     */
    @Override
    public String getToken() {
        return "autoexec/combop/generate";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "脚本/工具发布生成组合工具";
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
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "脚本/工具主键id"),
            @Param(name = "operationType", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "脚本/工具")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "脚本/工具发布生成组合工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
//        Long operationId = jsonObj.getLong("operationId");
//        String operationType = jsonObj.getString("operationType");
//        if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
//            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(operationId);
//            if (autoexecScriptVo == null) {
//                throw new AutoexecScriptNotFoundException(operationId);
//            }
//            AutoexecCombopVo autoexecCombopVo = new AutoexecCombopVo(autoexecScriptVo);
//            Long combopId = autoexecCombopVo.getId();
//            if (autoexecCombopMapper.checkAutoexecCombopUkIsRepeat(autoexecCombopVo) > 0) {
//                autoexecCombopVo.setUk(autoexecCombopVo.getUk() + "_" + combopId);
//            }
//            if (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) > 0) {
//                autoexecCombopVo.setName(autoexecCombopVo.getName() + "_" + combopId);
//            }
//            JSONObject config = new JSONObject();
//            JSONArray combopPhaseList = new JSONArray();
//            JSONObject combopPhaseObj = new JSONObject();
//            JSONObject combopPhaseConfig = new JSONObject();
//            combopPhaseObj.put("uk", autoexecScriptVo.getUk());
//            combopPhaseObj.put("name", autoexecScriptVo.getName());
//            combopPhaseObj.put("execMode", autoexecScriptVo.getExecMode());
//            combopPhaseObj.put("config", combopPhaseConfig);
//            combopPhaseList.add(combopPhaseObj);
//            List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
//            if (CollectionUtils.isNotEmpty(autoexecScriptVersionParamVoList)) {
//                List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
//                int sort = 0;
//                for (AutoexecScriptVersionParamVo autoexecScriptVersionParamVo : autoexecScriptVersionParamVoList) {
//                    if (Objects.equals(autoexecScriptVersionParamVo.getMode(), "input")) {
//                        AutoexecCombopParamVo autoexecCombopParamVo = new AutoexecCombopParamVo(autoexecScriptVersionParamVo);
//                        autoexecCombopParamVo.setCombopId(combopId);
//                        autoexecCombopParamVo.setSort(sort++);
//                        autoexecCombopParamVoList.add(autoexecCombopParamVo);
//                    }
//                }
//                autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
//            }
//            autoexecCombopVo.setConfig(config.toJSONString());
//            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
//        } else {
//
//        }
        return null;
    }
}
