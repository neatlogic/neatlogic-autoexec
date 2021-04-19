/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @since 2021/4/12 18:44
 **/
@Service
public class AutoexecJobServiceImpl implements AutoexecJobService {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public void saveAutoexecCombopJob(AutoexecCombopVo combopVo, String source, Integer threadCount) {
        JSONObject config = combopVo.getConfig();
        AutoexecJobVo jobVo = new AutoexecJobVo(combopVo,CombopOperationType.COMBOP.getValue(),source,threadCount);
        autoexecJobMapper.insertJob(jobVo);
        
    }

}
