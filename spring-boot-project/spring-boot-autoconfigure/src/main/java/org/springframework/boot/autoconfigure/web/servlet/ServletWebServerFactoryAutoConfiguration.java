/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.WebListenerRegistrar;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for servlet web servers.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
//在自动配置中具有最高优先级执行
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class) // 项目里面要有这个类
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
// 导入了一些类, 但是具体能不能导入还要看这些类的 condition 条件注解
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
		ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
		ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
		ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {

	@Bean
	public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,
			ObjectProvider<WebListenerRegistrar> webListenerRegistrars,
			ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers) {
		return new ServletWebServerFactoryCustomizer(serverProperties,
				webListenerRegistrars.orderedStream().collect(Collectors.toList()),
				cookieSameSiteSuppliers.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat") // 项目里面有tomcat, 这个生效
	public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
			ServerProperties serverProperties) {
		return new TomcatServletWebServerFactoryCustomizer(serverProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
	@ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
	static class ForwardedHeaderFilterConfiguration {

		@Bean
		@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat") // 项目里面有tomcat, 这个生效
		ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
			return (filter) -> filter.setRelativeRedirects(serverProperties.getTomcat().isUseRelativeRedirects());
		}

		@Bean
		FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
				ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider) {
			ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
			customizerProvider.ifAvailable((customizer) -> customizer.customize(filter));
			FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return registration;
		}

	}

	interface ForwardedHeaderFilterCustomizer {

		void customize(ForwardedHeaderFilter filter);

	}

	/**
	 * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
	 * {@link ImportBeanDefinitionRegistrar} for early registration.
	 */
	public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
			}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			// 编程式注入组件
			registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",
					WebServerFactoryCustomizerBeanPostProcessor.class,
					WebServerFactoryCustomizerBeanPostProcessor::new);
			registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class, ErrorPageRegistrarBeanPostProcessor::new);
		}

		private <T> void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry, String name,
				Class<T> beanClass, Supplier<T> instanceSupplier) {
			if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, instanceSupplier);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
