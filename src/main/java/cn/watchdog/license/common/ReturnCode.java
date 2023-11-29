package cn.watchdog.license.common;

import lombok.Getter;

/**
 * 错误码
 */
@Getter
public enum ReturnCode {
	/**
	 * 操作成功
	 */
	SUCCESS(20000, "Operation succeeded"),
	/**
	 * 请求参数错误
	 */
	PARAMS_ERROR(40000, "Request parameter error"),
	/**
	 * 未登录
	 */
	NOT_LOGIN_ERROR(40100, "Not logged in"),
	/**
	 * 无权限
	 */
	NO_AUTH_ERROR(40101, "No permission"),
	/**
	 * 验证失败
	 */
	VALIDATION_FAILED(40102, "Validation failed"),
	/**
	 * 账号被封禁
	 */
	ACCOUNT_BANED(40103, "The account has been baned."),
	/**
	 * 请求数据不存在
	 */
	NOT_FOUND_ERROR(40400, "Request data does not exist"),
	/**
	 * 禁止访问
	 */
	FORBIDDEN_ERROR(40300, "Forbidden Access"),
	/**
	 * 系统内部异常
	 */
	SYSTEM_ERROR(50000, "System internal exception"),
	/**
	 * 操作失败
	 */
	OPERATION_ERROR(50001, "Operation failed");

	/**
	 * 状态码
	 */
	private final int code;

	/**
	 * 信息
	 */
	private final String message;

	ReturnCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

}
