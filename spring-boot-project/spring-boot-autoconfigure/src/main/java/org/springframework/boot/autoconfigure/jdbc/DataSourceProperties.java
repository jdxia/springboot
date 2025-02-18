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

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a data source.
 *
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Benedikt Ritter
 * @author Eddú Meléndez
 * @author Scott Frederick
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {

	// 用户注入容器的类加载器
	private ClassLoader classLoader;

	/**
	 * Whether to generate a random datasource name.
	 */
	// 是否要随机生成一个数据源名称
	private boolean generateUniqueName = true;

	/**
	 * Datasource name to use if "generate-unique-name" is false. Defaults to "testdb"
	 * when using an embedded database, otherwise null.
	 */
	// 数据源的名称
	private String name;

	/**
	 * Fully qualified name of the connection pool implementation to use. By default, it
	 * is auto-detected from the classpath.
	 */
	// 用于精确指定用户要使用的数据源类型的全类名
	private Class<? extends DataSource> type;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 */
	// 数据源驱动的全类名
	private String driverClassName;

	/**
	 * JDBC URL of the database.
	 */
	// 数据库连接地址
	private String url;

	/**
	 * Login username of the database.
	 */
	// 数据库用户名
	private String username;

	/**
	 * Login password of the database.
	 */
	// 数据库密码
	private String password;

	/**
	 * JNDI location of the datasource. Class, url, username and password are ignored when
	 * set.
	 */
	// jndi名字
	private String jndiName;

	/**
	 * Mode to apply when determining if DataSource initialization should be performed
	 * using the available DDL and DML scripts.
	 */
	// 是否在数据源初始化后，执行配置的sql脚本
	@Deprecated
	private org.springframework.boot.jdbc.DataSourceInitializationMode initializationMode = org.springframework.boot.jdbc.DataSourceInitializationMode.EMBEDDED;

	/**
	 * Platform to use in the DDL or DML scripts (such as schema-${platform}.sql or
	 * data-${platform}.sql).
	 */
	// 指定要执行的脚本，schema-${platform}.sql 和 data-${platform}.sql
	@Deprecated
	private String platform = "all";

	/**
	 * Schema (DDL) script resource references.
	 */
	// DDL脚本
	private List<String> schema;

	/**
	 * Username of the database to execute DDL scripts (if different).
	 */
	// 执行DDL脚本所使用的用户名(如果不同于之前的用户名的话)
	@Deprecated
	private String schemaUsername;

	/**
	 * Password of the database to execute DDL scripts (if different).
	 */
	// 执行DDL脚本所使用的密码(如果不同于之前的密码的话)
	@Deprecated
	private String schemaPassword;

	/**
	 * Data (DML) script resource references.
	 */
	// DML脚本
	@Deprecated
	private List<String> data;

	/**
	 * Username of the database to execute DML scripts (if different).
	 */
	// 执行DML脚本所使用的用户名(如果不同于之前的用户名的话)
	@Deprecated
	private String dataUsername;

	/**
	 * Password of the database to execute DML scripts (if different).
	 */
	// 执行DML脚本所使用的密码(如果不同于之前的密码的话)
	@Deprecated
	private String dataPassword;

	/**
	 * Whether to stop if an error occurs while initializing the database.
	 */
	// 当初始化sql脚本发生错误时，是否继续
	@Deprecated
	private boolean continueOnError = false;

	/**
	 * Statement separator in SQL initialization scripts.
	 */
	// sql初始化脚本分隔符
	@Deprecated
	private String separator = ";";

	/**
	 * SQL scripts encoding.
	 */
	// sql初始化脚字符本编码格式
	@Deprecated
	private Charset sqlScriptEncoding;

	/**
	 * Connection details for an embedded database. Defaults to the most suitable embedded
	 * database that is available on the classpath.
	 */
	private EmbeddedDatabaseConnection embeddedDatabaseConnection;

	// XA全局事务相关配置
	private Xa xa = new Xa();

	// 唯一数据源名称
	private String uniqueName;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 依次尝试加载 H2、DERBY、HSQL的驱动，返回第一个成功加载的驱动
		// 那如果我指定了内嵌数据源类型，但是afterPropertiesSet是后面执行的，那这个值就被这里给覆盖了
		if (this.embeddedDatabaseConnection == null) {
			this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
		}
	}

	/**
	 * Initialize a {@link DataSourceBuilder} with the state of this instance.
	 * @return a {@link DataSourceBuilder} initialized with the customizations defined on
	 * this instance
	 */
	// 这里使用数据源属性配置类DataSourceProperties，构建DataSourceBuilder
	// 将会使用DataSourceBuilder来构建数据源
	public DataSourceBuilder<?> initializeDataSourceBuilder() {
		return DataSourceBuilder.create(getClassLoader()).type(
				// 即: spring.datasource.type的配置
				getType()
				).driverClassName(
						// 确定驱动类
						//		1. 如果有设置spring.datasource.driver-class-name，直接返回
						// 		2. 如果上面还不能确定，并且如果有设置spring.datasource.url，这个url必须以jdbc开头(否则报错),
						//		   然后从该url上截取，从DatabaseDriver中的常用驱动枚举类获取对应的驱动
						//		3. 如果上面还不能确定，则使用embeddedDatabaseConnection内嵌数据源连接的指定的驱动类
						// 		4. 现在还不能确定的话，就直接抛异常了
						determineDriverClassName())
				.url(
						// 确定数据库连接地址
						//		1. 如果有设置spring.datasource.url，直接返回
						//		2. 如果上面还不能确定, 则须先确定数据库名称，即spring.datasource.name配置或默认的testdb
						//		   然后使用embeddedDatabaseConnection.getUrl(数据库名称)获取连接地址
						// 		3. 现在还不能确定的话，就直接抛异常了
						determineUrl()).username(
								// 确定用户名
								//		1. 如果有设置spring.datasource.username，直接返回
								//		2. 如果上面确定的驱动类是HSQL或H2或DERBY中的任何一个，则返回“sa”
								//		3. 以上不能确定的话，返回null
								determineUsername()).password(
										// 确定密码
										//		1. 如果有设置spring.datasource.password，直接返回
										//		2. 如果上面确定的驱动类是HSQL或H2或DERBY中的任何一个，则返回空字符串
										// 		3. 以上不能确定的话，返回null
										determinePassword());
	}

	public boolean isGenerateUniqueName() {
		return this.generateUniqueName;
	}

	public void setGenerateUniqueName(boolean generateUniqueName) {
		this.generateUniqueName = generateUniqueName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<? extends DataSource> getType() {
		return this.type;
	}

	public void setType(Class<? extends DataSource> type) {
		this.type = type;
	}

	/**
	 * Return the configured driver or {@code null} if none was configured.
	 * @return the configured driver
	 * @see #determineDriverClassName()
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	/**
	 * Determine the driver to use based on this configuration and the environment.
	 * @return the driver to use
	 * @since 1.4.0
	 */
	// 确定驱动类
	// DataSourceConfiguration中定义的3个组件都是会调用到这个方法
	public String determineDriverClassName() {

		// 如果设置了spring.datasource.driverClassName，尝试加载这个类，并返回
		if (StringUtils.hasText(this.driverClassName)) {
			Assert.state(driverClassIsLoadable(), () -> "Cannot load driver class: " + this.driverClassName);
			return this.driverClassName;
		}
		String driverClassName = null;

		// 如果有设置spring.datasource.url， 则从DatabaseDriver枚举值中查找
		if (StringUtils.hasText(this.url)) {
			driverClassName = DatabaseDriver.fromJdbcUrl(this.url).getDriverClassName();
		}

		// 如果上面还不能确定， 则使用embeddedDatabaseConnection获取驱动类(这个时候，就要使用内嵌数据源了)
		if (!StringUtils.hasText(driverClassName)) {
			driverClassName = this.embeddedDatabaseConnection.getDriverClassName();
		}

		// 如果此时还不能确定驱动类，就要抛异常了
		if (!StringUtils.hasText(driverClassName)) {
			throw new DataSourceBeanCreationException("Failed to determine a suitable driver class", this,
					this.embeddedDatabaseConnection);
		}
		return driverClassName;
	}

	// 尝试反射加载驱动类
	private boolean driverClassIsLoadable() {
		try {
			ClassUtils.forName(this.driverClassName, null);
			return true;
		}
		catch (UnsupportedClassVersionError ex) {
			// Driver library has been compiled with a later JDK, propagate error
			throw ex;
		}
		catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * Return the configured url or {@code null} if none was configured.
	 * @return the configured url
	 * @see #determineUrl()
	 */
	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Determine the url to use based on this configuration and the environment.
	 * @return the url to use
	 * @since 1.4.0
	 */
	// 确定数据库连接地址
	public String determineUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		String databaseName = determineDatabaseName();
		String url = (databaseName != null) ? this.embeddedDatabaseConnection.getUrl(databaseName) : null;
		if (!StringUtils.hasText(url)) {
			throw new DataSourceBeanCreationException("Failed to determine suitable jdbc url", this,
					this.embeddedDatabaseConnection);
		}
		return url;
	}

	/**
	 * Determine the name to used based on this configuration.
	 * @return the database name to use or {@code null}
	 * @since 2.0.0
	 */
	// 确定数据库名称
	public String determineDatabaseName() {
		if (this.generateUniqueName) {
			if (this.uniqueName == null) {
				this.uniqueName = UUID.randomUUID().toString();
			}
			return this.uniqueName;
		}
		if (StringUtils.hasLength(this.name)) {
			return this.name;
		}
		if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
			return "testdb";
		}
		return null;
	}

	/**
	 * Return the configured username or {@code null} if none was configured.
	 * @return the configured username
	 * @see #determineUsername()
	 */
	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Determine the username to use based on this configuration and the environment.
	 * @return the username to use
	 * @since 1.4.0
	 */
	// 确定数据库用户名
	public String determineUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(determineDriverClassName(), determineUrl())) {
			return "sa";
		}
		return null;
	}

	/**
	 * Return the configured password or {@code null} if none was configured.
	 * @return the configured password
	 * @see #determinePassword()
	 */
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Determine the password to use based on this configuration and the environment.
	 * @return the password to use
	 * @since 1.4.0
	 */
	// 确定数据库密码
	public String determinePassword() {
		if (StringUtils.hasText(this.password)) {
			return this.password;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(determineDriverClassName(), determineUrl())) {
			return "";
		}
		return null;
	}

	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * Allows the DataSource to be managed by the container and obtained via JNDI. The
	 * {@code URL}, {@code driverClassName}, {@code username} and {@code password} fields
	 * will be ignored when using JNDI lookups.
	 * @param jndiName the JNDI name
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.mode")
	public org.springframework.boot.jdbc.DataSourceInitializationMode getInitializationMode() {
		return this.initializationMode;
	}

	@Deprecated
	public void setInitializationMode(org.springframework.boot.jdbc.DataSourceInitializationMode initializationMode) {
		this.initializationMode = initializationMode;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.platform")
	public String getPlatform() {
		return this.platform;
	}

	@Deprecated
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.schema-locations")
	public List<String> getSchema() {
		return this.schema;
	}

	@Deprecated
	public void setSchema(List<String> schema) {
		this.schema = schema;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.username")
	public String getSchemaUsername() {
		return this.schemaUsername;
	}

	@Deprecated
	public void setSchemaUsername(String schemaUsername) {
		this.schemaUsername = schemaUsername;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.password")
	public String getSchemaPassword() {
		return this.schemaPassword;
	}

	@Deprecated
	public void setSchemaPassword(String schemaPassword) {
		this.schemaPassword = schemaPassword;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.data-locations")
	public List<String> getData() {
		return this.data;
	}

	@Deprecated
	public void setData(List<String> data) {
		this.data = data;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.username")
	public String getDataUsername() {
		return this.dataUsername;
	}

	@Deprecated
	public void setDataUsername(String dataUsername) {
		this.dataUsername = dataUsername;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.password")
	public String getDataPassword() {
		return this.dataPassword;
	}

	@Deprecated
	public void setDataPassword(String dataPassword) {
		this.dataPassword = dataPassword;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.continue-on-error")
	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	@Deprecated
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.separator")
	public String getSeparator() {
		return this.separator;
	}

	@Deprecated
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.sql.init.encoding")
	public Charset getSqlScriptEncoding() {
		return this.sqlScriptEncoding;
	}

	@Deprecated
	public void setSqlScriptEncoding(Charset sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	public EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
		return this.embeddedDatabaseConnection;
	}

	public void setEmbeddedDatabaseConnection(EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		this.embeddedDatabaseConnection = embeddedDatabaseConnection;
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public Xa getXa() {
		return this.xa;
	}

	public void setXa(Xa xa) {
		this.xa = xa;
	}

	/**
	 * XA Specific datasource settings.
	 */
	// XA全局事务配置
	public static class Xa {

		/**
		 * XA datasource fully qualified name.
		 */
		private String dataSourceClassName;

		/**
		 * Properties to pass to the XA data source.
		 */
		private Map<String, String> properties = new LinkedHashMap<>();

		public String getDataSourceClassName() {
			return this.dataSourceClassName;
		}

		public void setDataSourceClassName(String dataSourceClassName) {
			this.dataSourceClassName = dataSourceClassName;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

	}

	static class DataSourceBeanCreationException extends BeanCreationException {

		private final DataSourceProperties properties;

		private final EmbeddedDatabaseConnection connection;

		DataSourceBeanCreationException(String message, DataSourceProperties properties,
				EmbeddedDatabaseConnection connection) {
			super(message);
			this.properties = properties;
			this.connection = connection;
		}

		DataSourceProperties getProperties() {
			return this.properties;
		}

		EmbeddedDatabaseConnection getConnection() {
			return this.connection;
		}

	}

}
