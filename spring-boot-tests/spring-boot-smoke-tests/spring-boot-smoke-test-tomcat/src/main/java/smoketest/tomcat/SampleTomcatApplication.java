/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.tomcat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.Objects;


@EnableAsync
@SpringBootApplication
public class SampleTomcatApplication {

	private static Log logger = LogFactory.getLog(SampleTomcatApplication.class);

	@Bean
	protected ServletContextListener listener() {
		return new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				logger.info("============> ServletContext initialized");
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				logger.info("============> ServletContext destroyed");
			}

		};
	}

	public static void main(String[] args) {

		cleanProxy();

		/**
		 * jar包是由 JarLauncher 这个类启动, 这个类可以点进去看下 它的 main
		 * 主要包括三大目录：META-INF、BOOT-INF、org(springboot启动程序)
		 *
		 * META-INF内容
		 * Main-Class是 org.springframework.boot.loader.JarLauncher ，即jar启动的Main函数
		 * Start-Class是 smoketest.tomcat.SampleTomcatApplication，即我们自己 SpringBoot 项目的启动类
		 *
		 * BOOT-INF/classes 目录：存放应用编译后的 class 文件源码,
		 * BOOT-INF/lib 目录：存放应用依赖的所有三方 jar 包文件
		 *
		 * org(springboot启动程序)
		 */

		// 是不是 jar 包启动用这个判断
		// 这是 JarLauncher 放进去的
		boolean isJar = Objects.isNull(System.getProperty("java.protocol.handler.pkgs"));
		System.out.println(isJar);


//		SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(SampleTomcatApplication.class)
//				.properties("spring.config.location=classpath:application.properties");
//		springApplicationBuilder.build().run(args);

		/**
		 * 推测应用类型和设置启动初始化器
		 * 设置初始化器和监听器
		 *
		 * 自动装配在注解上面 @SpringBootApplication
		 *
		 * 注意: spring cloud的话 , 启动的时候会调用
		 *  {@link org.springframework.cloud.bootstrap.BootstrapApplicationListener#onApplicationEvent}
		 *  public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		 *
		 *  然后web server会初始化这个
		 *  {@link org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration#onApplicationEvent}
		 */
		SpringApplication springApplication = new SpringApplication(SampleTomcatApplication.class);

		ConfigurableApplicationContext context = springApplication.run(args);

	}

	private static void cleanProxy() {
		System.clearProperty("socksProxyHost");
		System.clearProperty("socksProxyPort");

		System.clearProperty("http.proxyHost");
		System.clearProperty("http.proxyPort");

		System.clearProperty("https.proxyHost");
		System.clearProperty("https.proxyPort");

		System.clearProperty("frp.proxyHost");
		System.clearProperty("frp.proxyPort");
	}

}
