package smoketest.tomcat.config;

import java.util.Objects;

public class Result<T> {
	private String code;
	private String message;
	private T data;

	public Result(String code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	// 也可使用建造者模式来简化创建
	public static <T> Result<T> success(T data) {
		return new Result<>("200", "success", data);
	}

	public static <T> Result<T> fail(String code, String message) {
		return new Result<>(code, message, null);
	}

	public static <T> Result<T> fail(String code, String message, T data) {
		return new Result<>(code, message, data);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Result<?> result = (Result<?>) o;
		return Objects.equals(code, result.code) && Objects.equals(message, result.message) && Objects.equals(data, result.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, message, data);
	}

	@Override
	public String toString() {
		return "Result{" +
				"code='" + code + '\'' +
				", message='" + message + '\'' +
				", data=" + data +
				'}';
	}
}

