package cn.watchdog.license.service;

import cn.watchdog.license.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
	User getLoginUser(HttpServletRequest request);
}
