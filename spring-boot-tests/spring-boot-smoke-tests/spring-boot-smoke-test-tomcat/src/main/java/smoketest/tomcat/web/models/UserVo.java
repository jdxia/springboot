package smoketest.tomcat.web.models;

import lombok.Data;
import org.springframework.validation.annotation.Validated;
import smoketest.tomcat.valida.Phone;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Validated
public class UserVo {

	private interface StrategyA {}

	private interface StrategyB {}

	@Phone(message = "手机号不正确", groups = {StrategyA.class})
	private String phone;

	@NotBlank(message = "名字不能为空", groups = {StrategyA.class})
	private String name;

	@NotNull(message = "年龄不能为空")
	private Integer age;

	// 嵌套验证
	@Valid
	@NotNull(message = "学校不能为空")
	private SchoolVo school;
}
