/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 查询组合工具顶层参数列表接口
 *
 * @author: linbq
 * @since: 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口唯一标识，也是访问URI
     */
    @Override
    public String getToken() {
        return "autoexec/combop/param/list";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "查询组合工具顶层参数列表";
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
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
    })
    @Output({
            @Param(explode = AutoexecCombopParamVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询组合工具顶层参数列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        for (AutoexecCombopParamVo autoexecCombopParamVo : autoexecCombopParamVoList) {
            ParamType paramType = ParamType.getParamType(autoexecCombopParamVo.getType());
            if (paramType != null) {
                JSONObject config = new JSONObject(paramType.getConfig());
                if (Objects.equals(autoexecCombopParamVo.getIsRequired(), 0)) {
                    config.put("isRequired", false);
                } else {
                    config.put("isRequired", true);
                }
                autoexecCombopParamVo.setConfig(config);
                Object value = autoexecCombopParamVo.getValue();
                if (value != null) {
                    switch (paramType) {
                        case TEXT:
                            break;
                        case PASSWORD:
                            config.put("showPassword", false);
                            break;
                        case FILE:
                            autoexecCombopParamVo.setValue(JSONObject.parseObject(value.toString()));
                            break;
                        case DATE:
                            break;
                        case JSON:
                            autoexecCombopParamVo.setValue(JSONObject.parseObject(value.toString()));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return autoexecCombopParamVoList;
    }

}
