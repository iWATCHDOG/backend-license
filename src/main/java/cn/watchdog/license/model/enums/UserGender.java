package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum UserGender {
	/**
	 * 男
	 */
	MALE(1, "男"),
	/**
	 * 女
	 */
	FEMALE(2, "女"),
	/**
	 * 保密
	 */
	UNKNOWN(3, "保密");

	final int code;
	final String name;

	UserGender(int code, String name) {
		this.code = code;
		this.name = name;
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
