/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.*;

import java.util.List;

public interface AutoexecScriptMapper {

    public int checkScriptIsExistsById(Long id);

    public int checkScriptNameIsExists(AutoexecScriptVo vo);

    public int checkScriptLabelIsExists(AutoexecScriptVo vo);

    public int checkScriptLineContentHashIsExists(String hash);

    public AutoexecScriptVersionVo getVersionByVersionId(Long versionId);

    public Integer getMaxVersionByScriptId(Long id);

    public List<AutoexecScriptVersionParamVo> getParamListByVersionId(Long versionId);

    public List<AutoexecScriptLineVo> getLineListByVersionId(Long versionId);

    public int updateScriptVersion(AutoexecScriptVersionVo versionVo);

    public int insertScript(AutoexecScriptVo vo);

    public int insertScriptVersion(AutoexecScriptVersionVo versionVo);

    public int insertScriptVersionParamList(List<AutoexecScriptVersionParamVo> paramList);

    public int insertScriptLineContent(AutoexecScriptLineContentVo contentVo);

    public int insertScriptLineList(List<AutoexecScriptLineVo> lineList);

    public int deleteParamByVersionId(Long versionId);

    public int deleteScriptLineByVersionId(Long versionId);

}
