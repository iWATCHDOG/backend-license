package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
	boolean userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request);

	boolean userLogout(HttpServletRequest request);

	User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	boolean checkDuplicates(String userName);
}
