/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.core.validate;

import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.scriptcheck.ScriptCheckHandlerBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptValidateMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class PowershellScriptCheckHandler extends ScriptCheckHandlerBase {

    @Resource
    private AutoexecScriptValidateMapper validateMapper;

    /**
     * 对每个非注释行进行分词，与词库对比，看是否存在危险代码
     *
     * @param lineList
     */
    @Override
    protected void myCheck(List<AutoexecScriptLineVo> lineList) {
        // todo 待补充校验逻辑
    }

    /**
     * 标记注释行
     *
     * @param lineList
     */
    @Override
    protected void markAnnotationLines(List<AutoexecScriptLineVo> lineList) {
    }

    @Override
    public String getType() {
        return ScriptParser.POWERSHELL.getValue();
    }
}
