/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询组合工具列表接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private TeamMapper teamMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/list";
    }

    @Override
    public String getName() {
        return "查询组合工具列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询，支持名称或唯一标识"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "状态"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecCombopVo[].class, desc = "组合工具列表")
    })
    @Description(desc = "查询组合工具列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        int pageCount = 0;
        AutoexecCombopVo searchVo = JSON.toJavaObject(jsonObj, AutoexecCombopVo.class);
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            List<AutoexecCombopVo> autoexecCombopList = autoexecCombopMapper.getAutoexecCombopList(searchVo);
            for (AutoexecCombopVo autoexecCombopVo : autoexecCombopList) {
                AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopVo.getTypeId());
                if (autoexecTypeVo == null) {
                    throw new AutoexecTypeNotFoundException(autoexecCombopVo.getTypeId());
                }
                autoexecCombopVo.setTypeName(autoexecTypeVo.getName());
                autoexecCombopService.setOperableButtonList(autoexecCombopVo);
            }
            resultObj.put("tbodyList", autoexecCombopList);
        } else {
            resultObj.put("tbodyList", new ArrayList<>());
        }
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", pageCount);
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        return resultObj;
    }
}
