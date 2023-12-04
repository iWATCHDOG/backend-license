package cn.watchdog.license.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 */
@Data
public class BaseResponse<T> implements Serializable {

	private int code;

	private T data;

	private String message;
	private RequestInfo requestInfo;

	public BaseResponse(int code, T data, String message) {
		this.code = code;
		this.data = data;
		this.message = message;
	}

	public BaseResponse(int code, T data) {
		this(code, data, "");
	}

	public BaseResponse(ReturnCode returnCode) {
		this(returnCode.getCode(), null, returnCode.getMessage());
	}

	public BaseResponse(ReturnCode returnCode, T data) {
		this(returnCode.getCode(), data, returnCode.getMessage());
	}

	@Data
	public static class RequestInfo {
		private String requestId;
		private long timestamp;
		private long cost;
	}
}
