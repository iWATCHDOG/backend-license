package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum OAuthPlatForm {
	/**
	 * 微信
	 */
	WECHAT(1, "微信"),
	/**
	 * QQ
	 */
	QQ(2, "QQ"),
	/**
	 * GitHub
	 */
	GITHUB(3, "GitHub"),
	/**
	 * 支付宝
	 */
	ALIPAY(4, "支付宝");
	final int code;
	final String name;

	OAuthPlatForm(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public static OAuthPlatForm valueOf(int code) {
		for (OAuthPlatForm payPlatform : OAuthPlatForm.values()) {
			if (payPlatform.code == code) {
				return payPlatform;
			}
		}
		return null;
	}
}
