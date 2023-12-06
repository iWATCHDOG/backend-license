package cn.watchdog.license.util.crypto;

import cn.watchdog.license.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Slf4j
public class AESUtil {

	private static final String AES = "AES";
	private static String key = null;

	/**
	 * AES加密
	 *
	 * @param data 要加密的数据
	 * @param key  密钥
	 * @return String 加密后的数据
	 */
	public static String encrypt(String data, String key) throws Exception {
		Cipher cipher = Cipher.getInstance(AES);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encrypted);
	}

	public static String encrypt(String data) throws Exception {
		if (key == null) {
			throw new Exception("AES密钥未初始化");
		}
		return encrypt(data, key);
	}

	/**
	 * AES解密
	 *
	 * @param data 要解密的数据
	 * @param key  密钥
	 * @return String 解密后的数据
	 */
	public static String decrypt(String data, String key) throws Exception {
		Cipher cipher = Cipher.getInstance(AES);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
		byte[] decoded = Base64.getDecoder().decode(data);
		byte[] decrypted = cipher.doFinal(decoded);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	public static String decrypt(String data) throws Exception {
		if (key == null) {
			throw new Exception("AES密钥未初始化");
		}
		return decrypt(data, key);
	}

	public static void init() {
		// 判断config目录是否存在AES密钥，不存在则创建
		Path config = Paths.get("config");
		Path aesKey = Paths.get("config", "aes.key");
		if (!Files.exists(aesKey)) {
			log.warn("检测到当前AES密钥不存在，正在创建AES密钥。");
			try {
				Files.createDirectories(config);
			} catch (IOException e) {
				log.error("创建AES密钥目录失败", e);
			}
			try {
				Files.createFile(aesKey);
			} catch (IOException e) {
				log.error("创建AES密钥文件失败", e);
			}
			// 写入密钥 加入时间戳 形成唯一且不可能重复的密钥
			String key = StringUtil.getRandomString(16) + UUID.randomUUID();
			// 随机取key的16位,开始位置和结束位置不定
			key = key.substring(0, 16);
			try {
				Files.write(aesKey, key.getBytes());
				AESUtil.key = key;
				log.info("AES密钥创建成功！");
				log.warn("请妥善保管AES密钥，密钥丢失将导致无法解密授权文件！");
			} catch (IOException e) {
				log.error("写入AES密钥失败", e);
			}
		} else {
			try {
				AESUtil.key = Files.readString(aesKey);
			} catch (IOException e) {
				log.error("读取AES密钥失败", e);
			}
		}
	}
}