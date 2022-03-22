package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileOptionVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class AutoexecProfileServiceImpl implements AutoexecProfileService {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    /**
     * 获取profile参数
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileParamById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        //获取关联的工具
        List<AutoexecProfileOptionVo> profileOptionVoList = autoexecProfileMapper.getProfileToolListByProfileId(id);
        Map<String, List<AutoexecProfileOptionVo>> toolAndScriptMap = profileOptionVoList.stream().collect(Collectors.groupingBy(AutoexecProfileOptionVo::getType));
        return getProfileConfig(toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecProfileOptionVo::getOptionId).collect(Collectors.toList()), toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecProfileOptionVo::getOptionId).collect(Collectors.toList()), profileVo.getConfig().toJavaObject(JSONArray.class));
    }

    @Override
    public List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, JSONArray paramList) {
        List<AutoexecToolAndScriptVo> allAutoexecToolAndScriptVo = new ArrayList<>();
        allAutoexecToolAndScriptVo.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        allAutoexecToolAndScriptVo.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));


        return null;
    }

}
