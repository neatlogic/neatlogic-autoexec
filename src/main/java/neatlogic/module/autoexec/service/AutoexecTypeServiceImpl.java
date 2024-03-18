/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.AutoexecTypeAuthorityAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeAuthVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.UserType;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class AutoexecTypeServiceImpl implements AutoexecTypeService {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TeamMapper teamMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public Long saveAutoexecType(AutoexecTypeVo typeVo) {
        typeVo.setLcu(UserContext.get().getUserUuid());
        if (autoexecTypeMapper.checkTypeIsExistsById(typeVo.getId()) == 0) {
            autoexecTypeMapper.insertType(typeVo);
        } else {
            autoexecTypeMapper.deleteTypeAuthByTypeId(typeVo.getId());
            autoexecTypeMapper.updateType(typeVo);
        }
        List<String> addAuthList = typeVo.getAuthList();
        if (CollectionUtils.isNotEmpty(addAuthList)) {
            List<AutoexecTypeAuthVo> autoexecTypeAuthList = new ArrayList<>();
            for (String auth : addAuthList) {
                AutoexecTypeAuthVo autoexecTypeAuthVo = convertAutoexecTypeAuthVo(auth);
                if (autoexecTypeAuthVo == null) {
                    continue;
                }
                autoexecTypeAuthVo.setTypeId(typeVo.getId());
                autoexecTypeAuthVo.setAction(AutoexecTypeAuthorityAction.ADD.getValue());
                autoexecTypeAuthList.add(autoexecTypeAuthVo);
            }
            autoexecTypeMapper.insertTypeAuthList(autoexecTypeAuthList);
        }
        List<String> reviewAuthList = typeVo.getReviewAuthList();
        if (CollectionUtils.isNotEmpty(reviewAuthList)) {
            List<AutoexecTypeAuthVo> autoexecTypeAuthList = new ArrayList<>();
            for (String auth : reviewAuthList) {
                AutoexecTypeAuthVo autoexecTypeAuthVo = convertAutoexecTypeAuthVo(auth);
                if (autoexecTypeAuthVo == null) {
                    continue;
                }
                autoexecTypeAuthVo.setTypeId(typeVo.getId());
                autoexecTypeAuthVo.setAction(AutoexecTypeAuthorityAction.REVIEW.getValue());
                autoexecTypeAuthList.add(autoexecTypeAuthVo);
            }
            autoexecTypeMapper.insertTypeAuthList(autoexecTypeAuthList);
        }
        return null;
    }

    private AutoexecTypeAuthVo convertAutoexecTypeAuthVo(String authority) {
        if (StringUtils.isNotBlank(authority) && authority.contains("#")) {
            String[] split = authority.split("#");
            if (GroupSearch.USER.getValue().equals(split[0])) {
                if (userMapper.checkUserIsExists(split[1]) == 0) {
                    return null;
                }
            } else if (GroupSearch.TEAM.getValue().equals(split[0])) {
                if (teamMapper.checkTeamIsExists(split[1]) == 0) {
                    return null;
                }
            } else if (GroupSearch.ROLE.getValue().equals(split[0])) {
                if (roleMapper.checkRoleIsExists(split[1]) == 0) {
                    return null;
                }
            } else if (GroupSearch.COMMON.getValue().equals(split[0])) {
                if (!UserType.ALL.getValue().equals(split[1])) {
                    return null;
                }
            } else {
                return null;
            }
            AutoexecTypeAuthVo autoexecTypeAuthVo = new AutoexecTypeAuthVo();
            autoexecTypeAuthVo.setAuthType(split[0]);
            autoexecTypeAuthVo.setAuthUuid(split[1]);
            return autoexecTypeAuthVo;
        }
        return null;
    }
}
