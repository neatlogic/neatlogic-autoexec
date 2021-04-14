/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopAuthorityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.exception.role.RoleNotFoundException;
import codedriver.framework.exception.team.TeamNotFoundException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存组合工具授权信息接口
 *
 * @author: linbq
 * @since: 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action= AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopAuthoritySaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TeamMapper teamMapper;
    @Resource
    private RoleMapper roleMapper;

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口唯一标识，也是访问URI
     */
    @Override
    public String getToken() {
        return "autoexec/combop/authority/save";
    }

    /**
     * @return String
     * @Author: chenqiwei
     * @Time:Jun 19, 2020
     * @Description: 接口中文名
     */
    @Override
    public String getName() {
        return "保存组合工具授权信息";
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
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "edit", type = ApiParamType.JSONARRAY, isRequired = true, desc = "编辑授权数组"),
            @Param(name = "execute", type = ApiParamType.JSONARRAY, isRequired = true, desc = "执行授权数组")
    })
    @Description(desc = "保存组合工具授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if(autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0){
            throw new AutoexecCombopNotFoundException(combopId);
        }
        List<AutoexecCombopAuthorityVo> autoexecCombopAuthorityVoList = new ArrayList<>();
        JSONArray edit = jsonObj.getJSONArray("edit");
        if(CollectionUtils.isNotEmpty(edit)){
            for (int i = 0; i < edit.size(); i++) {
                AutoexecCombopAuthorityVo autoexecCombopAuthorityVo = getAutoexecCombopAuthorityVo(edit.getString(i));
                if(autoexecCombopAuthorityVo != null){
                    autoexecCombopAuthorityVo.setCombopId(combopId);
                    autoexecCombopAuthorityVo.setAction(CombopAuthorityAction.EDIT.getValue());
                    autoexecCombopAuthorityVoList.add(autoexecCombopAuthorityVo);
                    if(autoexecCombopAuthorityVoList.size() == 1000){
                        autoexecCombopMapper.insertAutoexecCombopAuthorityVoList(autoexecCombopAuthorityVoList);
                        autoexecCombopAuthorityVoList.clear();
                    }
                }
            }
        }
        JSONArray execute = jsonObj.getJSONArray("execute");
        if(CollectionUtils.isNotEmpty(execute)){
            for (int i = 0; i < execute.size(); i++) {
                AutoexecCombopAuthorityVo autoexecCombopAuthorityVo = getAutoexecCombopAuthorityVo(execute.getString(i));
                if(autoexecCombopAuthorityVo != null){
                    autoexecCombopAuthorityVo.setCombopId(combopId);
                    autoexecCombopAuthorityVo.setAction(CombopAuthorityAction.EXECUTE.getValue());
                    autoexecCombopAuthorityVoList.add(autoexecCombopAuthorityVo);
                    if(autoexecCombopAuthorityVoList.size() == 1000){
                        autoexecCombopMapper.insertAutoexecCombopAuthorityVoList(autoexecCombopAuthorityVoList);
                        autoexecCombopAuthorityVoList.clear();
                    }
                }
            }
        }
        autoexecCombopMapper.deleteAutoexecCombopAuthorityByCombopId(combopId);
        if(CollectionUtils.isNotEmpty(autoexecCombopAuthorityVoList)){
            autoexecCombopMapper.insertAutoexecCombopAuthorityVoList(autoexecCombopAuthorityVoList);
        }
        return null;
    }

    private AutoexecCombopAuthorityVo getAutoexecCombopAuthorityVo(String authority){
        if(StringUtils.isNotBlank(authority) && authority.contains("#")){
            String[] split = authority.split("#");
            if(GroupSearch.USER.getValue().equals(split[0])) {
                if(userMapper.checkUserIsExists(split[1]) == 0) {
                    throw new UserNotFoundException(split[1]);
                }
            }else if(GroupSearch.TEAM.getValue().equals(split[0])) {
                if(teamMapper.checkTeamIsExists(split[1]) == 0) {
                    throw new TeamNotFoundException(split[1]);
                }
            }else if(GroupSearch.ROLE.getValue().equals(split[0])) {
                if(roleMapper.checkRoleIsExists(split[1]) == 0) {
                    throw new RoleNotFoundException(split[1]);
                }
            }else if(GroupSearch.COMMON.getValue().equals(split[0])){
                //TODO linbq
            }else {
                return null;
            }
            AutoexecCombopAuthorityVo autoexecCombopAuthorityVo = new AutoexecCombopAuthorityVo();
            autoexecCombopAuthorityVo.setType(split[0]);
            autoexecCombopAuthorityVo.setUuid(split[1]);
            return autoexecCombopAuthorityVo;
        }
        return null;
    }
}
