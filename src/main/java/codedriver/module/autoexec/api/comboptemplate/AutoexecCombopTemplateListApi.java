/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询组合工具模板列表接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopTemplateListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/list";
    }

    @Override
    public String getName() {
        return "查询组合工具模板列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询，支持名称或描述"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "状态"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecCombopVo[].class, desc = "组合工具模板列表")
    })
    @Description(desc = "查询组合工具模板列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
//        JSONObject resultObj = new JSONObject();
//        int pageCount = 0;
        AutoexecCombopTemplateVo searchVo = jsonObj.toJavaObject(AutoexecCombopTemplateVo.class);
        int rowNum = autoexecCombopTemplateMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
//            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            List<AutoexecCombopTemplateVo> autoexecCombopTemplateList = autoexecCombopTemplateMapper.getAutoexecCombopList(searchVo);
            for (AutoexecCombopTemplateVo autoexecCombopTemplateVo : autoexecCombopTemplateList) {
                AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopTemplateVo.getTypeId());
                if (autoexecTypeVo == null) {
                    throw new AutoexecTypeNotFoundException(autoexecCombopTemplateVo.getTypeId());
                }
                autoexecCombopTemplateVo.setTypeName(autoexecTypeVo.getName());
            }
//            resultObj.put("tbodyList", autoexecCombopTemplateList);
            return TableResultUtil.getResult(autoexecCombopTemplateList, searchVo);
        } else {
//            resultObj.put("tbodyList", new ArrayList<>());
            return TableResultUtil.getResult(new ArrayList<>(), searchVo);
        }
//        resultObj.put("rowNum", rowNum);
//        resultObj.put("pageCount", pageCount);
//        resultObj.put("currentPage", searchVo.getCurrentPage());
//        resultObj.put("pageSize", searchVo.getPageSize());
//        return resultObj;
    }
}
