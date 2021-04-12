/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.servcie;

import codedriver.framework.autoexec.dto.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AutoexecScriptServiceImpl implements AutoexecScriptService {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    @Override
    public AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId) {
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        version.setParamList(autoexecScriptMapper.getParamListByVersionId(versionId));
        version.setLineList(autoexecScriptMapper.getLineListByVersionId(versionId));
        return version;
    }
}
