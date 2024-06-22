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

package smoketest.tomcat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smoketest.tomcat.web.models.UserVo;

@Component
public class HelloWorldService {

	private final static Logger logger = LoggerFactory.getLogger(HelloWorldService.class);

	@Value("${test.name:World}")
	private String name;

	public String getHelloMessage() {
		logger.info("==============>");
		return "Hello " + this.name;
	}


	public Boolean studyValida(UserVo userVo) {
		logger.info("==============> studyValida");


		return Boolean.TRUE;
	}

}
