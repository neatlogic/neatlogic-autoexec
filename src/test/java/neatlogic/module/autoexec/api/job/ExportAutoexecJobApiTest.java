package neatlogic.module.autoexec.api.job;

import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.job.AutoexecJobOutputParamExportConfig;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 验证作业导出范围和密码字段识别使用作业执行快照。 */
public class ExportAutoexecJobApiTest {

    /** 无报告字段配置时包含当前阶段全部操作，并识别工具和脚本密码输出。 */
    @Test
    public void fallbackIncludesAllOperationsAndFindsPasswordTypes() {
        ExportAutoexecJobApi api = api();

        Map<String, AutoexecJobOutputParamExportConfig> result = api.getOutputParamConfigMap(1L, 2L, null);

        Assert.assertTrue(result.get("tool_11").isIncluded("outtext"));
        Assert.assertTrue(result.get("tool_11").isPassword("toolPassword"));
        Assert.assertTrue(result.get("script_12").isIncluded("dynamicOutput"));
        Assert.assertTrue(result.get("script_12").isPassword("scriptPassword"));
    }

    /** 显式配置只保留选中的操作和字段，但不取消密码类型标记。 */
    @Test
    public void configuredFieldsOverrideFallback() {
        ExportAutoexecJobApi api = api();
        Map<String, List<String>> configuredMap = new HashMap<>();
        configuredMap.put("tool_11", Arrays.asList("outtext", "toolPassword"));

        Map<String, AutoexecJobOutputParamExportConfig> result = api.getOutputParamConfigMap(1L, 2L, configuredMap);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get("tool_11").isIncluded("outtext"));
        Assert.assertTrue(result.get("tool_11").isPassword("toolPassword"));
        Assert.assertFalse(result.get("tool_11").isIncluded("other"));
    }

    /** 注入仅返回作业快照的只读 Mapper 替身。 */
    private ExportAutoexecJobApi api() {
        String toolParamContent = "{\"outputParamList\":[{\"key\":\"outtext\",\"type\":\"text\"},{\"key\":\"toolPassword\",\"type\":\"password\"}]}";
        AutoexecJobPhaseOperationVo toolOperation = operation(11L, "tool", CombopOperationType.TOOL.getValue());
        toolOperation.setParamStr(toolParamContent);
        AutoexecJobPhaseOperationVo scriptOperation = operation(12L, "script", CombopOperationType.SCRIPT.getValue());
        scriptOperation.setVersionId(100L);

        AutoexecJobMapper jobMapper = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobMapper.class}, (proxy, method, args) -> {
            if ("getJobPhaseOperationListByJobIdAndPhaseId".equals(method.getName())) {
                return Arrays.asList(toolOperation, scriptOperation);
            }
            if ("getJobContentList".equals(method.getName())) {
                return Collections.singletonList(new AutoexecJobContentVo(toolOperation.getParamHash(), toolParamContent));
            }
            return null;
        });
        AutoexecScriptMapper scriptMapper = (AutoexecScriptMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecScriptMapper.class}, (proxy, method, args) -> {
            if ("getOutputParamListByVersionId".equals(method.getName())) {
                AutoexecScriptVersionParamVo textParam = new AutoexecScriptVersionParamVo();
                textParam.setKey("outtext");
                textParam.setType("text");
                AutoexecScriptVersionParamVo passwordParam = new AutoexecScriptVersionParamVo();
                passwordParam.setKey("scriptPassword");
                passwordParam.setType("password");
                return Arrays.asList(textParam, passwordParam);
            }
            return null;
        });
        ExportAutoexecJobApi api = new ExportAutoexecJobApi();
        api.autoexecJobMapper = jobMapper;
        api.autoexecScriptMapper = scriptMapper;
        return api;
    }

    /** 构造阶段操作快照。 */
    private AutoexecJobPhaseOperationVo operation(Long id, String name, String type) {
        AutoexecJobPhaseOperationVo operationVo = new AutoexecJobPhaseOperationVo();
        operationVo.setId(id);
        operationVo.setName(name);
        operationVo.setType(type);
        return operationVo;
    }
}
