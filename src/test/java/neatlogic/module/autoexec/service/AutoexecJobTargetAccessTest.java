package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.ICrossoverService;
import neatlogic.framework.exception.core.ApiRuntimeException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 验证节点选择结果只能引用授权查询返回的同一资源与地址。 */
public class AutoexecJobTargetAccessTest {
  private final AutoexecJobServiceImpl service =
      new AutoexecJobServiceImpl() {
        @Override
        public ResourceSearchVo getResourceSearchVoWithCmdbGroupType(
            AutoexecJobVo job, JSONObject filter, JSONObject condition) {
          Assert.assertEquals(Long.valueOf(19L), job.getOperationId());
          Assert.assertSame(preCondition, condition);
          return search;
        }
      };
  private final JSONObject preCondition = new JSONObject();
  private final ResourceSearchVo search = new ResourceSearchVo();
  private final ResourceVo resource = new ResourceVo();
  private Map<Class<? extends ICrossoverService>, ICrossoverService> registry;
  private ICrossoverService previous;
  private List<Long> currentResourceIds = Collections.singletonList(12L);

  /** 授权资源查询仅允许节点 12，配置范围通过实际作业查询契约传递。 */
  @Before
  @SuppressWarnings("unchecked")
  public void setup() throws Exception {
    search.setIsHasAuth(false);
    search.setCmdbGroupType("autoexec");
    search.setTypeIdList(Collections.singletonList(8L));
    resource.setId(12L);
    resource.setName("cmdb-host");
    resource.setIp("192.0.2.12");
    resource.setPort(4119);
    IResourceCenterResourceCrossoverService resources =
        (IResourceCenterResourceCrossoverService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {IResourceCenterResourceCrossoverService.class},
                (proxy, method, args) -> {
                  if ("getResourceIdList".equals(method.getName())) {
                    if (search.getInputNodeList() != null) {
                      Assert.assertEquals(
                          resource.getIp(), search.getInputNodeList().get(0).getIp());
                      Assert.assertNull(search.getInputNodeList().get(0).getPort());
                      return currentResourceIds;
                    }
                    Assert.assertSame(search, args[0]);
                    return search.getIdList().stream()
                        .filter(resource.getId()::equals)
                        .collect(Collectors.toList());
                  }
                  Assert.assertEquals("getResourceListByIdList", method.getName());
                  Assert.assertEquals(Collections.singletonList(resource.getId()), args[0]);
                  return Collections.singletonList(resource);
                });
    Field crossovers = CrossoverServiceFactory.class.getDeclaredField("apiMap");
    crossovers.setAccessible(true);
    registry = (Map<Class<? extends ICrossoverService>, ICrossoverService>) crossovers.get(null);
    previous = registry.put(IResourceCenterResourceCrossoverService.class, resources);
  }

  /** 恢复其它测试可能使用的资源查询组件。 */
  @After
  public void cleanup() {
    registry.remove(IResourceCenterResourceCrossoverService.class);
    if (previous != null) registry.put(IResourceCenterResourceCrossoverService.class, previous);
  }

  /** 忽略前端名称，使用已授权资源的名称与端口回填。 */
  @Test
  public void usesCanonicalResourceFields() {
    AutoexecNodeVo node = new AutoexecNodeVo(resource.getIp());
    node.setId(resource.getId());
    node.setName("old-name");
    service.checkSelectedTargets(19L, preCondition, Collections.singletonList(node));
    Assert.assertEquals(resource.getName(), node.getName());
    Assert.assertEquals(resource.getPort(), node.getPort());
  }

  /** 混合当前节点与资源选择时，沿用原生输入节点的 IP 和空端口语义。 */
  @Test
  public void resolvesCurrentHostInSameResourceScope() {
    resource.setPort(null);
    List<AutoexecNodeVo> nodes = service.resolveInputTargets(19L, preCondition, resource.getIp());
    Assert.assertEquals(1, nodes.size());
    Assert.assertEquals(resource.getId(), nodes.get(0).getId());
  }

  /** 同一来源 IP 匹配多个资源时拒绝把它们全部加入作业。 */
  @Test(expected = ApiRuntimeException.class)
  public void rejectsAmbiguousCurrentHost() {
    currentResourceIds = Arrays.asList(12L, 13L);
    service.resolveInputTargets(19L, preCondition, resource.getIp());
  }

  /** 显式选择的每个目标都必须授权，并与提交的资源地址匹配。 */
  @Test
  public void rejectsUnauthorizedOrMismatchedNodes() {
    AutoexecNodeVo valid = new AutoexecNodeVo(resource);
    AutoexecNodeVo unauthorized = new AutoexecNodeVo(resource);
    unauthorized.setId(99L);
    AutoexecNodeVo wrongAddress = new AutoexecNodeVo(resource);
    wrongAddress.setIp("192.0.2.50");
    AutoexecNodeVo wrongPort = new AutoexecNodeVo(resource);
    wrongPort.setPort(999);
    for (AutoexecNodeVo denied :
        Arrays.asList(
            unauthorized, wrongAddress, wrongPort, new AutoexecNodeVo(resource.getIp()))) {
      List<AutoexecNodeVo> selected = Arrays.asList(valid, denied);
      try {
        service.checkSelectedTargets(19L, preCondition, selected);
        Assert.fail("应拒绝无权、缺少资源标识或地址不匹配的目标");
      } catch (ApiRuntimeException expected) {
        Assert.assertNotNull(expected.getMessage());
      }
    }
  }
}
