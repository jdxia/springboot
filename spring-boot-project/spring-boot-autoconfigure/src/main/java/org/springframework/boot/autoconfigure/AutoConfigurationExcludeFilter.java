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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * A {@link TypeFilter} implementation that matches registered auto-configuration classes.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public class AutoConfigurationExcludeFilter implements TypeFilter, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	private volatile List<String> autoConfigurations;

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		// metadataReader 就是扫描到的类的元数据
		// 扫描的时候发现如果是配置类并且是自动配置类就过滤掉
		// 是否是一个配置类，是否是一个自动配置类
		return isConfiguration(metadataReader) && isAutoConfiguration(metadataReader);
	}

	private boolean isConfiguration(MetadataReader metadataReader) {
		return metadataReader.getAnnotationMetadata().isAnnotated(Configuration.class.getName());
	}

	private boolean isAutoConfiguration(MetadataReader metadataReader) {
		// 拿到 spring.factories文件中的EnableAutoConfiguration注解的类和当前类判断 是不是包含的
		return getAutoConfigurations().contains(metadataReader.getClassMetadata().getClassName());
	}

	protected List<String> getAutoConfigurations() {
		if (this.autoConfigurations == null) {
			this.autoConfigurations = SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
					this.beanClassLoader);
		}
		return this.autoConfigurations;
	}

}
