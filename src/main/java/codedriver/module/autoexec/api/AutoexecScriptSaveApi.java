/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.module.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.module.autoexec.constvalue.ScriptVersionStatus;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dto.*;
import codedriver.module.autoexec.exception.AutoexecScriptNameOrLabelRepeatException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
//@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
//@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getToken() {
        return "autoexec/script/save";
    }

    @Override
    public String getName() {
        return "保存脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "label", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, xss = true, desc = "名称"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "local,remote,localremote", desc = "执行方式", isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "脚本分类ID", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "操作级别ID", isRequired = true),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "参数列表"),
            @Param(name = "parser", type = ApiParamType.STRING, desc = "脚本解析器"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, desc = "脚本内容行数据列表"),
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id"),
    })
    @Description(desc = "保存脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        boolean needSave = true;

        /**
         * 没有id和versionId，表示首次创建脚本
         * 有id没有versionId，表示新增一个版本，脚本基本信息不作修改
         * 没有Id有versionId，表示编辑某个版本，脚本基本信息不作修改
         */
        AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
        versionVo.setParser(scriptVo.getParser());
        versionVo.setLcu(UserContext.get().getUserUuid());
        versionVo.setStatus(ScriptVersionStatus.DRAFT.getValue());
        if (jsonObj.getLong("id") == null) {
            // 首次创建脚本
            /**
             * 校验脚本分类、操作级别、解析器
             */
            if (scriptVo.getVersionId() == null) {
                /**
                 * 校验name和label
                 */
                if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
                    throw new AutoexecScriptNameOrLabelRepeatException(scriptVo.getName());
                }
                if (autoexecScriptMapper.checkScriptLabelIsExists(scriptVo) > 0) {
                    throw new AutoexecScriptNameOrLabelRepeatException(scriptVo.getName());
                }

                /**
                 * 生成脚本与版本
                 */
                scriptVo.setFcu(UserContext.get().getUserUuid());
                autoexecScriptMapper.insertScript(scriptVo);
                versionVo.setScriptId(scriptVo.getId());
                versionVo.setVersion(0);
                versionVo.setIsActive(1);
                autoexecScriptMapper.insertScriptVersion(versionVo);
                scriptVo.setVersionId(versionVo.getId());
            } else {  // 编辑版本
                /**
                 * 根据用户和版本状态，判断是否可以编辑
                 */

                /**
                 * 检查当前版本内容与待保存内容是否一致，据此决定是否需要保存
                 */
            }
        } else { //新增版本

        }

        /**
         *根据needSave判断是否需要保存脚本内容和参数
         */
        if (needSave) {
            /**
             * 保存参数
             */
            List<AutoexecScriptVersionParamVo> paramList = scriptVo.getParamList();
            if (CollectionUtils.isNotEmpty(paramList)) {
                paramList.stream().forEach(o -> o.setScriptVersionId(versionVo.getId()));
            }
            autoexecScriptMapper.insertScriptVersionParamList(paramList);

            /**
             * 保存脚本内容
             */
            saveScriptLineList(scriptVo);

        }
        return result;
    }

    /**
     * 保存脚本内容行
     * @param scriptVo 脚本VO
     */
    private void saveScriptLineList(AutoexecScriptVo scriptVo) {
        if (CollectionUtils.isNotEmpty(scriptVo.getLineList())) {
            int lineNumber = 0;
            List<AutoexecScriptLineVo> lineList = new ArrayList<>();
            for (String line : scriptVo.getLineList()) {
                AutoexecScriptLineVo lineVo = new AutoexecScriptLineVo();
                lineVo.setLineNumber(++lineNumber);
                lineVo.setScriptId(scriptVo.getId());
                lineVo.setScriptVersionId(scriptVo.getVersionId());
                if (StringUtils.isNotBlank(line)) {
                    AutoexecScriptLineContentVo content = new AutoexecScriptLineContentVo(line);
                    lineVo.setContentHash(content.getHash());
                    if (autoexecScriptMapper.checkScriptLineContentHashIsExists(content.getHash()) == 0) {
                        autoexecScriptMapper.insertScriptLineContent(content);
                    }
                }
                lineList.add(lineVo);
                if (lineList.size() >= 100) {
                    autoexecScriptMapper.insertScriptLineList(lineList);
                    lineList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(lineList)) {
                autoexecScriptMapper.insertScriptLineList(lineList);
            }
        }
    }

    /**
     * 校验name和label
     */


}
