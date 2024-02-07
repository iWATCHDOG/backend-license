package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum SecurityType {
	/**
	 * 通过忘记密码找回密码
	 */
	CHANGE_PASSWORD_FORGET(100),
	/**
	 * 通过原密码修改密码
	 */
	CHANGE_PASSWORD(101),
	/**
	 * 绑定GitHub
	 */
	BIND_GITHUB(200),
	/**
	 * 解绑GitHub
	 */
	UNBIND_GITHUB(201),
	/**
	 * 更新资料
	 */
	UPDATE_PROFILE(300),
	/**
	 * 修改头像
	 */
	CHANGE_AVATAR(301),
	/**
	 * 注册
	 */
	ADD_ACCOUNT(400),
	/**
	 * 注销账号
	 */
	DELETE_USER(401),
	/**
	 * 管理操作
	 */
	ADMIN_OPERATION(500),
	/**
	 * 添加权限
	 */
	ADD_PERMISSION(501),
	/**
	 * 移除权限
	 */
	REMOVE_PERMISSION(502),
	/**
	 * 修改权限
	 */
	UPDATE_PERMISSION(503),
	;

	final int code;

	SecurityType(int code) {
		this.code = code;
	}

	public static SecurityType valueOf(int code) {
		for (SecurityType securityType : SecurityType.values()) {
			if (securityType.code == code) {
				return securityType;
			}
		}
		return null;
	}
}
