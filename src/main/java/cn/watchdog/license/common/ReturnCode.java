package cn.watchdog.license.common;

import lombok.Getter;

/**
 * 错误码
 */
@Getter
public enum ReturnCode {
	SUCCESS(20000, "操作成功"),
	PARAMS_ERROR(40000, "请求参数错误"),
	NOT_LOGIN_ERROR(40100, "未登录"),
	NO_AUTH_ERROR(40101, "无权限"),
	VALIDATION_FAILED(40102, "验证失败"),
	ACCOUNT_BANED(40103, "账号已被封禁"),
	NOT_FOUND_ERROR(40400, "请求数据不存在"),
	FORBIDDEN_ERROR(40300, "禁止访问"),
	SYSTEM_ERROR(50000, "系统内部异常"),
	OPERATION_ERROR(50001, "操作失败");

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
