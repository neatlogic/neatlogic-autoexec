/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.script.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoexecScriptMapper {

    public AutoexecScriptVo getScriptBaseInfoById(Long id);

    public int checkScriptIsExistsById(Long id);

    public int checkScriptNameIsExists(AutoexecScriptVo vo);

    public int checkScriptUkIsExists(AutoexecScriptVo vo);

    public int checkScriptLineContentHashIsExists(String hash);

    public AutoexecScriptVersionVo getVersionByVersionId(Long versionId);

    public Integer getMaxVersionByScriptId(Long id);

    public int getVersionCountByScriptId(Long scriptId);

    public List<AutoexecScriptVersionVo> getVersionList(AutoexecScriptVersionVo versionVo);

    public AutoexecScriptVersionVo getActiveVersionByScriptId(Long scriptId);

    public AutoexecScriptVersionVo getLatestVersionByScriptId(Long scriptId);

    public List<AutoexecScriptVersionParamVo> getParamListByVersionId(Long versionId);

    public List<AutoexecScriptLineVo> getLineListByVersionId(Long versionId);

    public int searchScriptCount(AutoexecScriptVo scriptVo);

    public List<AutoexecScriptVo> searchScript(AutoexecScriptVo scriptVo);

    public AutoexecScriptAuditVo getScriptAuditByScriptVersionIdAndOperate(@Param("versionId") Long versionId, @Param("operate") String operate);

    public String getScriptAuditDetailByHash(String hash);

    public int updateScriptBaseInfo(AutoexecScriptVo scriptVo);

    public int updateScriptVersion(AutoexecScriptVersionVo versionVo);

    public int insertScript(AutoexecScriptVo vo);

    public int insertScriptVersion(AutoexecScriptVersionVo versionVo);

    public int insertScriptVersionParamList(List<AutoexecScriptVersionParamVo> paramList);

    public int insertScriptLineContent(AutoexecScriptLineContentVo contentVo);

    public int insertScriptAudit(AutoexecScriptAuditVo auditVo);

    public int insertScriptAuditDetail(AutoexecScriptAuditContentVo auditContentVo);

    public int insertScriptLineList(List<AutoexecScriptLineVo> lineList);

    public int deleteParamByVersionId(Long versionId);

    public int deleteScriptLineByVersionId(Long versionId);

}
