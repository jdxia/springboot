/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
@Configuration(proxyBeanMethods = false)
/**
 * 必须同时存在java.sql.DataSource接口 和
 * org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType类(spring-jdbc包中的类)，
 * 此配置类才会生效
 */
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@AutoConfigureBefore(SqlInitializationAutoConfiguration.class)
// 确保前缀为 spring.datasource 的配置属性项被加载到 bean DataSourceProperties
@EnableConfigurationProperties(DataSourceProperties.class)
/**
 * DataSourcePoolMetadataProvidersConfiguration: 为了配置数据源对应的DataSourcePoolMetadataProvider组件实例
 * DataSourceInitializationConfiguration: 用于执行一些数据库初始化脚本(执行sql),如DML,DDL
 */
@Import({ DataSourcePoolMetadataProvidersConfiguration.class,
		DataSourceInitializationConfiguration.InitializationSpecificCredentialsDataSourceInitializationConfiguration.class,
		DataSourceInitializationConfiguration.SharedCredentialsDataSourceInitializationConfiguration.class })
public class DataSourceAutoConfiguration {

	// 内嵌配置类
	@Configuration(proxyBeanMethods = false)
	// 仅在嵌入式数据库被使用时才生效
	// 嵌入式数据库这里指的是 h2, derby, 或者 hsql
	/**
	 * 须满足内置数据源的条件
	 * 1. 用户没有指定spring.datasource.type的配置
	 * 2. 项目中存在com.zaxxer.hikari.HikariDataSource、
	 * 		org.apache.tomcat.jdbc.pool.DataSource、
	 * 		org.apache.commons.dbcp2.BasicDataSource
	 * 		中其中任何一个类时(只要导入相关依赖即可)
	 * 		注意: 当引入了spring-boot-starter-jdbc时,会传递依赖	HikariCP,
	 * 		此时HikariDataSource会作为默认的数据源实现
	 */
	@Conditional(EmbeddedDatabaseCondition.class)
	// 用户没有配置数据源实例才会生效(优先用户配置，意思是：如果用户在项目中定义了数据源组件，那这个配置就不生效了)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	/**
	 * 当以上条件都满足了，下面导入的这个配置类才会起作用
	 * 导入的这个配置类，仅配置了EmbeddedDatabase（它继承了DataSource接口，作为内嵌数据源）
	 * 默认会依次尝试加载枚举类EmbeddedDatabaseConnection中定义的H2、DERBY、HSQL内嵌数据库的驱动类
	 * 第一个加载成功的，将会创建对应的内嵌数据源
	 */
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedDatabaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	/**
	 * 池化数据源生效条件
	 * 1. 用户精确的指定了spring.datasource.type的配置
	 * 2. 用户导入了com.zaxxer.hikari.HikariDataSource、
	 * 		org.apache.tomcat.jdbc.pool.DataSource、
	 * 		org.apache.commons.dbcp2.BasicDataSource
	 * 		其中任何一个依赖(这些类是以常量的方式定义在了DataSourceBuilder类中)
	 * 	  注意: 当引入了spring-boot-starter-jdbc时,会传递依赖	HikariCP,
	 */
	@Conditional(PooledDataSourceCondition.class)
	// 仅在没有类型为 DataSource/XADataSource 的 bean 定义时才生效
	// 用户没有配置数据源实例才会生效(优先用户配置，意思是：如果用户在项目中定义了数据源组件，那这个配置就不生效了)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	// 导入针对不同数据库类型数据源连接组件的数据源配置，这些配置仅在使用了相应的数据源连接
	// 组件时才生效，一般开发人员只使用其中一种，所以也就只会有一个生效。
	// 这些配置的目的都是为了定义一个 数据源 bean dataSource
	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.OracleUcp.class,
			DataSourceConfiguration.Generic.class, DataSourceJmxConfiguration.class })
	protected static class PooledDataSourceConfiguration {

	}

	/**
	 * {@link AnyNestedCondition} that checks that either {@code spring.datasource.type}
	 * is set or {@link PooledDataSourceAvailableCondition} applies.
	 */
	static class PooledDataSourceCondition extends AnyNestedCondition {

		PooledDataSourceCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.datasource", name = "type")
		static class ExplicitType {

		}

		@Conditional(PooledDataSourceAvailableCondition.class)
		static class PooledDataSourceAvailable {

		}

	}

	/**
	 * {@link Condition} to test if a supported connection pool is available.
	 */
	static class PooledDataSourceAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("PooledDataSource");
			if (DataSourceBuilder.findType(context.getClassLoader()) != null) {
				return ConditionOutcome.match(message.foundExactly("supported DataSource"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("supported DataSource").atAll());
		}

	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 * If a pooled {@link DataSource} is available, it will always be preferred to an
	 * {@code EmbeddedDatabase}.
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";

		private final SpringBootCondition pooledCondition = new PooledDataSourceCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("EmbeddedDataSource");
			if (hasDataSourceUrlProperty(context)) {
				return ConditionOutcome.noMatch(message.because(DATASOURCE_URL_PROPERTY + " is set"));
			}
			if (anyMatches(context, metadata, this.pooledCondition)) {
				return ConditionOutcome.noMatch(message.foundExactly("supported pooled data source"));
			}
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(context.getClassLoader()).getType();
			if (type == null) {
				return ConditionOutcome.noMatch(message.didNotFind("embedded database").atAll());
			}
			return ConditionOutcome.match(message.found("embedded database").items(type));
		}

		private boolean hasDataSourceUrlProperty(ConditionContext context) {
			Environment environment = context.getEnvironment();
			if (environment.containsProperty(DATASOURCE_URL_PROPERTY)) {
				try {
					return StringUtils.hasText(environment.getProperty(DATASOURCE_URL_PROPERTY));
				}
				catch (IllegalArgumentException ex) {
					// Ignore unresolvable placeholder errors
				}
			}
			return false;
		}

	}

}
