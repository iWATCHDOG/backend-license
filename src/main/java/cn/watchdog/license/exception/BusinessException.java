package cn.watchdog.license.exception;


import cn.watchdog.license.common.ReturnCode;
import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException {

	private final int code;
	/**
	 * 状态码
	 */
	private int status = 400;

	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	public BusinessException(ReturnCode returnCode) {
		super(returnCode.getMessage());
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
	}

	public BusinessException(ReturnCode returnCode, String message) {
		super(message);
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
	}

}
