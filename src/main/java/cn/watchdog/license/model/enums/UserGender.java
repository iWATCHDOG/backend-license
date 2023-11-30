package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum UserGender {
	/**
	 * 男
	 */
	MALE(1),
	/**
	 * 女
	 */
	FEMALE(2),
	/**
	 * 保密
	 */
	UNKNOWN(3);

	final int code;

	UserGender(int code) {
		this.code = code;
	}

	public static UserGender valueOf(int code) {
		for (UserGender userGender : UserGender.values()) {
			if (userGender.code == code) {
				return userGender;
			}
		}
		return UNKNOWN;
	}
}
