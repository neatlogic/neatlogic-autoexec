/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.fulltextindex;

import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerBase;
import codedriver.framework.fulltextindex.core.IFullTextIndexType;
import codedriver.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.fulltextindex.dto.globalsearch.DocumentVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ScriptLineFullTetIndexHandler extends FullTextIndexHandlerBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    protected String getModuleId() {
        return "autoexec";
    }

    @Override
    protected void myCreateIndex(FullTextIndexVo fullTextIndexVo) {
        List<AutoexecScriptLineVo> lineList = autoexecScriptMapper.getLineListByVersionId(fullTextIndexVo.getTargetId());
        StringBuilder sb = new StringBuilder();
        for (AutoexecScriptLineVo line : lineList) {
            if (StringUtils.isNotBlank(line.getContent())) {
                sb.append(line.getContent());
            }
        }
        fullTextIndexVo.addFieldContent("content", new FullTextIndexVo.WordVo(sb.toString()));
    }

    @Override
    protected void myMakeupDocument(DocumentVo documentVo) {

    }

    @Override
    public IFullTextIndexType getType() {
        return AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION;
    }

    @Override
    public void myRebuildIndex(String type, Boolean isRebuildAll) {

    }
}
