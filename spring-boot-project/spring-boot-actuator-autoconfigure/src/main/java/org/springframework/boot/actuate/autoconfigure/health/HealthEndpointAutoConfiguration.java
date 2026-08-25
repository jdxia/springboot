/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
/**
 * 这里的 available 不是简单的“类存在”，而是：
 * available = enabled && 至少通过某种技术暴露
 * 看 {@link OnAvailableEndpointCondition#getMatchOutcome(Environment, MergedAnnotation, MergedAnnotation)}
 */
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@EnableConfigurationProperties(HealthEndpointProperties.class)
// 加载了HealthEndpointConfiguration、ReactiveHealthEndpointConfiguration、HealthEndpointWebExtensionConfiguration、HealthEndpointReactiveWebExtensionConfiguration这几个类
// 挨个看
@Import({ HealthEndpointConfiguration.class, ReactiveHealthEndpointConfiguration.class,
		HealthEndpointWebExtensionConfiguration.class, HealthEndpointReactiveWebExtensionConfiguration.class })
public class HealthEndpointAutoConfiguration {

	/**
	 业务能力层
	 HealthEndpoint、MetricsEndpoint、LoggersEndpoint
	 |
	 v
	 端点发现与调用层
	 EndpointDiscoverer、WebOperation、OperationInvoker
	 |
	 v
	 技术暴露层
	 WebMvcEndpointHandlerMapping
	 WebFluxEndpointHandlerMapping
	 JmxEndpointExporter

	 */

}
