/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopAuthorityVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopBasicInfoGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/basic/info/get";
    }

    @Override
    public String getName() {
        return "查询组合工具基本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", isRequired = true, desc = "状态")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "组合工具基本信息")
    })
    @Description(desc = "查询组合工具基本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopVo.getTypeId());
        if (autoexecTypeVo != null) {
            autoexecCombopVo.setTypeName(autoexecTypeVo.getName() + "[" + autoexecTypeVo.getDescription()+ "]");
        } else {
            autoexecCombopVo.setTypeName(autoexecCombopVo.getTypeId().toString());
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        // owner字段必须在校验权限后，再加上前缀user#
        autoexecCombopVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopVo.getOwner());
        List<String> viewAuthorityList = new ArrayList<>();
        List<String> editAuthorityList = new ArrayList<>();
        List<String> executeAuthorityList = new ArrayList<>();
        List<AutoexecCombopAuthorityVo> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopId(id);
        for (AutoexecCombopAuthorityVo authorityVo : authorityList) {
            if ("view".equals(authorityVo.getAction())) {
                viewAuthorityList.add(authorityVo.getType() + "#" + authorityVo.getUuid());
            } else if ("edit".equals(authorityVo.getAction())) {
                editAuthorityList.add(authorityVo.getType() + "#" + authorityVo.getUuid());
            } else if ("execute".equals(authorityVo.getAction())) {
                executeAuthorityList.add(authorityVo.getType() + "#" + authorityVo.getUuid());
            }
        }
        autoexecCombopVo.setViewAuthorityList(viewAuthorityList);
        autoexecCombopVo.setEditAuthorityList(editAuthorityList);
        autoexecCombopVo.setExecuteAuthorityList(executeAuthorityList);
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(id);
        autoexecCombopVo.setActiveVersionId(activeVersionId);
        String versionStatus = jsonObj.getString("versionStatus");
        if (Objects.equals(versionStatus, ScriptVersionStatus.PASSED.getValue())) {
            autoexecCombopVo.setSpecifyVersionId(activeVersionId);
        } else {
            Long maxVersionId = autoexecCombopVersionMapper.getAutoexecCombopMaxVersionIdByCombopIdAndStatus(id, versionStatus);
            autoexecCombopVo.setSpecifyVersionId(maxVersionId);
        }
        return autoexecCombopVo;
    }
}
