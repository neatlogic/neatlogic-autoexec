/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.module.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dto.AutoexecScriptVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
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
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "label", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, xss = true, desc = "名称"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "local,remote,localremote", desc = "执行方式", isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "脚本分类ID", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "操作级别ID", isRequired = true),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "参数配置"),
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
         * 没有versionId，表示首次创建脚本
         * 有versionId，表示编辑某个版本
         */
        if (scriptVo.getVersionId() == null) {
            /**
             * 鉴权，看当前用户是否有权限新增脚本
             */

            /**
             * 校验name和label
             */

            /**
             * 生成脚本与版本
             */
        } else {
            /**
             * 根据用户和版本状态，判断是否可以编辑
             */

            /**
             * 校验name和label
             */

            /**
             * 检查当前版本内容与待保存内容是否一致，据此决定是否需要保存
             */
        }
        /**
         *根据needSave判断是否需要保存脚本内容
         */
        if (needSave) {

        }
        return result;
    }

    /**
     * 校验name和label
     */


}
