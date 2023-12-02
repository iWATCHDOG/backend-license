package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.util.oauth.GithubUser;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;

public interface UserService extends IService<User> {
	boolean userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request);

	boolean userLogout(HttpServletRequest request);

	User oAuthLogin(GithubUser githubUser, HttpServletRequest request);

	User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	boolean checkDuplicates(String userName);

	boolean checkDuplicatesIgnoreError(String userName);

	String generateUserName(String login, String prefix);

	@Async
	void downloadAvatar(User user, String avatarUrl);
}
