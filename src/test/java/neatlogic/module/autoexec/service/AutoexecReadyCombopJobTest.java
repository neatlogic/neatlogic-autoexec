package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.util.SpringContextUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 验证组合工具手动作业创建、启动及按持久化快照复核目标。 */
public class AutoexecReadyCombopJobTest {
  private Object previousContext;
  private final List<String> calls = new ArrayList<>();
  private final AutoexecJobActionServiceImpl service =
      new AutoexecJobActionServiceImpl() {
        @Override
        public void getJobDetailAndFireJob(AutoexecJobVo job) {
          calls.add("fire");
        }

        @Override
        protected void validateAndCreateJobFromCombop(
            AutoexecJobVo job, boolean snapshotRunnerSettings) {
          Assert.assertTrue(snapshotRunnerSettings);
          calls.add("create");
          job.setId(10L);
        }
      };

  /** 建立测试消息源，生产依赖仅由测试注入。 */
  @Before
  public void setUp() throws Exception {
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    previousContext = context.get(null);
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    StaticApplicationContext app = new StaticApplicationContext();
    app.getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
    new SpringContextUtil().setApplicationContext(app);
    inject(
        service,
        "autoexecCombopService",
        proxy(
            AutoexecCombopService.class,
            (p, m, a) -> {
              Assert.assertEquals("getExecutableActiveVersion", m.getName());
              AutoexecCombopVersionVo version = new AutoexecCombopVersionVo();
              version.setId(2L);
              return version;
            }));
    UserContext user = UserContext.init((UserContext) null);
    user.setUserUuid("operator-1");
    UserContext.init(user);
  }

  /** 恢复静态上下文，避免污染其他审计测试。 */
  @After
  public void tearDown() throws Exception {
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    context.set(null, previousContext);
    UserContext.init((UserContext) null);
  }

  /** 创建手动作业只落库，同时核对调用方确认的版本。 */
  @Test
  public void createsOnlyConfirmedManualJob() {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setOperationId(1L);
    job.setInvokeId(2L);
    job.setCombopVersionId(2L);
    job.setSource("combop");
    job.setOperationType("combop");
    job.setTriggerType("manual");
    Assert.assertEquals(Long.valueOf(10), service.createReadyCombopJob(job));
    job.setCombopVersionId(3L);
    rejected(() -> service.createReadyCombopJob(job));
    Assert.assertEquals(Collections.singletonList("create"), calls);
  }

  /** 已启动或终态作业再次提交只返回原状态，不能重跑。 */
  @Test
  public void completedJobIsNotStartedAgain() throws Exception {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setId(10L);
    job.setOperationId(1L);
    job.setSource("combop");
    job.setExecUser("operator-1");
    job.setStatus("completed");
    inject(service, "autoexecJobMapper", proxy(AutoexecJobMapper.class, (p, m, a) -> job));
    AutoexecJobVo result = service.startReadyTagentCombopJob(10L);
    Assert.assertEquals(Long.valueOf(10L), result.getId());
    Assert.assertEquals("completed", result.getStatus());
  }

