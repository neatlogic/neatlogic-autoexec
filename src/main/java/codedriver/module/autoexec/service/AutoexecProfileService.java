package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecParamVo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
public interface AutoexecProfileService {


    /**
     * 获取profile参数
     *
     * @param id
     * @return
     */
    List<AutoexecParamVo> getProfileParamById(Long id);

    /**
     * 获取工具参数并去重
     *
     * @param toolIdList   工具id
     * @param scriptIdList 脚本id
     * @param config       工具参数
     * @return
     */
    List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, JSONObject config);
}
