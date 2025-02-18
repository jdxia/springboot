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

package org.springframework.boot.loader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class Launcher {

	private static final String JAR_MODE_LAUNCHER = "org.springframework.boot.loader.jarmode.JarModeLauncher";

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		if (!isExploded()) {
			// 注册jar URL处理器
			JarFile.registerUrlProtocolHandler();
		}
		/**
		 * 创建一个类加载器, 这边不是AppClassLoader, 而是 LaunchedURLClassLoader
		 * 你把打好的jar包解压开来,你会发现,里面有一层一层包名,然后类名,
		 * AppClassLoader加载类的时候, 只能加载准确路径的类, 但是LaunchedURLClassLoader可以加载jar包里面的类, 在jar包里面根据包名类名一层一层去找
		 *
		 * 如果你在idea里面也想要这个效果, 需要在springboot里面引入 spring-boot-loader 这个包, 并且在 org.springframework.boot.loader.JarLauncher#main(java.lang.String[]) 这里启动
		 *
		 * getClassPathArchivesIterator 看子类 {@link org.springframework.boot.loader.ExecutableArchiveLauncher#getClassPathArchivesIterator()}
		 */
		ClassLoader classLoader = createClassLoader(getClassPathArchivesIterator());
		String jarMode = System.getProperty("jarmode");
		// getMainClass 是获取我们业务的main方法在的启动类, 重点
		String launchClass = (jarMode != null && !jarMode.isEmpty()) ? JAR_MODE_LAUNCHER : getMainClass();
		// 调用实际的引导类launch, 往下
		launch(args, launchClass, classLoader);
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of
	 * {@link #createClassLoader(Iterator)}
	 */
	@Deprecated
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		return createClassLoader(archives.iterator());
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 * @since 2.3.0
	 */
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		// <1> 获取所有 JarFileArchive 对应的 URL
		List<URL> urls = new ArrayList<>(50);
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}

		// <2> 创建 Spring Boot 自定义的 ClassLoader 类加载器，并设置父类加载器为当前线程的类加载器
		// 通过它解析这些 URL，也就是加载 `BOOT-INF/classes/` 目录下的类和 `BOOT-INF/lib/` 目录下的所有 jar 包
		return createClassLoader(urls.toArray(new URL[0]));
	}

	/**
	 * Create a classloader for the specified URLs.
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(isExploded(), getArchive(), urls, getClass().getClassLoader());
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param args the incoming arguments
	 * @param launchClass the launch class to run
	 * @param classLoader the classloader
	 * @throws Exception if the launch fails
	 */
	protected void launch(String[] args, String launchClass, ClassLoader classLoader) throws Exception {
		// 设置当前线程的 ClassLoader 为刚创建的类加载器
		Thread.currentThread().setContextClassLoader(classLoader);
		// 创建一个 MainMethodRunner 对象（main 方法执行器）
		// run往下, 执行你的 main 方法（反射）
		createMainMethodRunner(launchClass, args, classLoader).run();
	}

	/**
	 * Create the {@code MainMethodRunner} used to launch the application.
	 * @param mainClass the main class
	 * @param args the incoming arguments
	 * @param classLoader the classloader
	 * @return the main method runner
	 */
	protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
		return new MainMethodRunner(mainClass, args);
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 * @since 2.3.0
	 */
	protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
		return getClassPathArchives().iterator();
	}

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of implementing
	 * {@link #getClassPathArchivesIterator()}.
	 */
	@Deprecated
	protected List<Archive> getClassPathArchives() throws Exception {
		throw new IllegalStateException("Unexpected call to getClassPathArchives()");
	}

	protected final Archive createArchive() throws Exception {
		// 获取 jar 包（当前应用）所在的绝对路径
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}

		// 当前 jar 包
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException("Unable to determine code source archive from " + root);
		}

		// 为当前 jar 包创建一个 JarFileArchive（根条目），需要通过它解析出 jar 包中的所有信息
		// 如果是文件夹的话则创建 ExplodedArchive（根条目）
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

	/**
	 * Returns if the launcher is running in an exploded mode. If this method returns
	 * {@code true} then only regular JARs are supported and the additional URL and
	 * ClassLoader support infrastructure can be optimized.
	 * @return if the jar is exploded.
	 * @since 2.3.0
	 */
	protected boolean isExploded() {
		return false;
	}

	/**
	 * Return the root archive.
	 * @return the root archive
	 * @since 2.3.1
	 */
	protected Archive getArchive() {
		return null;
	}

}
