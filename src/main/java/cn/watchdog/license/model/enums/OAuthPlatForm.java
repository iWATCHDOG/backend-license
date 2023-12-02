package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum OAuthPlatForm {
	/**
	 * 微信
	 */
	WECHAT(1),
	/**
	 * QQ
	 */
	QQ(2),
	/**
	 * GitHub
	 */
	GITHUB(3),
	/**
	 * 支付宝
	 */
	ALIPAY(4);
	final int code;

	OAuthPlatForm(int code) {
		this.code = code;
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
