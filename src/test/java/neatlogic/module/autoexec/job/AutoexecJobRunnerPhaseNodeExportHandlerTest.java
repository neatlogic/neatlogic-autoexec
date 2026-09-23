package neatlogic.module.autoexec.job;

import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.INodeDetail;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.dto.runner.RunnerVo;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 验证 Runner 阶段使用本地资源标识读取并写入输出参数。 */
public class AutoexecJobRunnerPhaseNodeExportHandlerTest {

    /** Runner 节点应映射到 resourceId=0，并把对应输出写入 Excel 行数据。 */
    @Test
    public void runnerUsesLocalResourceOutput() {
        AutoexecJobPhaseNodeVo runnerNode = new AutoexecJobPhaseNodeVo();
        runnerNode.setId(11L);
        runnerNode.setRunnerId(22L);
        RunnerVo runner = new RunnerVo();
        runner.setId(22L);
        runner.setName("runner-name");
        runner.setHost("127.0.0.1");
        runner.setPort(8080);

        AutoexecJobMapper mapper = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AutoexecJobMapper.class}, (proxy, method, args) -> {
            if ("getJobPhaseRunnerNodeByJobIdAndPhaseId".equals(method.getName())) {
                return runnerNode;
            }
            if ("getJobRunnerById".equals(method.getName())) {
                return runner;
            }
            return null;
        });
        AutoexecJobRunnerPhaseNodeExportHandler handler = new AutoexecJobRunnerPhaseNodeExportHandler();
        handler.autoexecJobMapper = mapper;

        List<? extends INodeDetail> nodeList = handler.searchJobPhaseNode(new AutoexecJobPhaseNodeVo(1L, 2L), "test");
        Assert.assertEquals(Long.valueOf(0L), nodeList.get(0).getResourceId());

        Map<Long, Map<String, Object>> nodeDataMap = new LinkedHashMap<>();
        handler.assembleData(new AutoexecJobVo(), new AutoexecJobPhaseVo(), nodeList, nodeDataMap, new HashMap<>(), new HashMap<>(), Collections.singletonMap(0L, "{\"outtext\":\"value\"};"));

        Assert.assertEquals("{\"outtext\":\"value\"};", nodeDataMap.get(11L).get("outputParam"));
        Assert.assertEquals("127.0.0.1:8080", nodeDataMap.get(11L).get("runner"));
    }
}
