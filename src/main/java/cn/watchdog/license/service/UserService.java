package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.user.UserCreateRequest;
import cn.watchdog.license.model.dto.user.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.OAuthPlatForm;
import cn.watchdog.license.util.oauth.GithubUser;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
	boolean userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request);

	boolean userAdd(User user, HttpServletRequest request);

	boolean userLogout(HttpServletRequest request);

	User oAuthLogin(GithubUser githubUser, HttpServletRequest request);

	User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	User getLoginUserIgnoreError(HttpServletRequest request);

	User getLoginUserIgnoreErrorCache(HttpServletRequest request);

	boolean checkDuplicates(String userName, HttpServletRequest request);

	boolean checkDuplicatesIgnoreError(String userName, HttpServletRequest request);

	String generateUserName(String login, String prefix, HttpServletRequest request);

	@Async
	void downloadAvatar(User user, String avatarUrl, HttpServletRequest request);

	void setupAvatar(User user, MultipartFile file, HttpServletRequest request);

	@Async
	void generateDefaultAvatar(User user, HttpServletRequest request);

	void unbind(OAuthPlatForm oAuthPlatForm, HttpServletRequest request);

	boolean checkStatus(User user, HttpServletRequest request);

	boolean init();

	boolean checkEmail(String email, HttpServletRequest request);

	User getByEmail(String email, HttpServletRequest request);
}
