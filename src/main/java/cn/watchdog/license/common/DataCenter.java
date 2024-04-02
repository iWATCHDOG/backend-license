package cn.watchdog.license.common;

import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.util.CaffeineFactory;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class DataCenter {
	public static final Cache<String, List<User>> userCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();

	public static final Cache<String, List<Log>> logCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();

	public static final Cache<String, List<SecurityLog>> securityLogCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();

	public static final Cache<String, List<Permission>> permissionCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();

	public static final Cache<String, List<Blacklist>> blacklistCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();


	public static String getKey() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
		return now.format(formatter);
	}
}
