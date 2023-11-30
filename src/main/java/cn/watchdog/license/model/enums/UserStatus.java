package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
	/**
	 * 正常
	 */
	NORMAL(0),
	/**
	 * 禁用
	 */
	DISABLED(1),
	/**
	 * 删除
	 */
	DELETED(2);

	final int code;

	UserStatus(int code) {
		this.code = code;
	}

	public static UserStatus valueOf(int value) {
		for (UserStatus status : UserStatus.values()) {
			if (status.code == value) {
				return status;
			}
		}
		return NORMAL;
	}
}
