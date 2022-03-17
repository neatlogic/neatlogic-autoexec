/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_TEMPLATE_MANAGE;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateAuthorityVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.exception.role.RoleNotFoundException;
import codedriver.framework.exception.team.TeamNotFoundException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
 * 保存组合工具模板授权信息接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_COMBOP_TEMPLATE_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopTemplateAuthoritySaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TeamMapper teamMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/authority/save";
    }

    @Override
    public String getName() {
        return "保存组合工具模板授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "editAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "编辑授权列表"),
            @Param(name = "executeAuthorityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "执行授权列表")
    })
    @Description(desc = "保存组合工具模板授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopTemplateId = jsonObj.getLong("combopTemplateId");
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopById(combopTemplateId);
        if (autoexecCombopTemplateVo == null) {
            throw new AutoexecCombopTemplateNotFoundException(combopTemplateId);
        }
//        autoexecCombopService.setOperableButtonList(autoexecCombopTemplateVo);
//        if (autoexecCombopTemplateVo.getEditable() == 0) {
//            throw new PermissionDeniedException();
//        }
        autoexecCombopTemplateMapper.deleteAutoexecCombopAuthorityByCombopId(combopTemplateId);
        List<AutoexecCombopTemplateAuthorityVo> autoexecCombopTemplateAuthorityVoList = new ArrayList<>();
        for (CombopAuthorityAction authorityAction : CombopAuthorityAction.values()) {
            JSONArray authorityList = jsonObj.getJSONArray(authorityAction.getValue() + "AuthorityList");
            if (CollectionUtils.isNotEmpty(authorityList)) {
                for (int i = 0; i < authorityList.size(); i++) {
                    AutoexecCombopTemplateAuthorityVo autoexecCombopTemplateAuthorityVo = getAutoexecCombopTemplateAuthorityVo(authorityList.getString(i));
                    if (autoexecCombopTemplateAuthorityVo != null) {
                        autoexecCombopTemplateAuthorityVo.setCombopTemplateId(combopTemplateId);
                        autoexecCombopTemplateAuthorityVo.setAction(authorityAction.getValue());
                        autoexecCombopTemplateAuthorityVoList.add(autoexecCombopTemplateAuthorityVo);
                        if (autoexecCombopTemplateAuthorityVoList.size() == 1000) {
                            autoexecCombopTemplateMapper.insertAutoexecCombopAuthorityVoList(autoexecCombopTemplateAuthorityVoList);
                            autoexecCombopTemplateAuthorityVoList.clear();
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(autoexecCombopTemplateAuthorityVoList)) {
            autoexecCombopTemplateMapper.insertAutoexecCombopAuthorityVoList(autoexecCombopTemplateAuthorityVoList);
        }
        return null;
    }

    private AutoexecCombopTemplateAuthorityVo getAutoexecCombopTemplateAuthorityVo(String authority) {
        if (StringUtils.isNotBlank(authority) && authority.contains("#")) {
            String[] split = authority.split("#");
            if (GroupSearch.USER.getValue().equals(split[0])) {
                if (userMapper.checkUserIsExists(split[1]) == 0) {
                    throw new UserNotFoundException(split[1]);
                }
            } else if (GroupSearch.TEAM.getValue().equals(split[0])) {
                if (teamMapper.checkTeamIsExists(split[1]) == 0) {
                    throw new TeamNotFoundException(split[1]);
                }
            } else if (GroupSearch.ROLE.getValue().equals(split[0])) {
                if (roleMapper.checkRoleIsExists(split[1]) == 0) {
                    throw new RoleNotFoundException(split[1]);
                }
            } else if (GroupSearch.COMMON.getValue().equals(split[0])) {
                //TODO linbq
            } else {
                return null;
            }
            AutoexecCombopTemplateAuthorityVo autoexecCombopTemplateAuthorityVo = new AutoexecCombopTemplateAuthorityVo();
            autoexecCombopTemplateAuthorityVo.setType(split[0]);
            autoexecCombopTemplateAuthorityVo.setUuid(split[1]);
            return autoexecCombopTemplateAuthorityVo;
        }
        return null;
    }
}
