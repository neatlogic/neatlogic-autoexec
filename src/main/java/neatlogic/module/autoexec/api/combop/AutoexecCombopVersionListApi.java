package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopVersionListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/version/list";
    }

    @Override
    public String getName() {
        return "查询组合工具版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具id"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "notPassed,passed", isRequired = true, desc = "是否已经审核通过"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = AutoexecCombopVersionVo[].class, desc = "组合工具版本列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询组合工具版本列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecCombopVersionVo autoexecCombopVersionVo = paramObj.toJavaObject(AutoexecCombopVersionVo.class);
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecCombopVersionVo.getCombopId());
        }
        List<AutoexecCombopVersionVo> autoexecCombopVersionList = new ArrayList<>();
        int rowNum = autoexecCombopVersionMapper.getAutoexecCombopVersionCount(autoexecCombopVersionVo);
        if (rowNum > 0) {
            autoexecCombopVersionVo.setRowNum(rowNum);
            autoexecCombopVersionList = autoexecCombopVersionMapper.getAutoexecCombopVersionList(autoexecCombopVersionVo);
        }
        return TableResultUtil.getResult(autoexecCombopVersionList, autoexecCombopVersionVo);
    }
}
