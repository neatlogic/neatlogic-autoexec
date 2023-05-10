/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.core.validate;

import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.constvalue.ScriptRiskCodeLevel;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptValidateVo;
import neatlogic.framework.autoexec.scriptcheck.ScriptCheckHandlerBase;
import neatlogic.framework.autoexec.util.ArgumentTokenizer;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptValidateMapper;
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
