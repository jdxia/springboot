package smoketest.tomcat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import smoketest.tomcat.web.SampleController;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

	private final static Logger logger = LoggerFactory.getLogger(WebConfig.class);

	// 配置 Content Negotiation（内容协商）
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		// 忽略请求的Accept头，强制使用默认配置
		configurer
				.favorPathExtension(false)     // 不再使用 URL 后缀 (.json / .xml) 来进行判断
				.favorParameter(false)         // 不再使用请求参数 (?format=json) 来进行判断
				.ignoreAcceptHeader(true)      // 忽略客户端的Accept头
				.defaultContentType(MediaType.APPLICATION_JSON);
	}
}

