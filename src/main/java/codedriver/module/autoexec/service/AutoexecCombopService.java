/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import com.alibaba.fastjson.JSONObject;

/**
 * @author: linbq
 * @since: 2021/4/15 16:13
 **/
public interface AutoexecCombopService {

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo);

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param config
     * @return
     */
    public boolean verifyAutoexecCombopConfig(JSONObject config);
}
