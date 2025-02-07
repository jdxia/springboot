package smoketest.tomcat.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import smoketest.tomcat.config.Result;
import smoketest.tomcat.config.WebConfig;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class SysExceptionAdvice implements Ordered {

	private final static Logger logger = LoggerFactory.getLogger(WebConfig.class);

//	/**
//	 * 业务异常
//	 * @param exception
//	 * @return
//	 */
//	@ExceptionHandler(value = BizException.class)
//	public PxResponse bizExceptionHandle(BizException exception, HttpServletRequest request) {
//		logger.warn("请求:{} 发生业务异常", request.getRequestURI(), exception);
//
//		return PxResponse.fail(exception.getErrCode(), exception.getMessage());
//	}

	/**
	 * 参数验证异常
	 * @param exception
	 * @return
	 */
	@ExceptionHandler(value = ConstraintViolationException.class)
	public Result constraintViolationException(ConstraintViolationException exception) {
		Result response = Result.fail("0014", "非法参数");
		exception.getConstraintViolations().stream().findFirst().ifPresent(violation -> response.setMessage(violation.getMessage()));
		return response;
	}

	/**
	 * 参数验证异常, 返回一个
	 * @param exception
	 * @return
	 */
//	@ExceptionHandler(value = MethodArgumentNotValidException.class)
//	public Result methodArgumentNotValidException(MethodArgumentNotValidException exception) {
//		String defaultMessage = Objects.requireNonNull(exception.getBindingResult().getFieldError()).getDefaultMessage();
//		return Result.fail("0014", defaultMessage);
//	}


	/**
	 * 返回
	 * [
	 *     "用户名不能为空",
	 *     "邮箱格式不正确"
	 *   ]
	 */
//	@ExceptionHandler(MethodArgumentNotValidException.class)
//	public Result<List<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
//		List<String> errorMessages = exception.getBindingResult().getAllErrors()
//				.stream()
//				.map(ObjectError::getDefaultMessage)
//				.collect(Collectors.toList());
//
//		// 这里的 fail() 方法可以根据需要改造，让 data 部分返回 List<String>
//		return Result.fail("0014", "参数校验失败", errorMessages);
//	}


	/**
	 * 参数验证异常, 返回多个
	 * @param exception
	 * @return
	 * 返回
	 * [
	 *     {
	 *       "field": "userName",
	 *       "message": "用户名不能为空"
	 *     },
	 *     {
	 *       "field": "email",
	 *       "message": "邮箱格式不正确"
	 *     }
	 *   ]
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<List<Map<String, String>>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
		List<Map<String, String>> fieldErrors = exception.getBindingResult().getAllErrors()
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

		// 统一用 Result.fail() 返回
		return Result.fail("0014", "参数校验失败", fieldErrors);
	}



	/**
	 * 非法参数异常
	 * @param exception
	 * @return
	 */
	@ExceptionHandler(value = IllegalArgumentException.class)
	public Result illegalArgumentException(IllegalArgumentException exception) {
		return Result.fail("0014", exception.getMessage());
	}

	/**
	 * 参数校验异常, spring-web 里面的
	 * @param exception
	 * @return
	 */
//	@ExceptionHandler(value = HandlerMethodValidationException.class)
//	public Result HandlerMethodValidationException(HandlerMethodValidationException exception) {
//		Result response = Result.fail("0014", "非法参数");
//		exception.getValueResults().stream().findFirst()
//				.flatMap(validation -> validation.getResolvableErrors().stream().findFirst())
//				.ifPresent(resolvable -> response.setMessage(resolvable.getDefaultMessage()));
//		return response;
//	}

	/**
	 * 非法参数异常
	 * @param exception
	 * @return
	 */
	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	public Result httpMessageNotReadableException(HttpMessageNotReadableException exception) {
		return Result.fail("0015", "未获取到请求参数");
	}


	@Override
	public int getOrder() {
		return 0;
	}
}
