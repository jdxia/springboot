package smoketest.tomcat.valida;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {

	/**
	 * 校验不通过的message
	 */
	String message() default "请输入正确的手机号";

	/**
	 * 分组校验
	 */
	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
}
