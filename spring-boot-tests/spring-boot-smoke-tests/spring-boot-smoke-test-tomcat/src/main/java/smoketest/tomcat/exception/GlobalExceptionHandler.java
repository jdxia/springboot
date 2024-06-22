package smoketest.tomcat.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import smoketest.tomcat.config.Result;
import smoketest.tomcat.config.WebConfig;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final static Logger logger = LoggerFactory.getLogger(WebConfig.class);

	/**
	 * 处理所有未捕获的异常
	 */
	@ExceptionHandler(Exception.class)
	public Result<?> handleException(Exception e, HttpServletRequest request) {
		// 可以根据需要进行日志打印等操作
		logger.error("请求:{} 发生系统内部异常", request.getRequestURI(), e);

		// 返回统一结构
		return Result.fail("00500", "服务器内部错误：" + e.getMessage());
	}
}

