package codedriver.module.autoexec.config;

import codedriver.framework.common.RootConfiguration;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

@RootConfiguration
public class AutoexecConfig {
	private static final Logger logger = LoggerFactory.getLogger(AutoexecConfig.class);
	@NacosInjected
	private ConfigService configService;
	private static final String CONFIG_FILE = "config.properties";
	private static String PROXY_BASIC_USER_NAME;
	private static String PROXY_BASIC_PASSWORD;

	public static String PROXY_BASIC_USER_NAME() {
		return PROXY_BASIC_USER_NAME;
	}
	public static String PROXY_BASIC_PASSWORD() {
		return PROXY_BASIC_PASSWORD;
	}

	@PostConstruct
	public void init() {
		try {
			String propertiesString = configService.getConfig("config", "codedriver.framework", 3000);
			loadNacosProperties(propertiesString);
			configService.addListener("config", "codedriver.framework", new Listener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					loadNacosProperties(configInfo);
				}

				@Override
				public Executor getExecutor() {
					return null;
				}
			});
		} catch (NacosException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static void loadNacosProperties(String configInfo) {
		try {
			Properties prop = new Properties();
			if (StringUtils.isNotBlank(configInfo)) {
				prop.load(new ByteArrayInputStream(configInfo.getBytes()));
			} else {
				// 如果从nacos中读不出配置，则使用本地配置文件配置
				prop.load(new InputStreamReader(Objects.requireNonNull(AutoexecConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)), "UTF-8"));
			}

			PROXY_BASIC_USER_NAME = prop.getProperty("proxy.basic.username", "codedriver");
			PROXY_BASIC_PASSWORD = prop.getProperty("proxy.basic.password", "123456");

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static String getProperty(String configFile, String keyName, String defaultValue) {
		Properties pro = new Properties();
		try (InputStream is = AutoexecConfig.class.getClassLoader().getResourceAsStream(configFile)) {
			pro.load(is);
			String value = pro.getProperty(keyName, defaultValue);
			if (value != null) {
				value = value.trim();
			}
			return value;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static String getProperty(String configFile, String keyName) {
		return getProperty(configFile, keyName, "");
	}

}
