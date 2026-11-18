package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.ICrossoverService;
import neatlogic.framework.crossover.IFileCrossoverService;
import neatlogic.framework.util.SpringContextUtil;
import neatlogic.module.autoexec.script.paramtype.*;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** 真实作业参数类型转换矩阵，不访问数据库或执行 Runner。 */
public class AutoexecRuntimeParamConversionTest {
  private Map<String, IScriptParamType> registry;
  private Map<String, IScriptParamType> oldRegistry;
  private Map<Class<? extends ICrossoverService>, ICrossoverService> crossovers;
  private Map<Class<? extends ICrossoverService>, ICrossoverService> oldCrossovers;
  private Object oldContext;

  /** 注册全部真实参数类型，并注入只读账户和文件替身。 */
  @Before
  @SuppressWarnings("unchecked")
  public void setup() throws Exception {
    Field ctx = SpringContextUtil.class.getDeclaredField("ctx");
    ctx.setAccessible(true);
    oldContext = ctx.get(null);
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    StaticApplicationContext app = new StaticApplicationContext();
    app.getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
    new SpringContextUtil().setApplicationContext(app);
    Field types = ScriptParamTypeFactory.class.getDeclaredField("scriptParamTypeMap");
    types.setAccessible(true);
    registry = (Map<String, IScriptParamType>) types.get(null);
    oldRegistry = new HashMap<>(registry);
    registry.clear();
    for (IScriptParamType handler :
        Arrays.asList(
            new ScriptParamTypeText(),
            new ScriptParamTypePassword(),
            new ScriptParamTypeFile(),
            new ScriptParamTypeDate(),
            new ScriptParamTypeDatetime(),
            new ScriptParamTypeTime(),
            new ScriptParamTypeJson(),
            new ScriptParamTypeSelect(),
            new ScriptParamTypeMultiSelect(),
            new ScriptParamTypeRadio(),
            new ScriptParamTypeCheckbox(),
            new ScriptParamTypeNode(),
            new ScriptParamTypeAccount(),
            new ScriptParamTypeUserSelect(),
            new ScriptParamTypeTextarea(),
            new ScriptParamTypePhase(),
            new ScriptParamTypeSwitch(),
            new ScriptParamTypeFilePath(),
            new ScriptParamTypeRunnerGroup(),
            new ScriptParamTypeRunnerGroupTag())) {
      registry.put(handler.getType(), handler);
    }
    Field apis = CrossoverServiceFactory.class.getDeclaredField("apiMap");
    apis.setAccessible(true);
    crossovers = (Map<Class<? extends ICrossoverService>, ICrossoverService>) apis.get(null);
    oldCrossovers = new HashMap<>(crossovers);
    crossovers.clear();
    crossovers.put(
        IFileCrossoverService.class, proxy(IFileCrossoverService.class, (p, m, a) -> null));
    crossovers.put(
        IResourceAccountCrossoverMapper.class,
        proxy(
            IResourceAccountCrossoverMapper.class,
            (p, m, a) -> {
              if (m.getName().equals("getAccountById")) {
                AccountVo account = new AccountVo();
                account.setId((Long) a[0]);
                return account;
              }
              return null;
            }));
  }

  /** 恢复注册表和消息上下文，避免影响其它作业参数测试。 */
  @After
  public void cleanup() throws Exception {
    registry.clear();
    registry.putAll(oldRegistry);
    crossovers.clear();
    crossovers.putAll(oldCrossovers);
    Field ctx = SpringContextUtil.class.getDeclaredField("ctx");
    ctx.setAccessible(true);
    ctx.set(null, oldContext);
  }

  /** 所有原生注册类型均用真实转换器核对约定的输入、输出形态。 */
  @Test
  public void checkNativeTypes() {
    Assert.assertEquals(ParamType.values().length, registry.size());
    for (ParamType type : ParamType.values()) {
      Object value = sample(type.getValue());
      JSONArray input = new JSONArray();
      if (value instanceof JSONArray && type != ParamType.JSON) input.addAll((JSONArray) value);
      else input.add(value);
      Object result = registry.get(type.getValue()).convertDataForProcessComponent(input);
      // 账号经查询转为数值编号；其他样例已经是编辑器原生格式。
      Object expected = value;
      if (type == ParamType.ACCOUNT) expected = Long.valueOf(value.toString());
      Assert.assertEquals(type.getValue(), JSON.toJSONString(expected), JSON.toJSONString(result));
    }
  }

  /** 每种输入样例使用原生编辑器/流程已经建立的值结构。 */
  private Object sample(String type) {
    if (Arrays.asList("multiselect", "checkbox", "runnergrouptag").contains(type)) {
      return JSON.parseArray("[\"a\",\"b\"]");
    }
    if ("node".equals(type)) {
      return JSON.parseArray("[{\"ip\":\"192.0.2.1\"}]");
    }
    if ("userselect".equals(type)) {
      return JSON.parseArray("[\"user#01234567890123456789012345678901\"]");
    }
    if ("file".equals(type)) {
      return JSON.parseObject(
          "{\"fileIdList\":[\"1\"],\"fileList\":[{\"id\":\"1\",\"name\":\"test\"}]}");
    }
    if ("json".equals(type)) {
      return JSON.parseObject("{\"test\":1}");
    }
    if ("switch".equals(type)) {
      return Boolean.TRUE;
    }
    if ("account".equals(type)) {
      return "12";
    }
    if ("runnergroup".equals(type)) {
      return "1";
    }
    return "value";
  }

  /** 创建只提供当前测试需要行为的接口替身。 */
  private <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
