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

package org.springframework.boot.autoconfigure.condition;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} and {@link AutoConfigurationImportFilter} that checks for the
 * presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class OnClassCondition extends FilteringSpringBootCondition {

	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		// Split the work and perform half in a background thread if more than one
		// processor is available. Using a single additional thread seems to offer the
		// best performance. More threads make things worse.
		// 判断是不是多核
		if (autoConfigurationClasses.length > 1 && Runtime.getRuntime().availableProcessors() > 1) {
			// 多核走这
			return resolveOutcomesThreaded(autoConfigurationClasses, autoConfigurationMetadata);
		}
		else {
			OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, 0,
					autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
			return outcomesResolver.resolveOutcomes();
		}
	}

	private ConditionOutcome[] resolveOutcomesThreaded(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		// 所有配置项除以2, 分开来
		int split = autoConfigurationClasses.length / 2;
		// 处理, 里面有多线程, 第一半开启一个线程处理
		OutcomesResolver firstHalfResolver = createOutcomesResolver(autoConfigurationClasses, 0, split,
				autoConfigurationMetadata);
		// 另一半就当前线程处理了
		OutcomesResolver secondHalfResolver = new StandardOutcomesResolver(autoConfigurationClasses, split,
				autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
		// resolveOutcomes 方法里面有 join线程等待
		ConditionOutcome[] secondHalf = secondHalfResolver.resolveOutcomes();
		ConditionOutcome[] firstHalf = firstHalfResolver.resolveOutcomes();
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		System.arraycopy(firstHalf, 0, outcomes, 0, firstHalf.length);
		System.arraycopy(secondHalf, 0, outcomes, split, secondHalf.length);
		return outcomes;
	}

	private OutcomesResolver createOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, start, end,
				autoConfigurationMetadata, getBeanClassLoader());
		try {
			// 开启线程处理了
			return new ThreadedOutcomesResolver(outcomesResolver);
		}
		catch (AccessControlException ex) {
			return outcomesResolver;
		}
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ClassLoader classLoader = context.getClassLoader();
		ConditionMessage matchMessage = ConditionMessage.empty();

		// 拿到ConditionalOnClass注解中的value值，也就是要判断是否存在的类名
		List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
		if (onClasses != null) {
			// 判断onClasses中不存在的类
			List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
			// 如果有缺失的类，那就表示不匹配
			if (!missing.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
						.didNotFind("required class", "required classes").items(Style.QUOTE, missing));
			}
			// 否则就表示匹配
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
					.found("required class", "required classes")
					.items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
		}

		// 和上面类似，只不过是判断onMissingClasses是不是全部缺失，如果是则表示匹配
		List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
						.found("unwanted class", "unwanted classes").items(Style.QUOTE, present));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
					.didNotFind("unwanted class", "unwanted classes")
					.items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
		}
		return ConditionOutcome.match(matchMessage);
	}

	private List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
		if (attributes == null) {
			return null;
		}
		List<String> candidates = new ArrayList<>();
		addAll(candidates, attributes.get("value"));
		addAll(candidates, attributes.get("name"));
		return candidates;
	}

	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private interface OutcomesResolver {
		// 可以看下子类 ThreadedOutcomesResolver
		ConditionOutcome[] resolveOutcomes();

	}

	private static final class ThreadedOutcomesResolver implements OutcomesResolver {

		private final Thread thread;

		private volatile ConditionOutcome[] outcomes;

		private ThreadedOutcomesResolver(OutcomesResolver outcomesResolver) {
			this.thread = new Thread(() -> this.outcomes = outcomesResolver.resolveOutcomes());
			this.thread.start();
		}

		@Override
		public ConditionOutcome[] resolveOutcomes() {
			try {
				this.thread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return this.outcomes;
		}

	}

	private static final class StandardOutcomesResolver implements OutcomesResolver {

		private final String[] autoConfigurationClasses;

		private final int start;

		private final int end;

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private final ClassLoader beanClassLoader;

		private StandardOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
				AutoConfigurationMetadata autoConfigurationMetadata, ClassLoader beanClassLoader) {
			this.autoConfigurationClasses = autoConfigurationClasses;
			this.start = start;
			this.end = end;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
			this.beanClassLoader = beanClassLoader;
		}

		@Override
		public ConditionOutcome[] resolveOutcomes() {
			return getOutcomes(this.autoConfigurationClasses, this.start, this.end, this.autoConfigurationMetadata);
		}

		private ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses, int start, int end,
				AutoConfigurationMetadata autoConfigurationMetadata) {

			// 记录每个自动配置的匹配结果
			ConditionOutcome[] outcomes = new ConditionOutcome[end - start];

			// 遍历每个自动配置进行匹配
			for (int i = start; i < end; i++) {
				String autoConfigurationClass = autoConfigurationClasses[i];
				if (autoConfigurationClass != null) {
					// 从autoConfigurationMetadata中获取当前自动配置的ConditionalOnClass的属性，拿到的就是当前自动配置所需要的类
					String candidates = autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnClass");
					if (candidates != null) {
						// 判断需要的类存不存在
						outcomes[i - start] = getOutcome(candidates);
					}
				}
			}
			return outcomes;
		}

		private ConditionOutcome getOutcome(String candidates) {
			try {
				if (!candidates.contains(",")) {
					return getOutcome(candidates, this.beanClassLoader);
				}
				for (String candidate : StringUtils.commaDelimitedListToStringArray(candidates)) {
					ConditionOutcome outcome = getOutcome(candidate, this.beanClassLoader);
					if (outcome != null) {
						return outcome;
					}
				}
			}
			catch (Exception ex) {
				// We'll get another chance later
			}
			return null;
		}

		private ConditionOutcome getOutcome(String className, ClassLoader classLoader) {
			if (ClassNameFilter.MISSING.matches(className, classLoader)) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
						.didNotFind("required class").items(Style.QUOTE, className));
			}
			return null;
		}

	}

}
