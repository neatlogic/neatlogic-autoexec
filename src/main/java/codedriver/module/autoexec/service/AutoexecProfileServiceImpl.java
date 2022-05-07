package codedriver.module.autoexec.service;


import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class AutoexecProfileServiceImpl implements AutoexecProfileService {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    /**
     * 根据profileId 获取profile参数
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
        IAutoexecServiceCrossoverService iAutoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        return iAutoexecServiceCrossoverService.getAutoexecOperationParamVoList(profileVo.getAutoexecOperationVoList(), autoexecProfileMapper.getProfileParamListByProfileId(id));
    }

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId               profile id
     * @param autoexecOperationVoList 自动化工具list
     */
    @Override
    public void saveProfileOperation(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList) {
        List<Long> toolIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //tool
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, toolIdList, ToolType.TOOL.getValue());
        }
        //script
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, scriptIdList, ToolType.SCRIPT.getValue());
        }
    }

    /**
     * 保存profile、profile参数、profile参数引用全局参数的关系
     *
     * @param profileVo profile
     */
    @Override
    public void insertProfile(AutoexecProfileVo profileVo) {
        autoexecProfileMapper.insertProfile(profileVo);
        List<AutoexecProfileParamVo> paramList = profileVo.getParamList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            autoexecProfileMapper.insertAutoexecProfileParam(paramList);
        }
    }
}
