/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dependency.constvalue.CalleeType;
import codedriver.framework.dependency.core.DependencyHandlerBase;
import codedriver.framework.dependency.core.ICalleeType;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author linbq
 * @since 2021/6/21 16:31
 **/
@Service
public class MatrixAutoexecCombopParamDependencyHandler extends DependencyHandlerBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    protected String getTableName() {
        return "autoexec_combop_param_matrix";
    }

    @Override
    protected String getCalleeField() {
        return "matrix_uuid";
    }

    @Override
    protected String getCallerField() {
        return "combop_id";
    }

    @Override
    protected List<String> getCallerFieldList() {
        List<String> result = new ArrayList<>();
        result.add("combop_id");
        result.add("key");
        return result;
    }

    @Override
    protected ValueTextVo parse(Object caller) {
        if (caller == null) {
            return null;
        }
        if(caller instanceof Map){
            Map<String, Object> map = (Map)caller;
            ValueTextVo valueTextVo = new ValueTextVo();
            Long combopId = (Long) map.get("combop_id");
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (autoexecCombopVo != null) {
                String key = (String) map.get("key");
                AutoexecCombopParamVo autoexecCombopParamVo = autoexecCombopMapper.getAutoexecCombopParamByCombopIdAndKey(autoexecCombopVo.getId(), key);
                if (autoexecCombopParamVo != null) {
                    valueTextVo.setValue(autoexecCombopVo.getId());
                    String text = String.format("<a href=\"/%s/autoexec.html#/action-detail-%s\" target=\"_blank\">%s</a>",
                            TenantContext.get().getTenantUuid(),
                            autoexecCombopVo.getId(),
                            autoexecCombopVo.getName() + "-运行参数-" + autoexecCombopParamVo.getName());
                    valueTextVo.setText(text);
                    return valueTextVo;
                }
            }
        }
        return null;
    }

    @Override
    public ICalleeType getCalleeType() {
        return CalleeType.MATRIX;
    }
}
