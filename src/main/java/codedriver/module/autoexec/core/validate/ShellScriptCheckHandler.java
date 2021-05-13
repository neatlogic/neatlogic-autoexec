/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.core.validate;

import codedriver.framework.autoexec.constvalue.ScriptParser;
import codedriver.framework.autoexec.constvalue.ScriptRiskCodeLevel;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptValidateVo;
import codedriver.framework.autoexec.scriptcheck.ScriptCheckHandlerBase;
import codedriver.framework.autoexec.util.ArgumentTokenizer;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptValidateMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ShellScriptCheckHandler extends ScriptCheckHandlerBase {

    @Resource
    private AutoexecScriptValidateMapper validateMapper;

    /**
     * 对每个非注释行进行分词，与词库对比，看是否存在危险代码
     *
     * @param lineList
     */
    @Override
    protected void myCheck(List<AutoexecScriptLineVo> lineList) {
        List<AutoexecScriptValidateVo> codeList = validateMapper.getCodeListByType(getType());
        if (CollectionUtils.isNotEmpty(codeList)) {
            for (AutoexecScriptLineVo lineVo : lineList) {
                String content = lineVo.getContent();
                if (StringUtils.isNotBlank(content) && !content.trim().startsWith("#")) { // 跳过注释行
                    List<Map<String, String>> tokenize = ArgumentTokenizer.tokenize(content);
                    if (CollectionUtils.isNotEmpty(tokenize)) {
                        String riskCodeLevel = null;
                        for (Map<String, String> map : tokenize) {
                            // 只匹配command
                            if (Objects.equals(map.get("type"), "command")) {
                                String value = map.get("value");
                                if (codeList.stream().anyMatch(o -> o.getCode().equals(value)
                                        && ScriptRiskCodeLevel.WARNING.getValue().equals(o.getLevel()))) {
                                    riskCodeLevel = ScriptRiskCodeLevel.WARNING.getValue();
                                } else if (codeList.stream().anyMatch(o -> o.getCode().equals(value)
                                        && ScriptRiskCodeLevel.CRITICAL.getValue().equals(o.getLevel()))) {
                                    riskCodeLevel = ScriptRiskCodeLevel.CRITICAL.getValue();
                                }
                            }
                            // 可能存在多个等级的危险代码，取等级最高的
                            if (ScriptRiskCodeLevel.CRITICAL.getValue().equals(riskCodeLevel)) {
                                break;
                            }
                        }
                        lineVo.setRiskCodeLevel(riskCodeLevel);
                    }
                }
            }
        }

    }

    /**
     * 标记注释行
     *
     * @param lineList
     */
    @Override
    protected void markAnnotationLines(List<AutoexecScriptLineVo> lineList) {
        lineList.stream().forEach(o -> {
            if (o.getContent().startsWith("#")) {
                o.setIsAnnotation(1);
            }
        });
        // todo shell存在多种多行注释方式
//        for (int i = 0; i < lineList.size(); i++) {
//            if (lineList.get(i).getContent().startsWith("#")) {
//                lineList.get(i).setIsAnnotation(1);
//            } else if (lineList.get(i).getContent().startsWith(":<<!")) {
//                while (lineList.get(i).getContent().startsWith(":<<!")
//                        || !lineList.get(i).getContent().endsWith("!")) {
//                    lineList.get(i).setIsAnnotation(1);
//                    i++;
//                    if (lineList.get(i).getContent().endsWith("!")) {
//                        lineList.get(i).setIsAnnotation(1);
//                    }
//                }
//            }
//        }
    }

    @Override
    public String getType() {
        return ScriptParser.SHELL.getValue();
    }
}
