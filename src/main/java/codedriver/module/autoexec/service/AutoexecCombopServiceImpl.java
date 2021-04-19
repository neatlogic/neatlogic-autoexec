/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopAtLeastOnePhaseException;
import codedriver.framework.autoexec.exception.AutoexecCombopPhaseAtLeastOneOperationException;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author: linbq
 * @since: 2021/4/15 16:13
 **/
@Service
public class AutoexecCombopServiceImpl implements AutoexecCombopService {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private TeamMapper teamMapper;

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    @Override
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo) {
        String userUuid = UserContext.get().getUserUuid(true);
        if (Objects.equals(autoexecCombopVo.getFcu(), userUuid)) {
            autoexecCombopVo.setEditable(1);
            autoexecCombopVo.setDeletable(1);
            if (Objects.equals(autoexecCombopVo.getIsActive(), 1)) {
                autoexecCombopVo.setExecutable(1);
            }
        } else {
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, teamUuidList, roleUuidList);
            if (authorityList.contains(CombopAuthorityAction.EDIT.getValue())) {
                autoexecCombopVo.setEditable(1);
                autoexecCombopVo.setDeletable(1);
            } else {
                autoexecCombopVo.setEditable(0);
                autoexecCombopVo.setDeletable(0);
            }
            if (Objects.equals(autoexecCombopVo.getIsActive(), 1) && authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
    }

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param autoexecCombopVo 组合工具Vo对象
     * @return
     */
    @Override
    public boolean verifyAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo) {
        JSONObject config = autoexecCombopVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        JSONArray combopPhaseList = config.getJSONArray("combopPhaseList");
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
//        List<Long> prePhaseOperationIdList = new ArrayList<>();
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.getObject(i, AutoexecCombopPhaseVo.class);
            if (autoexecCombopPhaseVo != null) {
                JSONObject phaseConfig = autoexecCombopPhaseVo.getConfig();
                if (MapUtils.isEmpty(phaseConfig)) {
                    throw new AutoexecCombopPhaseAtLeastOneOperationException();
                }
                JSONArray phaseOperationList = phaseConfig.getJSONArray("phaseOperationList");
                if (CollectionUtils.isEmpty(phaseOperationList)) {
                    throw new AutoexecCombopPhaseAtLeastOneOperationException();
                }
                for (int j = 0; j < phaseOperationList.size(); j++) {
                    AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo = phaseOperationList.getObject(j, AutoexecCombopPhaseOperationVo.class);
                    if (autoexecCombopPhaseOperationVo != null) {
//                        JSONObject operationConfig = autoexecCombopPhaseOperationVo.getConfig();
//                        if(MapUtils.isNotEmpty(operationConfig)){
//                            JSONArray paramList = operationConfig.getJSONArray("paramList");
//                            if(CollectionUtils.isNotEmpty(paramList)){
//
//                            }
//                        }
//                        prePhaseOperationIdList.add(autoexecCombopPhaseOperationVo.getOperationId());
                    }
                }
//                JSONArray phaseNodeList = phaseConfig.getJSONArray("phaseNodeList");
//                if (CollectionUtils.isNotEmpty(phaseNodeList)) {
//                }
            }
        }
//        JSONArray combopNodeList = config.getJSONArray("combopNodeList");
//        if (CollectionUtils.isNotEmpty(combopNodeList)) {
//        }
        return true;
    }
}
