/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.NodeStatus;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeStatusVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 检查执行目标及对应的执行用户是否存在接口
 *
 * @author: linbq
 * @since: 2021/4/23 10:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecNodeCheckApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "autoexec/node/check";
    }

    @Override
    public String getName() {
        return "检查执行目标及对应的执行用户是否存在";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "executeUser", type = ApiParamType.STRING, isRequired = true, desc = "执行用户"),
            @Param(name = "selectNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "选择节点列表"),
            @Param(name = "inputNodeList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "输入节点列表")
    })
    @Output({
            @Param(name = "selectNodeList", explode = AutoexecNodeVo[].class, desc = "选择节点列表"),
            @Param(name = "inputNodeList", explode = AutoexecNodeVo[].class, desc = "输入节点列表"),
            @Param(name = "allNormal", type = ApiParamType.INTEGER, desc = "是否全部正常")
    })
    @Description(desc = "检查执行目标及对应的执行用户是否存在")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        int allNormal = 1;
        List<AutoexecNodeVo> existNodeList = new ArrayList<>();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject(jsonObj, AutoexecCombopExecuteNodeConfigVo.class);
        List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
        for (AutoexecNodeVo autoexecNodeVo : selectNodeList) {
            AutoexecNodeVo autoexecNode = checkAutoexecNodeIsExists(autoexecNodeVo);
            if (autoexecNode != null) {
                autoexecNodeVo.setId(autoexecNode.getId());
                existNodeList.add(autoexecNodeVo);
            } else {
                autoexecNodeVo.setId(-1L);
                autoexecNodeVo.setStatusVo(new AutoexecNodeStatusVo(NodeStatus.NODE_NOT_FOUND));
                allNormal = 0;
            }
        }
        List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
        for (AutoexecNodeVo autoexecNodeVo : inputNodeList) {
            AutoexecNodeVo autoexecNode = checkAutoexecNodeIsExists(autoexecNodeVo);
            if (autoexecNode != null) {
                autoexecNodeVo.setId(autoexecNode.getId());
                existNodeList.add(autoexecNodeVo);
            } else {
                autoexecNodeVo.setId(-1L);
                autoexecNodeVo.setStatusVo(new AutoexecNodeStatusVo(NodeStatus.NODE_NOT_FOUND));
                allNormal = 0;
            }
        }
        String executeUser = jsonObj.getString("executeUser");
        boolean executeUserExist = false;
        for (ValueTextVo valueTextVo : AutoexecNodeUserListApi.AUTOEXEC_NODE_USER_VO_LIST) {
            if (Objects.equals(valueTextVo.getValue(), executeUser)) {
                executeUserExist = true;
                break;
            }
        }
        for (AutoexecNodeVo autoexecNodeVo : existNodeList) {
            if (executeUserExist) {
                if (checkAutoexecNodeExecuteUserExists(autoexecNodeVo, executeUser)) {
                    autoexecNodeVo.setStatusVo(new AutoexecNodeStatusVo(NodeStatus.NORMAL));
                } else {
                    autoexecNodeVo.setStatusVo(new AutoexecNodeStatusVo(NodeStatus.USER_NOT_FOUND));
                    allNormal = 0;
                }
            } else {
                autoexecNodeVo.setStatusVo(new AutoexecNodeStatusVo(NodeStatus.USER_NOT_FOUND));
                allNormal = 0;
            }
        }
        JSONObject resultObj = (JSONObject) JSONObject.toJSON(executeNodeConfigVo);
        resultObj.put("allNormal", allNormal);
        return resultObj;
    }

    /**
     * 检查执行目标是否存在
     *
     * @param autoexecNodeVo 执行目标信息Vo
     * @return
     */
    private AutoexecNodeVo checkAutoexecNodeIsExists(AutoexecNodeVo autoexecNodeVo) {
        for (AutoexecNodeVo autoexecNode : AutoexecNodeListApi.AUTOEXEC_NODE_VO_LIST) {
            if (Objects.equals(autoexecNode.getHost(), autoexecNodeVo.getHost()) && Objects.equals(autoexecNode.getPort(), autoexecNodeVo.getPort())) {
                return autoexecNode;
            }
        }
        return null;
    }

    /**
     * 检查执行目标中执行用户是否存在
     *
     * @param autoexecNodeVo 执行目标信息Vo
     * @param executeUser    执行用户
     * @return
     */
    private boolean checkAutoexecNodeExecuteUserExists(AutoexecNodeVo autoexecNodeVo, String executeUser) {
        // TODO linbq 造假数据测试
        if (autoexecNodeVo.getId() % 2 == 0) {
            return true;
        } else {
            return false;
        }
    }
}
