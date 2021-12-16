package codedriver.module.autoexec.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptHasNoDraftVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptHasNoRejectedVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dependency.constvalue.CalleeType;
import codedriver.framework.dependency.core.DependencyHandlerBase;
import codedriver.framework.dependency.core.ICalleeType;
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
public class AutoexecCatalogDependencyHandler extends DependencyHandlerBase {

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
     * 被调用者字段
     *
     * @return
     */
    @Override
    protected String getCalleeField() {
        return "catalog_id";
    }

    /**
     * 调用者字段
     *
     * @return
     */
    @Override
    protected String getCallerField() {
        return "id";
    }

    @Override
    protected List<String> getCallerFieldList() {
        return null;
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param caller 调用者值
     * @return
     */
    @Override
    protected ValueTextVo parse(Object caller) {
        if (caller instanceof Map) {
            Map<String, Object> map = (Map) caller;
            Long id = (Long) map.get("id");
            AutoexecScriptVersionVo version = null;
            Boolean hasStaus = false;
            AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
            if (scriptVo == null) {
                return null;//还是空的ValueTextVo
            }
            Long scriptId = scriptVo.getId();

            List<AutoexecScriptVersionVo> versionVoList = autoexecScriptMapper.getScriptVersionListByScriptId(scriptId);
            AutoexecScriptVersionVo versionVo = versionVoList.get(0);
            String status = versionVo.getStatus();

            if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), status)) {
                AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(scriptId);
                if (activeVersion != null) {
                    version = activeVersion;
                    hasStaus = true;
                } else {
                    throw new AutoexecScriptVersionHasNoActivedException();
                }
            } else if (hasStaus == false && Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)) {
                AutoexecScriptVersionVo recentlyDraftVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.DRAFT.getValue());
                if (recentlyDraftVersion != null) {
                    version = recentlyDraftVersion;
                    hasStaus = true;
                } else {
                    throw new AutoexecScriptHasNoDraftVersionException();
                }
            } else if (hasStaus == false && Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status)) {
                AutoexecScriptVersionVo recentlyRejectedVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.REJECTED.getValue());
                if (recentlyRejectedVersion != null) {
                    version = recentlyRejectedVersion;
                    hasStaus = true;
                } else {
                    throw new AutoexecScriptHasNoRejectedVersionException();
                }
            }
            if (scriptVo != null && StringUtils.isNotBlank(status)) {
                ValueTextVo valueTextVo = new ValueTextVo();
                valueTextVo.setValue(scriptVo.getId());
                //submitted的页面不一样
                if (Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
                    valueTextVo.setText(String.format("<a href=\"/%s/autoexec.html#/review-detail?versionId=%s\" target=\"_blank\">%s</a>", TenantContext.get().getTenantUuid(), versionVo.getId(), scriptVo.getName()));
                } else {
                    valueTextVo.setText(String.format("<a href=\"/%s/autoexec.html#/script-detail?scriptId=%s&status=%s\" target=\"_blank\">%s</a>", TenantContext.get().getTenantUuid(), scriptVo.getId(), version.getStatus(), scriptVo.getName()));
                }
                return valueTextVo;
            }
        }
        return null;
    }

    /**
     * 被调用方法名
     *
     * @return
     */
    @Override
    public ICalleeType getCalleeType() {
        return CalleeType.AUTOEXEC_CATALOG;
    }
}
