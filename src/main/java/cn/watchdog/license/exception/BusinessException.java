package cn.watchdog.license.exception;


import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.util.gson.GsonProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * 自定义异常类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException {
	private final int code;
	private Object data;
	private String method;
	private String url;
	private Cookie[] cookies;
	private String params;
	private String userAgent;
	private String ip;
	private HttpServletRequest request;

	/**
	 * 状态码
	 */
	private int status = 400;

	public BusinessException(int code, String message, Object data, @Nullable HttpServletRequest request) {
		super(message);
		this.code = code;
		this.data = data;
		init(request);
	}

	public BusinessException(int code, String message, @Nullable HttpServletRequest request) {
		super(message);
		this.code = code;
		init(request);
	}

	public BusinessException(int code, String message, int status, @Nullable HttpServletRequest request) {
		super(message);
		this.code = code;
		this.status = status;
		init(request);
	}

	public BusinessException(ReturnCode returnCode, @Nullable HttpServletRequest request) {
		super(returnCode.getMessage());
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		init(request);
	}

	public BusinessException(ReturnCode returnCode, Object data, @Nullable HttpServletRequest request) {
		super(returnCode.getMessage());
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		this.data = data;
		init(request);
	}

	public BusinessException(ReturnCode returnCode, String message, @Nullable HttpServletRequest request) {
		super(message);
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		init(request);
	}

	public BusinessException(ReturnCode returnCode, String message, Object data, @Nullable HttpServletRequest request) {
		super(message);
		this.code = returnCode.getCode();
		this.status = returnCode.getStatus();
		this.data = data;
		init(request);
	}


	private void init(@Nullable HttpServletRequest request) {
		this.request = request;
		if (request != null) {
			this.method = request.getMethod();
			this.url = request.getRequestURI();
			this.cookies = request.getCookies();
			this.params = GsonProvider.normal().toJson(request.getParameterMap());
			this.userAgent = request.getHeader("User-Agent");
			this.ip = request.getRemoteAddr();
		}
	}
}
