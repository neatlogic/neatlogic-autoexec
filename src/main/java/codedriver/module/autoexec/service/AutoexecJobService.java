/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import com.alibaba.fastjson.JSONArray;

/**
 * @since 2021/4/12 18:44
 **/
public interface AutoexecJobService {
    /**
     * 通过combopVo保存作业配置
     *
     * @param combopVo    组合工具vo
     * @param source      来源
     * @param threadCount 并发线程数
     */
    void saveAutoexecCombopJob(AutoexecCombopVo combopVo, String source, Integer threadCount, JSONArray jobParamList);
}
