/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.node;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/22 14:20
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecNodeUserListApi extends PrivateApiComponentBase {

    private final static List<ValueTextVo> AUTOEXEC_NODE_USER_VO_LIST = new ArrayList<>();

    static {
        AUTOEXEC_NODE_USER_VO_LIST.add(new ValueTextVo("root", "root"));
        AUTOEXEC_NODE_USER_VO_LIST.add(new ValueTextVo("admin", "admin"));
    }

    @Override
    public String getToken() {
        return "autoexec/node/user/list";
    }

    @Override
    public String getName() {
        return "查询执行用户列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "list", explode = ValueTextVo[].class, desc = "执行用户列表")
    })
    @Description(desc = "查询执行用户列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        BasePageVo searchVo = JSON.toJavaObject(jsonObj, BasePageVo.class);
        int pageSize = searchVo.getPageSize();
        int rowNum = AUTOEXEC_NODE_USER_VO_LIST.size();
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            String keyword = searchVo.getKeyword();
            List<ValueTextVo> resultList = new ArrayList<>();
            for (ValueTextVo valueTextVo : AUTOEXEC_NODE_USER_VO_LIST) {
                if (StringUtils.isNotBlank(keyword)) {
                    if (valueTextVo.getText().contains(keyword)) {
                        resultList.add(valueTextVo);
                    } else if (valueTextVo.getValue().toString().contains(keyword)) {
                        resultList.add(valueTextVo);
                    }
                } else {
                    resultList.add(valueTextVo);
                }
            }
            int size = resultList.size();
            int fromIndex = searchVo.getStartNum();
            fromIndex = fromIndex < size ? fromIndex : size;
            int toIndex = fromIndex + pageSize;
            toIndex = toIndex < size ? toIndex : size;
            resultObj.put("list", resultList.subList(fromIndex, toIndex));
        } else {
            resultObj.put("list", new ArrayList<>());
        }
        resultObj.put("rowNum", searchVo.getRowNum());
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", pageSize);
        return resultObj;
    }
}
