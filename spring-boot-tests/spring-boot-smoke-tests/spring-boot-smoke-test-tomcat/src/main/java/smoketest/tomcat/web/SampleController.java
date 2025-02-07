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

package smoketest.tomcat.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import smoketest.tomcat.config.Result;
import smoketest.tomcat.service.HelloWorldService;
import smoketest.tomcat.web.models.UserVo;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Validated
/**
 * 类似一个切面, 有 ValidationAutoConfiguration 这个自动注入类
 * {@link ValidationAutoConfiguration#methodValidationPostProcessor(Environment, Validator, ObjectProvider)}
 */
@RestController
public class SampleController {

	private final static Logger log = LoggerFactory.getLogger(SampleController.class);

	@Resource
	private HelloWorldService helloWorldService;

	// http://127.0.0.1:8083/
	@GetMapping("/")
	@ResponseBody
	public Result<String> helloWorld() {
		return Result.success(this.helloWorldService.getHelloMessage());
	}

	@GetMapping("/test1")
	@ResponseBody
	public Result<Boolean> test1() {
		UserVo userVo = new UserVo();

		return Result.success(this.helloWorldService.studyValida(userVo));
	}

	// http://127.0.0.1:8083/valid
	@PostMapping("/valid")
	@ResponseBody
	public Result<String> testValid(@RequestBody @Valid UserVo userVo, BindingResult bindingResult) {

		log.info("===> 开始验证");

		List<Map<String, String>> fieldErrors = bindingResult.getAllErrors()
				.stream()
				.map(error -> {
					Map<String, String> fieldErrorMap = new HashMap<>();
					if (error instanceof FieldError) {
						FieldError fieldError = (FieldError) error;
						fieldErrorMap.put("field", fieldError.getField());
						fieldErrorMap.put("message", fieldError.getDefaultMessage());
					} else {
						// 对于 @Valid 验证的是类级别的错误，可能是 ObjectError
						fieldErrorMap.put("field", error.getObjectName());
						fieldErrorMap.put("message", error.getDefaultMessage());
					}
					return fieldErrorMap;
				})
				.collect(Collectors.toList());

		log.error("参数验证里面错误信息: {}", fieldErrors);

		return Result.success(this.helloWorldService.getHelloMessage());
	}

}