  /** 从已保存快照依次复核两台主机；最后一台失败时应保持就绪，不能只启动第一台。 */
  @Test
  public void recheckAllSnapshotTargetsBeforeFiring() throws Exception {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setId(10L);
    job.setOperationId(1L);
    job.setInvokeId(2L);
    job.setSource("combop");
    job.setExecUser("operator-1");
    job.setStatus("ready");
    job.setConfigHash("config");
    job.setParamHash("params");
    AutoexecJobContentVo params =
        new AutoexecJobContentVo(
            "params", "[{\"key\":\"routeTag\",\"type\":\"text\",\"value\":\"effective-tag\"}]");
    AutoexecJobContentVo content =
        new AutoexecJobContentVo(
            "config",
            "{\"runtimeParamList\":[{\"key\":\"routeTag\",\"type\":\"text\",\"defaultValue\":\"wrong-tag\"}],"
                + "\"executeConfig\":{\"runnerGroup\":{\"mappingMode\":\"constant\",\"value\":\"pool-A\"},"
                + "\"runnerGroupTag\":{\"mappingMode\":\"runtimeparam\",\"value\":\"routeTag\"},"
                + "\"executeNodeConfig\":{\"selectNodeList\":["
                + "{\"id\":2,\"ip\":\"192.0.2.2\"},{\"id\":3,\"ip\":\"192.0.2.3\"}]}}}");
    for (boolean failLast : Arrays.asList(false, true)) {
      calls.clear();
      inject(
          service,
          "autoexecJobMapper",
          proxy(
              AutoexecJobMapper.class,
              (p, m, a) -> {
                if ("getJobContent".equals(m.getName())) {
                  if ("config".equals(a[0])) return content;
                  Assert.assertEquals("params", a[0]);
                  return params;
                }
                return job;
              }));
      inject(
          service,
          "autoexecJobService",
          proxy(
              AutoexecJobService.class,
              (p, m, a) -> {
                if ("checkSelectedTargets".equals(m.getName())) {
                  Assert.assertEquals(1L, a[0]);
                  List<?> targets = (List<?>) a[2];
                  Assert.assertEquals(2, targets.size());
                  Assert.assertEquals(Long.valueOf(2), ((AutoexecNodeVo) targets.get(0)).getId());
                  Assert.assertEquals(Long.valueOf(3), ((AutoexecNodeVo) targets.get(1)).getId());
                  calls.add("auth");
                } else {
                  Assert.assertEquals("validateTagentRoutes", m.getName());
                  AutoexecCombopVersionConfigVo effective = (AutoexecCombopVersionConfigVo) a[0];
                  Assert.assertEquals(
                      "pool-A", effective.getExecuteConfig().getRunnerGroup().getValue());
                  Assert.assertEquals(
                      "routeTag", effective.getExecuteConfig().getRunnerGroupTag().getValue());
                  Assert.assertEquals(
                      "effective-tag", effective.getRuntimeParamList().get(0).getValue());
                  for (AutoexecNodeVo n : (List<AutoexecNodeVo>) a[1]) {
                    calls.add(n.getIp());
                    if (failLast && "192.0.2.3".equals(n.getIp()))
                      throw new ParamIrregularException("Tagent runner");
                  }
                }
                return null;
              }));
      if (failLast) {
        try {
          service.startReadyTagentCombopJob(10L);
          Assert.fail("应拒绝路由失败的目标");
        } catch (
            neatlogic.framework.autoexec.exception.job.AutoexecJobTargetInvalidException expected) {
          Assert.assertEquals(Arrays.asList("auth", "192.0.2.2", "192.0.2.3"), calls);
        }
      } else {
        service.startReadyTagentCombopJob(10L);
        Assert.assertEquals(Arrays.asList("auth", "192.0.2.2", "192.0.2.3", "fire"), calls);
      }
    }
  }

  /** 即使可查看作业，也不能代替原执行人启动就绪作业。 */
  @Test
  public void otherExecutorCannotStartJob() throws Exception {
    AutoexecJobVo job = new AutoexecJobVo();
    job.setId(10L);
    job.setOperationId(1L);
    job.setSource("combop");
    job.setExecUser("other-operator");
    job.setStatus("ready");
    inject(service, "autoexecJobMapper", proxy(AutoexecJobMapper.class, (p, m, a) -> job));
    rejected(() -> service.startReadyTagentCombopJob(10L));
  }

  /** 断言约束失败，不接受 NPE 等意外异常作为通过。 */
  private static void rejected(Runnable action) {
    try {
      action.run();
      Assert.fail("应拒绝不安全配置");
    } catch (ParamIrregularException expected) {
    }
  }

  /** 测试代码通过反射注入替身，不给生产类增加装配入口。 */
  private static void inject(Object target, String name, Object value) throws Exception {
    Field field = AutoexecJobActionServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** 创建窄接口替身，任何非预期调用会由测试断言捕获。 */
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
