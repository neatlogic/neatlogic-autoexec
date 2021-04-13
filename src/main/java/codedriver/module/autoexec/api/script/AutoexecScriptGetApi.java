/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotAnyVersionException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserAuthVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.AutoexecScriptVo;
import codedriver.module.autoexec.operate.ScriptOperateBuilder;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "autoexec/script/get";
    }

    @Override
    public String getName() {
        return "查看脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID，表示不指定版本查看，两个参数二选一"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID，表示指定版本查看"),
    })
    @Output({
            @Param(name = "script", explode = AutoexecScriptVo[].class, desc = "脚本内容"),
            @Param(name = "operateList", explode = ValueTextVo[].class, desc = "按钮列表"),
    })
    @Description(desc = "查看脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVo script = null;
        AutoexecScriptVersionVo version = null;
        List<ValueTextVo> operateList = null;
        Long id = jsonObj.getLong("id");
        Long versionId = jsonObj.getLong("versionId");
        if (id != null) { // 不指定版本
            if (autoexecScriptMapper.checkScriptIsExistsById(id) == 0) {
                throw new AutoexecScriptNotFoundException(id);
            }
            script = autoexecScriptMapper.getScriptBaseInfoById(id);
            AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(id);
            if (activeVersion != null) { // 有激活版本
                version = activeVersion;
            } else { // 没有激活版本，拿最新的版本
                AutoexecScriptVersionVo latestVersion = autoexecScriptMapper.getLatestVersionByScriptId(id);
                if (latestVersion == null) {
                    throw new AutoexecScriptNotAnyVersionException();
                }
                version = latestVersion;
            }
        } else if (versionId != null) { // 指定查看某个版本
            AutoexecScriptVersionVo currentVersion = autoexecScriptMapper.getVersionByVersionId(versionId);
            if (currentVersion == null) {
                throw new AutoexecScriptVersionNotFoundException(versionId);
            }
            version = currentVersion;
            script = autoexecScriptMapper.getScriptBaseInfoById(currentVersion.getScriptId());
            if (script == null) {
                throw new AutoexecScriptNotFoundException(currentVersion.getScriptId());
            }
        }
        if (script != null) {
            script.setVersionVo(version);
            version.setParamList(autoexecScriptMapper.getParamListByVersionId(version.getId()));
            version.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
            // todo 如果是已驳回状态，要查询驳回原因
            // todo 关联的流水线
            // 获取操作按钮
            List<UserAuthVo> authList = userMapper.searchUserAllAuthByUserAuth(new UserAuthVo(UserContext.get().getUserUuid()));
            if (CollectionUtils.isNotEmpty(authList)) {
                ScriptOperateBuilder builder = new ScriptOperateBuilder(authList.stream()
                        .map(UserAuthVo::getAuth)
                        .collect(Collectors.toList()), version.getStatus());
                builder.setDelete().setCopy().setCompare().setTest().setValidate().setSave().setSubmit().setPass().setReject();
                operateList = builder.build();
            }
        }
        result.put("script", script);
        result.put("operateList", operateList);
        return result;
    }


}
