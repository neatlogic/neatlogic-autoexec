/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopHasSubmittedVersionException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopPhaseNameRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopVersionSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/version/save";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopversionsaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "common.name"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "draft,submitted", isRequired = true, desc = "common.status"),
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.combopid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "common.id")
    })
    @Description(desc = "nmaac.autoexeccombopversionsaveapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVersionVo autoexecCombopVersionVo = jsonObj.toJavaObject(AutoexecCombopVersionVo.class);
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecCombopVersionVo.getCombopId());
        }
        if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            Long versionId = autoexecCombopVersionMapper.getAutoexecCombopMaxVersionIdByCombopIdAndStatus(autoexecCombopVersionVo.getCombopId(), autoexecCombopVersionVo.getStatus());
            if (versionId != null) {
                throw new AutoexecCombopHasSubmittedVersionException(autoexecCombopVo.getName());
            }
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (autoexecCombopVo.getEditable() == 0) {
            throw new PermissionDeniedException();
        }
        Long id = jsonObj.getLong("id");
        if (id != null) {
            AutoexecCombopVersionVo oldAutoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
            if (oldAutoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(id);
            }
        }
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        List<String> nameList = new ArrayList<>();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            String name = autoexecCombopPhaseVo.getName();
            if (nameList.contains(name)) {
                throw new AutoexecCombopPhaseNameRepeatException(name);
            }
            nameList.add(name);
        }
        autoexecCombopService.saveAutoexecCombopVersion(autoexecCombopVersionVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", autoexecCombopVersionVo.getId());
        if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            resultObj.put("reviewable", autoexecCombopVo.getReviewable());
        }
        return resultObj;
    }

}
