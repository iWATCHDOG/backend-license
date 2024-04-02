package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.user.UpdateUserPasswordRequest;
import cn.watchdog.license.model.dto.user.UserCreateRequest;
import cn.watchdog.license.model.dto.user.UserLoginRequest;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.OAuthPlatForm;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import me.zhyd.oauth.model.AuthUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
	User userLoginToken(String token, HttpServletRequest request);

	User userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request);

	boolean userAdd(User user, HttpServletRequest request);

	boolean userLogout(HttpServletRequest request);

	User oAuthLogin(AuthUser authUser, OAuthPlatForm oAuthPlatForm, HttpServletRequest request);

	User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	User getLoginUserIgnoreError(HttpServletRequest request);

	boolean checkDuplicates(String userName, HttpServletRequest request);

	boolean checkDuplicatesIgnoreError(String userName, HttpServletRequest request);

	String generateUserName(String login, String prefix, HttpServletRequest request);

	@Async
	void downloadAvatar(User user, String avatarUrl, HttpServletRequest request);

	void setupAvatar(User user, MultipartFile file, HttpServletRequest request);

	@Async
	void generateDefaultAvatar(User user, HttpServletRequest request);

	@Async
	void generateDefaultAvatar(long uid, HttpServletRequest request);


	void unbind(OAuthPlatForm oAuthPlatForm, HttpServletRequest request);

	boolean checkStatus(User user, HttpServletRequest request);

	boolean init();

	boolean checkEmail(String email, HttpServletRequest request);

	User getByEmail(String email, HttpServletRequest request);

	void updatePassword(@NotNull User user, @NotNull String password, HttpServletRequest request);

	void updatePassword(UpdateUserPasswordRequest updateUserPasswordRequest, HttpServletRequest request);

	OAuth getOAuthByUidAndPlatform(@NotNull User user, @NotNull OAuthPlatForm oAuthPlatForm, HttpServletRequest request);

	void clearOAuthByUser(@NotNull User user, HttpServletRequest request);

	User getUserByCache(Long uid, HttpServletRequest request);

	User getByUsername(String username, HttpServletRequest request);
}
