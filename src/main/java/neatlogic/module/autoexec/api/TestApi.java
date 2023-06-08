package neatlogic.module.autoexec.api;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthBase;
import neatlogic.framework.auth.core.AuthFactory;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Service

@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper jobMapper;

    @Override
    public String getToken() {
        return "autoexec/test";
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "test")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Map<String, List<AuthBase>> stringListMap = AuthFactory.getAuthGroupMap();
        stringListMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(key -> {
                    String group = key.getKey();
                    List<AuthBase> authBaseList = key.getValue();
                    authBaseList = authBaseList.stream().sorted(Comparator.comparing(o -> o.getClass().getSimpleName())).collect(Collectors.toList());
                    for (AuthBase authBase : authBaseList) {
                        //System.out.println("auth." + group + "." + authBase.getClass().getSimpleName().toLowerCase(Locale.ROOT).replace("_", "") + ".name = " + authBase.getAuthDisplayName());
                        //System.out.println("auth." + group + "." + authBase.getClass().getSimpleName().toLowerCase(Locale.ROOT).replace("_", "") + ".introduction = " + authBase.getAuthIntroduction());
                    }
                    //System.out.println("");
                }
        );

        Reflections reflections = new Reflections("neatlogic");
        Set<Class<? extends ApiRuntimeException>> exceptionClass = reflections.getSubTypesOf(ApiRuntimeException.class);
        /*exceptionClass.stream().forEach(key->{
            System.out.println(Arrays.asList("autoexec","change","cmdb","dashboard","deploy","event","inspect","knowledge","pbc","process","rdm","report","tagent","tenant").contains(key.getName().split("\\.")[2])?key.getName().split("\\.")[2]+key.getName().split("\\.")[key.getName().split("\\.").length-1]:"framework"+key.getName().split("\\.")[key.getName().split("\\.").length-1]);
        });*/
        exceptionClass = exceptionClass.stream().sorted(Comparator.comparing(ex -> Arrays.asList("autoexec", "change", "cmdb", "dashboard", "deploy", "event", "inspect", "knowledge", "pbc", "process", "rdm", "report", "tagent", "tenant").contains(ex.getName().split("\\.")[2]) ? ex.getName().split("\\.")[2] + ex.getName().split("\\.")[ex.getName().split("\\.").length - 1] : "framework" + ex.getName().split("\\.")[ex.getName().split("\\.").length - 1])).collect(Collectors.toCollection(LinkedHashSet::new));
        for (Class<? extends ApiRuntimeException> ex : exceptionClass) {
            String exName = ex.getName();
            String exSimpleName = ex.getSimpleName();
            String group = Arrays.asList("autoexec", "change", "cmdb", "dashboard", "deploy", "event", "inspect", "knowledge", "pbc", "process", "rdm", "report", "tagent", "tenant").contains(ex.getName().split("\\.")[2]) ? ex.getName().split("\\.")[2] : "framework";
            //System.out.println("exception." + group + "." + ex.getSimpleName().toLowerCase(Locale.ROOT) +" = ");
            Constructor[] constructs = ex.getConstructors();
            for (Constructor construct : constructs) {
                Object[] objects = new Object[construct.getParameterTypes().length];
                for (int i = 0; i < construct.getParameterTypes().length; i++) {
                    if (construct.getParameterTypes()[i] == String.class) {
                        objects[i] = "{" + i + "}";
                    } else if (construct.getParameterTypes()[i] == Integer.class || construct.getParameterTypes()[i] == int.class) {
                        if (i == 0) {
                            objects[i] = 9999;
                        } else {
                            objects[i] = -i;
                        }
                    } else if (construct.getParameterTypes()[i] == Long.class || construct.getParameterTypes()[i] == long.class) {
                        if (i == 0) {
                            objects[i] = 8888L;
                        } else {
                            objects[i] = -Long.valueOf(i);
                        }
                    } else {
                        objects[i] = "{" + i + "}";
                    }

                }
                try {

                    Exception exception = ex.getConstructor(construct.getParameterTypes()).newInstance(objects);
                    String message = exception.getMessage();
                    for (int i = 0; i < construct.getParameterTypes().length; i++) {
                        message = message.replaceAll("-" + i, "{" + i + "}");
                        message = message.replaceAll("9999", "{0}");
                        message = message.replaceAll("8888", "{0}");
                    }
                    //System.out.println("exception." + group + "." + ex.getSimpleName().toLowerCase(Locale.ROOT) +" = "+message);
                } catch (Exception ex1) {
                    //System.out.println("exception." + group + "." + ex.getSimpleName().toLowerCase(Locale.ROOT) +" = ");
                }

            }

        }


        //ENUM
        Set<Class<?>> classes = getClasses("neatlogic");
        Map<String, String> enumMap = new LinkedHashMap<>();
        for (Class<?> enumTmp : classes) {
            if(!(enumTmp.isEnum())){
                continue;
            }
            Object[] objects = enumTmp.getEnumConstants();
            String group = Arrays.asList("autoexec", "change", "cmdb", "dashboard", "deploy", "event", "inspect", "knowledge", "pbc", "process", "rdm", "report", "tagent", "tenant").contains(enumTmp.getName().split("\\.")[2]) ? enumTmp.getName().split("\\.")[2] : "framework";
            Object instance = null;
            if (objects != null && objects.length > 0) {
                for (Object ob : objects) {
                    try {
                        enumMap.put("enum." + group + "." +enumTmp.getSimpleName().toLowerCase(Locale.ROOT)+"."+ enumTmp.getMethod("toString").invoke(ob).toString().toLowerCase(Locale.ROOT), enumTmp.getMethod("getText").invoke(ob).toString());
                        //System.out.println("enum."+group + "." + enumTmp.getMethod("toString").invoke(ob).toString().toLowerCase(Locale.ROOT)+"="+enumTmp.getMethod("getText").invoke(ob));
                    } catch (Exception ignored) {
                        enumMap.put("enum." + group + "." +enumTmp.getSimpleName().toLowerCase(Locale.ROOT)+"."+ enumTmp.getMethod("toString").invoke(ob).toString().toLowerCase(Locale.ROOT), StringUtils.EMPTY);
                        //System.out.println(enumTmp.getName());
                    }
                }
            } else {
                instance = enumTmp.newInstance();
            }
        }


        enumMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(System.out::println);

        return $.t("用户管理权限");
    }


    /**
     * 从指定的package中获取所有的Class
     *
     * @param packageName
     * @return
     */
    private Set<Class<?>> getClasses(String packageName) {

        // 第一个class类的集合
        Set<Class<?>> classes = new HashSet<>();
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    classes.addAll(findClassByDirectory(packageName, filePath));
                }
                else if ("jar".equals(protocol)) {
                    classes.addAll(findClassInJar(packageName, url));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     */
    private List<Class<?>> findClassByDirectory(String packageName, String packagePath) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>(0);
        }

        File[] dirs = dir.listFiles();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        // 循环所有文件
        for (File file : dirs) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                classes.addAll(findClassByDirectory(packageName + "." + file.getName(),
                        file.getAbsolutePath()));
            }
            else if (file.getName().endsWith(".class")) {
                // 如果是java类文件，去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return classes;
    }

    private List<Class<?>> findClassInJar(String packageName, URL url) {

        List<Class<?>> classes = new ArrayList<Class<?>>();

        String packageDirName = packageName.replace('.', '/');
        // 定义一个JarFile
        JarFile jar;
        try {
            // 获取jar
            jar = ((JarURLConnection) url.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (name.charAt(0) == '/') {
                    // 获取后面的字符串
                    name = name.substring(1);
                }

                // 如果前半部分和定义的包名相同
                if (name.startsWith(packageDirName) && name.endsWith(".class")) {
                    // 去掉后面的".class"
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        // 添加到classes
                        classes.add(Class.forName(className));
                    }
                    catch (ClassNotFoundException e ) {
                        e.printStackTrace();
                    }catch (NoClassDefFoundError ex ){

                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }


}

