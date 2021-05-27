/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptAndToolOperate;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.OperateVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecToolSearchApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/search";
    }

    @Override
    public String getName() {
        return "查询工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target", desc = "执行方式"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类ID列表"),
            @Param(name = "riskIdList", type = ApiParamType.JSONARRAY, desc = "操作级别ID列表"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecToolVo[].class, desc = "工具列表"),
            @Param(name = "operateList", type = ApiParamType.JSONARRAY, desc = "操作按钮"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecToolVo toolVo = JSON.toJavaObject(jsonObj, AutoexecToolVo.class);
        List<AutoexecToolVo> toolVoList = autoexecToolMapper.searchTool(toolVo);
        result.put("tbodyList", toolVoList);
        if (CollectionUtils.isNotEmpty(toolVoList)) {
            List<AutoexecToolVo> list = autoexecToolMapper.checkToolListHasBeenGeneratedToCombop(toolVoList.stream().map(AutoexecToolVo::getId).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(list)) {
                for (AutoexecToolVo vo : list) {
                    toolVoList.stream().forEach(o -> {
                        if (Objects.equals(o.getId(), vo.getId())) {
                            o.setHasBeenGeneratedToCombop(vo.getHasBeenGeneratedToCombop() > 0 ? 1 : 0);
                        }
                    });
                }
            }
            // 获取操作按钮
            String userUuid = UserContext.get().getUserUuid();
            toolVoList.stream().forEach(o -> {
                List<OperateVo> operateList = new ArrayList<>();
                if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
                    operateList.add(new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText()));
                }
                if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_COMBOP_MODIFY.class.getSimpleName())) {
                    OperateVo vo = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
                    if (Objects.equals(o.getHasBeenGeneratedToCombop(), 1)) {
                        vo.setDisabled(1);
                        vo.setDisabledReason("已经被发布为组合工具");
                    } else if (!Objects.equals(o.getIsActive(), 1)) {
                        vo.setDisabled(1);
                        vo.setDisabledReason("工具未激活");
                    }
                    operateList.add(vo);
                }
                if (AuthActionChecker.checkByUserUuid(userUuid, AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
                    operateList.add(new OperateVo("active", "启用/禁用"));
                }
                if (CollectionUtils.isNotEmpty(operateList)) {
                    o.setOperateList(operateList);
                }
            });
        }
        if (toolVo.getNeedPage()) {
            int rowNum = autoexecToolMapper.searchToolCount(toolVo);
            toolVo.setRowNum(rowNum);
            result.put("currentPage", toolVo.getCurrentPage());
            result.put("pageSize", toolVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, toolVo.getPageSize()));
            result.put("rowNum", toolVo.getRowNum());
        }
        return result;
    }


}
