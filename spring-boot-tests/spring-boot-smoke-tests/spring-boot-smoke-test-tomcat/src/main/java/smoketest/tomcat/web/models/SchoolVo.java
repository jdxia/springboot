package smoketest.tomcat.web.models;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

public class SchoolVo {

	@NotNull(message = "学校id不能为空")
	private Long id;

	@NotBlank(message = "学校名字不能为空")
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		SchoolVo schoolVo = (SchoolVo) o;
		return Objects.equals(id, schoolVo.id) && Objects.equals(name, schoolVo.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Override
	public String toString() {
		return "SchoolVo{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}
