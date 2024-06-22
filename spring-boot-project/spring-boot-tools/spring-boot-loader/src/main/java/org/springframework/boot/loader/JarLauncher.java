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

package org.springframework.boot.loader;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

	private static final String DEFAULT_CLASSPATH_INDEX_LOCATION = "BOOT-INF/classpath.idx";

	static final EntryFilter NESTED_ARCHIVE_ENTRY_FILTER = (entry) -> {
		// 只接受 `BOOT-INF/classes/` 目录
		if (entry.isDirectory()) {
			return entry.getName().equals("BOOT-INF/classes/");
		}

		// 只接受 `BOOT-INF/lib/` 目录下的 jar 包
		return entry.getName().startsWith("BOOT-INF/lib/");
	};

	public JarLauncher() {
		// 看下父类的
	}

	protected JarLauncher(Archive archive) {
		super(archive);
	}

	@Override
	protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		// Only needed for exploded archives, regular ones already have a defined order
		if (archive instanceof ExplodedArchive) {
			String location = getClassPathIndexFileLocation(archive);
			return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
		}
		return super.getClassPathIndex(archive);
	}

	private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : DEFAULT_CLASSPATH_INDEX_LOCATION;
	}

	@Override
	protected boolean isPostProcessingClassPathArchives() {
		return false;
	}

	@Override
	protected boolean isSearchCandidate(Archive.Entry entry) {
		return entry.getName().startsWith("BOOT-INF/");
	}

	// 用于判断FAT JAR资源的相对路径是否为nestedArchive嵌套文档。进而决定这些FAT JAR是否会被launch。 当方法返回false时，说明FAT JAR被解压至文件目录
	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		return NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
	}

	// 参考 Oracle 官方对于 jar 的说明 https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html
	// jar包启动的时候, 是用这个来启动的, 和idea里面的不一样
	public static void main(String[] args) throws Exception {
		/**
		 * Manifest-Version: 1.0
		 * Spring-Boot-Classpath-Index: BOOT-INF/classpath.idx
		 * Implementation-Title: eleven-springboot
		 * Implementation-Version: 0.0.1-SNAPSHOT
		 * Spring-Boot-Layers-Index: BOOT-INF/layers.idx
		 * Start-Class: smoketest.tomcat.SampleTomcatApplication
		 * Spring-Boot-Classes: BOOT-INF/classes/   应用程序类
		 * Spring-Boot-Lib: BOOT-INF/lib/
		 * Build-Jdk-Spec: 1.8
		 * Spring-Boot-Version: 2.5.5
		 * Created-By: Maven Jar Plugin 3.2.0
		 * Main-Class: org.springframework.boot.loader.JarLauncher  应用的启动类
		 *
		 *
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
		 * org: Spring Boot loader 相关的源代码
		 *
		 * 打包可以看 {@link org.springframework.boot.maven.RepackageMojo#execute()}
		 *
		 * 怎么定位jarFile里面的class可以看这个
		 * https://docs.spring.io/spring-boot/docs/3.0.1/reference/html/executable-jar.html#:~:text=myapp.jar%20%2B,3980
		 *
		 * jar:file:C:/Users/Administrator/Desktop/demo/demo/target/jarlauncher-0.0.1-SNAPSHOT.jar!/lib/spring-boot-1.5.10.RELEASE.jar!/
		 * 如果有 jar 包中包含 jar，或者 jar 包中包含 jar 包里面的 class 文件，那么会使用 !/ 分隔开，这种方式只有 org.springframework.boot.loader.jar.Handler 能处理，它是 SpringBoot 内部扩展出来一种URL协议
		 */


		/**
		 * launch 方法里面有个classloader
		 * 如果你在idea里面也想要这个效果, 需要在springboot里面引入 spring-boot-loader 这个包, 并且在这里启动, 就可以了
		 * 你在项目中一开始打印下 线程上下文的类加载器, 你看下idea项目的main启动, 和jar包启动打印的类加载器是不是一样, 是不一样的
		 * jar包怎么打出来? spring-boot-maven-plugin 插件就可以
		 *
		 * 为啥要这个? 因为默认的classloader是只能加载当前工程的, 不能找到依赖的jar包
		 *
		 * jar包启动是先执行这个的main方法, 然后创建 LaunchedURLClassLoader 类加载器,
		 * 管理的路径是jar包里面的BOOT-INF的classes(项目自己写的类)和lib(项目的依赖)文件夹
		 *
		 * 然后从META-INF/MANIFEST.MF文件里面获取Start-Class的值, 然后启动这个类的main方法
		 *
		 * 为什么JarLauncher能够引导，而直接运行Start-Class却不行?
		 * 在解压jar包后的根目录下运行 java org.springframework.boot.loader.JarLauncher。项目引导类（META-INF/MANIFEST.MF文件中的Start-Class属性）被JarLauncher加载并执行。
		 * 如果直接运行Start-Class类，会报错ClassNotFoundException。
		 * Spring Boot依赖的jar文件均存放在BOOT-INF/lib目录下。JarLauncher会将这些jar文件作为Start-Class的类库依赖。
		 *
		 * 先看 new JarLauncher() 构造方法 的父类
		 *
		 * 看 launch 方法, 重点
		 * 调用父类Launcher中的launch()方法启动程序
		 */
		new JarLauncher().launch(args);
	}

}
