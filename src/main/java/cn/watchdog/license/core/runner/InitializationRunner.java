package cn.watchdog.license.core.runner;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.ChartService;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.PasswordUtil;
import cn.watchdog.license.util.StringUtil;
import cn.watchdog.license.util.chart.ChartType;
import cn.watchdog.license.util.crypto.AESUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Slf4j
public class InitializationRunner implements ApplicationRunner {
	@Resource
	private UserService userService;
	@Resource
	private ChartService chartService;
	@Resource
	private PermissionService permissionService;

	public static void resetCellMaxTextLength() {
		SpreadsheetVersion excel2007 = SpreadsheetVersion.EXCEL2007;
		if (Integer.MAX_VALUE != excel2007.getMaxTextLength()) {
			Field field;
			try {
				// SpreadsheetVersion.EXCEL2007的_maxTextLength变量
				field = excel2007.getClass().getDeclaredField("_maxTextLength");
				// 关闭反射机制的安全检查，可以提高性能
				field.setAccessible(true);
				// 重新设置这个变量属性值
				field.set(excel2007, Integer.MAX_VALUE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void initChartData() {
		int days = 90;
		for (ChartType value : ChartType.values()) {
			chartService.getChartDataForType(value, days);
		}
	}

	@Override
	public void run(ApplicationArguments args) {
		AESUtil.init();
		init();
	}

	@Async
	public void init() {
		try {
			if (userService.init()) {
				log.warn("检测到当前用户数据库未存在账户，正在创建管理员账户。");
				String userName = "admin";
				String userEmail = "admin@qq.com";
				String userPassword = StringUtil.getRandomString(10);
				String encryptPassword = PasswordUtil.encodePassword(userPassword);
				User user = new User();
				user.setUsername(userName);
				user.setEmail(userEmail);
				user.setPassword(encryptPassword);
				boolean result = userService.save(user);
				if (!result) {
					throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", null);
				}
				log.info("管理员账户创建成功！");
				log.info("用户名：{}", userName);
				log.info("密码：{}", userPassword);
				log.info("邮箱：{}", userEmail);
				log.warn("请网站管理员在登录后及时修改账户信息（用户名、密码、邮箱等）。");
				log.warn("此信息只显示一次，请妥善保管！");
				userService.generateDefaultAvatar(user, null);
				permissionService.addPermission(user.getUid(), "*", 0, true, null);
				try {
					Files.write(Paths.get("data", "admin.txt"), String.format("用户名: %s\n密码: %s\n邮箱: %s\n请网站管理员在登录后及时修改账户信息（用户名、密码、邮箱等）。", userName, userPassword, userEmail).getBytes());
				} catch (IOException e) {
					log.error("创建管理员账户信息文件失败", e);
				}
			}
		} catch (Exception e) {
			log.error("初始化失败,数据库异常", e);
		}
		resetCellMaxTextLength();
		initChartData();
	}

}
