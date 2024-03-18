/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.core.validate;

import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.constvalue.ScriptRiskCodeLevel;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptValidateMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptValidateVo;
import neatlogic.framework.autoexec.scriptcheck.ScriptCheckHandlerBase;
import neatlogic.framework.autoexec.util.ArgumentTokenizer;
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
        return ScriptParser.SH.getValue();
    }
}
