package codedriver.module.autoexec.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_PROFILE_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecProfileService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/18 10:08 上午
 */
@Service
@Transactional
@AuthAction(action = AUTOEXEC_PROFILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecProfileSaveApi extends PrivateApiComponentBase {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecProfileService autoexecProfileService;

    @Override
    public String getName() {
        return "保存自动化工具profile";
    }

    @Override
    public String getToken() {
        return "autoexec/profile/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "profile id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "profile 名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "工具参数"),
            @Param(name = "autoexecToolAndScriptVoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "自动化工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecProfileVo profileVo = JSON.toJavaObject(paramObj, AutoexecProfileVo.class);
        List<AutoexecToolAndScriptVo> toolAndScriptVoList = profileVo.getAutoexecToolAndScriptVoList();
        //分类 类型(tool:工具;script:脚本)
        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = toolAndScriptVoList.stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));

        //tool
        List<Long> toolIdList = toolAndScriptMap.get("tool").stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecProfileMapper.insertAutoexecProfileTooLByProfileIdAndTooLIdList(profileVo.getId(), toolIdList);
        }
        //script
        List<Long> scriptIdList = toolAndScriptMap.get("script").stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecProfileMapper.insertAutoexecProfileScriptByProfileIdAndScriptIdList(profileVo.getId(), toolIdList);
        }
        profileVo.setInputParamList(autoexecProfileService.getProfileConfig(toolIdList,scriptIdList,paramObj.getJSONArray("paramList")));
        autoexecProfileMapper.insertProfile(profileVo);
        return null;
    }
}
