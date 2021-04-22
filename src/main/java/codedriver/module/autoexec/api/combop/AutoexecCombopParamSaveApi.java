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
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
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
 * 保存组合工具顶层参数接口
 *
 * @author: linbq
 * @since: 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopParamSaveApi extends PrivateApiComponentBase {

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
        return "autoexec/combop/param/save";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "保存组合工具顶层参数";
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
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "参数列表[{\"key\": \"参数名\", \"value\": \"参数值\", \"description\": \"描述\", \"isRequired\": \"是否必填\", \"type\": \"参数类型\"}]")
    })
    @Description(desc = "保存组合工具顶层参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        List<AutoexecCombopParamVo> autoexecCombopParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(combopId);
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
        JSONArray paramList = jsonObj.getJSONArray("paramList");
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecCombopParamVo autoexecCombopParamVo = paramList.getObject(i, AutoexecCombopParamVo.class);
            if (autoexecCombopParamVo != null) {
                Object value = autoexecCombopParamVo.getDefaultValue();
                if (value != null && Objects.equals(autoexecCombopParamVo.getType(), ParamType.PASSWORD.getValue())) {
                    Integer sort = autoexecCombopParamVo.getSort();
                    if (sort != null && sort < autoexecCombopParamList.size()) {
                        AutoexecCombopParamVo oldParamVo = autoexecCombopParamList.get(sort);
                        if (!Objects.equals(value, oldParamVo.getDefaultValue())) {
                            autoexecCombopParamVo.setDefaultValue(RC4Util.encrypt((String) value));
                        }
                    } else {
                        autoexecCombopParamVo.setDefaultValue(RC4Util.encrypt((String) value));
                    }
                }
                autoexecCombopParamVo.setCombopId(combopId);
                autoexecCombopParamVo.setSort(i);
                autoexecCombopParamVoList.add(autoexecCombopParamVo);
                if (autoexecCombopParamVoList.size() == 1000) {
                    autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
                    autoexecCombopParamVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
            autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
        }
        return null;
    }

}
