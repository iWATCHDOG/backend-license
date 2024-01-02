package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
	/**
	 * 正常
	 */
	NORMAL(0, "正常"),
	/**
	 * 禁用
	 */
	BANNED(1, "禁用"),
	/**
	 * 删除
	 */
	DELETED(2, "删除");

	final int code;
	final String desc;

	UserStatus(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static UserStatus valueOf(int value) {
		for (UserStatus status : UserStatus.values()) {
			if (status.code == value) {
				return status;
			}
		}
		return BANNED;
	}
}
