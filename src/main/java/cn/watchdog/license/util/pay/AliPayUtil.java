package cn.watchdog.license.util.pay;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.enums.pay.TradeStatus;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
@Slf4j
public class AliPayUtil {
	private final String domain;

	private final String appId;

	private final String privateKey;

	private String publicKey = null;

	public AliPayUtil(String domain, String appId) {
		this.domain = domain;
		this.appId = appId;
		boolean certificate = true;
		// 读取私钥和公钥
		// 私钥路径"./foo/alipay/private.txt"
		// 公钥路径"./foo/alipay/public.txt"
		// 从文件中读取字符串，赋值给privateKey和publicKey
		try {
			this.privateKey = new String(Files.readAllBytes(Paths.get("./data/alipay/privateKey.txt")));
			if (!certificate) {
				this.publicKey = new String(Files.readAllBytes(Paths.get("./data/alipay/publicKey.txt")));
			}
		} catch (IOException e) {
			log.error("读取支付宝公钥和私钥失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "读取支付宝公钥和私钥失败", null);
		}
		Factory.setOptions(getOptions(certificate));
	}

	public static TradeStatus getTradeStatus(String tradeStatus) {
		return switch (tradeStatus) {
			case "TRADE_SUCCESS" -> TradeStatus.SUCCESS;
			case "TRADE_CLOSED" -> TradeStatus.CLOSED;
			case "WAIT_BUYER_PAY" -> TradeStatus.NOTPAY;
			default -> TradeStatus.UNKNOWN;
		};
	}

	private Config getOptions(boolean certificate) {
		Config config = new Config();
		config.protocol = "https";
		config.gatewayHost = "openapi.alipay.com";
		config.signType = "RSA2";

		config.appId = appId;

		// 为避免私钥随源码泄露，推荐从文件中读取私钥字符串而不是写入源码中
		config.merchantPrivateKey = privateKey;

		if (certificate) {
			//注：证书文件路径支持设置为文件系统中的路径或CLASS_PATH中的路径，优先从文件系统中加载，加载失败后会继续尝试从CLASS_PATH中加载
			config.merchantCertPath = "./data/alipay/appCertPublicKey.crt";
			config.alipayCertPath = "./data/alipay/alipayCertPublicKey.crt";
			config.alipayRootCertPath = "./data/alipay/alipayRootCert.crt";
		} else {
			//注：如果采用非证书模式，则无需赋值上面的三个证书路径，改为赋值如下的支付宝公钥字符串即可
			config.alipayPublicKey = publicKey;
		}

		config.notifyUrl = domain + "/pay/alipay";

		return config;
	}
}
