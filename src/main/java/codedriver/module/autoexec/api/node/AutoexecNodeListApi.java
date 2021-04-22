/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.node;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
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
 * 查询执行目标列表接口
 *
 * @author: linbq
 * @since: 2021/4/22 14:20
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecNodeListApi extends PrivateApiComponentBase {

    private final static List<AutoexecNodeVo> AUTOEXEC_NODE_VO_LIST = new ArrayList<>();

    static {
        int sort = 0;
        long id = 340994557796350L;
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
        AUTOEXEC_NODE_VO_LIST.add(new AutoexecNodeVo(id + sort, "执行目标" + sort, "192.168.0." + sort, 8080 + sort++, "linux"));
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口唯一标识，也是访问URI
     */
    @Override
    public String getToken() {
        return "autoexec/node/list";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "查询执行目标列表";
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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AutoexecNodeVo[].class, desc = "执行目标列表")
    })
    @Description(desc = "查询执行目标列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        BasePageVo searchVo = JSON.toJavaObject(jsonObj, BasePageVo.class);
        int pageSize = searchVo.getPageSize();
        int rowNum = AUTOEXEC_NODE_VO_LIST.size();
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            String keyword = searchVo.getKeyword();
            List<AutoexecNodeVo> autoexecNodeVoList = new ArrayList<>();
            for (AutoexecNodeVo autoexecNodeVo : AUTOEXEC_NODE_VO_LIST) {
                if (StringUtils.isNotBlank(keyword)) {
                    if (autoexecNodeVo.getName().contains(keyword)) {
                        autoexecNodeVoList.add(autoexecNodeVo);
                    } else if (autoexecNodeVo.getHost().contains(keyword)) {
                        autoexecNodeVoList.add(autoexecNodeVo);
                    } else if (autoexecNodeVo.getPort().toString().contains(keyword)) {
                        autoexecNodeVoList.add(autoexecNodeVo);
                    }
                } else {
                    autoexecNodeVoList.add(autoexecNodeVo);
                }
            }
            int size = autoexecNodeVoList.size();
            int fromIndex = searchVo.getStartNum();
            fromIndex = fromIndex < rowNum ? fromIndex : rowNum;
            int toIndex = fromIndex + pageSize;
            toIndex = toIndex < rowNum ? toIndex : rowNum;
            resultObj.put("tbodyList", autoexecNodeVoList.subList(fromIndex, toIndex));
        } else {
            resultObj.put("tbodyList", new ArrayList<>());
        }
        resultObj.put("rowNum", searchVo.getRowNum());
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", pageSize);
        return resultObj;
    }
}
