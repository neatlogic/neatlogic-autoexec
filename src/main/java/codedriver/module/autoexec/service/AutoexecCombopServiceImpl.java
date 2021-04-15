/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
            autoexecCombopVo.setExecutable(1);
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
            if (authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
    }
}
