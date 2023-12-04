package cn.watchdog.license.exception;


import cn.watchdog.license.common.ReturnCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义异常类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException {

	private final int code;
	private Object data;
	/**
	 * 状态码
	 */
	private int status = 400;

	public BusinessException(int code, String message, Object data) {
		super(message);
		this.code = code;
		this.data = data;
	}

	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	public BusinessException(int code, String message, int status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public BusinessException(ReturnCode returnCode) {
		super(returnCode.getMessage());
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
	}

	public BusinessException(ReturnCode returnCode, Object data) {
		super(returnCode.getMessage());
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		this.data = data;
	}

	public BusinessException(ReturnCode returnCode, String message) {
		super(message);
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
	}

	public BusinessException(ReturnCode returnCode, String message, Object data) {
		super(message);
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		this.data = data;
	}

}
