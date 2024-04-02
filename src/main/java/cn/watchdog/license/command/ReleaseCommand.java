package cn.watchdog.license.command;

import cn.watchdog.license.BackendLicenseApplication;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
public class ReleaseCommand {
	public static void init() {
		createDirectory("data", "download");
		createDirectory("data", "download", "user");
		createDirectory("data", "download", "log");
		createDirectory("data", "download", "blacklist");
		createDirectory("data", "download", "permission");
		createDirectory("data", "download", "security-log");
	}

	public static void release() {
		log.warn("Start checking and releasing resource files...");
		createDirectory("config");
		createDirectory("data");
		releaseConfigFile("application.yml");
		releaseConfigFile("application.properties");
		log.info("Resource files checked and released.");
		log.warn("Start checking and releasing payment profiles.");
		createDirectory("data", "alipay");
		createDirectory("data", "wechat");
		init();
		log.info("Payment folder released.");
		log.info("Payment profiles checked and released.");
		log.warn("Start releasing payment configuration empty file");
		createEmptyFile("data", "alipay", "alipayCertPublicKey.crt");
		createEmptyFile("data", "alipay", "alipayRootCert.crt");
		createEmptyFile("data", "alipay", "appCertPublicKey.crt");
		createEmptyFile("data", "alipay", "privateKey.txt");
		createEmptyFile("data", "alipay", "publicKey.txt");
		createEmptyFile("data", "wechat", "apiclient_key.pem");
		log.info("Empty payment profile created.");
	}

	public static void createDirectory(String... paths) {
		Path path = Paths.get("", paths);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			log.error("Failed to create directory: " + path, e);
		}
	}

	private static void releaseConfigFile(String file) {
		Path path = Paths.get("config", file);
		if (!Files.exists(path)) {
			log.warn("Start releasing the file: " + path);
			try {
				Files.copy(Objects.requireNonNull(BackendLicenseApplication.class.getClassLoader().getResourceAsStream(file)), path);
			} catch (IOException e) {
				log.error("Failed to copy file: " + path, e);
			}
			log.info(path + " released.");
		}
	}

	private static void createEmptyFile(String... paths) {
		Path path = Paths.get("", paths);
		try {
			Files.createFile(path);
		} catch (IOException e) {
			log.error("Failed to create an empty file: " + path, e);
		}
	}
}
