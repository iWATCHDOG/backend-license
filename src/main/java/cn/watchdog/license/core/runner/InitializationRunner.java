package cn.watchdog.license.core.runner;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.PasswordUtil;
import cn.watchdog.license.util.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Slf4j
public class InitializationRunner implements ApplicationRunner {
	@Resource
	private UserService userService;
	@Resource
	private PermissionService permissionService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
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
			log.info("用户名：" + userName);
			log.info("密码：" + userPassword);
			log.info("邮箱：" + userEmail);
			log.warn("请网站管理员在登录后及时修改账户信息（用户名、密码、邮箱等）。");
			log.warn("此信息只显示一次，请妥善保管！");
			userService.generateDefaultAvatar(user, null);
			permissionService.addPermission(user.getUid(), "*", 0, null);
			try {
				Files.write(Paths.get("admin.txt"), String.format("用户名: %s\n密码: %s\n邮箱: %s\n请网站管理员在登录后及时修改账户信息（用户名、密码、邮箱等）。", userName, userPassword, userEmail).getBytes());
			} catch (IOException e) {
				log.error("创建管理员账户信息文件失败", e);
			}

		}
	}
}
