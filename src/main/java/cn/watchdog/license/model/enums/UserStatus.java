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

	private final int value;

	UserStatus(int value) {
		this.value = value;
	}

	public static UserStatus valueOf(int value) {
		for (UserStatus status : UserStatus.values()) {
			if (status.value == value) {
				return status;
			}
		}
		return NORMAL;
	}
}
