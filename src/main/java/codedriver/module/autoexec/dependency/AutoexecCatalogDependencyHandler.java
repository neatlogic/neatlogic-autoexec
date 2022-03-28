package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptHasNoDraftVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptHasNoRejectedVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 自动化工具目录引用自定义工具处理器
 *
 * @author longrf
 * @date 2021/12/15 3:59 下午
 */
@Service
public class AutoexecCatalogDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    /**
     * 表名
     *
     * @return
     */
    @Override
    protected String getTableName() {
        return "autoexec_script";
    }

    /**
     * 被引用者（上游）字段
     *
     * @return
     */
    @Override
    protected String getFromField() {
        return "catalog_id";
    }

    /**
     * 引用者（下游）字段
     *
     * @return
     */
    @Override
    protected String getToField() {
        return "id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param dependencyObj 调用者值
     * @return
     */
    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            Long id = (Long) map.get("id");
            AutoexecScriptVersionVo version = null;
            Boolean hasStatus = false;
            AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
            if (scriptVo == null) {
                return null;
            }
            Long scriptId = scriptVo.getId();

            List<AutoexecScriptVersionVo> versionVoList = autoexecScriptMapper.getScriptVersionListByScriptId(scriptId);
            AutoexecScriptVersionVo versionVo = versionVoList.get(0);
            String status = versionVo.getStatus();

            if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), status)) {
                AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(scriptId);
                if (activeVersion != null) {
                    version = activeVersion;
                    hasStatus = true;
                } else {
                    throw new AutoexecScriptVersionHasNoActivedException();
                }
            } else if (hasStatus == false && Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)) {
                AutoexecScriptVersionVo recentlyDraftVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.DRAFT.getValue());
                if (recentlyDraftVersion != null) {
                    version = recentlyDraftVersion;
                    hasStatus = true;
                } else {
                    throw new AutoexecScriptHasNoDraftVersionException();
                }
            } else if (hasStatus == false && Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status)) {
                AutoexecScriptVersionVo recentlyRejectedVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.REJECTED.getValue());
                if (recentlyRejectedVersion != null) {
                    version = recentlyRejectedVersion;
                    hasStatus = true;
                } else {
                    throw new AutoexecScriptHasNoRejectedVersionException();
                }
            }
            if (scriptVo != null && StringUtils.isNotBlank(status)) {
                JSONObject dependencyInfoConfig = new JSONObject();
                dependencyInfoConfig.put("scriptId", scriptVo.getId());
                dependencyInfoConfig.put("scriptName", scriptVo.getName());
                dependencyInfoConfig.put("versionId", versionVo.getId());
                String pathFormat = "自动化工具目录-${DATA.scriptName}";
                String urlFormat = "";
                //submitted的页面不一样
                if (Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
                    urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/review-detail?versionId=${DATA.versionId}";
                } else if (version != null) {
                    dependencyInfoConfig.put("versionStatus", version.getStatus());
                    urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?scriptId=${DATA.scriptId}&status=${DATA.versionStatus}";
                }
                return new DependencyInfoVo(scriptVo.getId(), dependencyInfoConfig, pathFormat, urlFormat, this.getGroupName());
//                DependencyInfoVo dependencyInfoVo = new DependencyInfoVo();
//                dependencyInfoVo.setValue(scriptVo.getId());
//                //submitted的页面不一样
//                if (Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
//                    dependencyInfoVo.setText(String.format("<a href=\"/%s/autoexec.html#/review-detail?versionId=%s\" target=\"_blank\">%s</a>", TenantContext.get().getTenantUuid(), versionVo.getId(), scriptVo.getName()));
//                } else if (version != null) {
//                    dependencyInfoVo.setText(String.format("<a href=\"/%s/autoexec.html#/script-detail?scriptId=%s&status=%s\" target=\"_blank\">%s</a>", TenantContext.get().getTenantUuid(), scriptVo.getId(), version.getStatus(), scriptVo.getName()));
//                }
//                return dependencyInfoVo;
            }
        }
        return null;
    }

    /**
     * 被引用者（上游）类型
     *
     * @return
     */
    @Override
    public IFromType getFromType() {
        return AutoexecFromType.AUTOEXEC_CATALOG;
    }
}
