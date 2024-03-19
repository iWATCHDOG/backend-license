package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.OAuthMapper;
import cn.watchdog.license.mapper.UserMapper;
import cn.watchdog.license.model.dto.NotifyResponse;
import cn.watchdog.license.model.dto.user.UpdateUserPasswordRequest;
import cn.watchdog.license.model.dto.user.UserCreateRequest;
import cn.watchdog.license.model.dto.user.UserLoginRequest;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.NotifyType;
import cn.watchdog.license.model.enums.OAuthPlatForm;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.model.enums.UserGender;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.service.PhotoService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.NetUtil;
import cn.watchdog.license.util.NumberUtil;
import cn.watchdog.license.util.PasswordUtil;
import cn.watchdog.license.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.watchdog.license.constant.UserConstant.*;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	public static final Cache<String, String> codeCache = CaffeineFactory.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
	public static final Cache<String, User> forgetPasswordCache = CaffeineFactory.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
	public static final Cache<String, User> tokenCache = CaffeineFactory.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).build();
	// 在UserServiceImpl类中，创建一个新的Caffeine缓存
	private static final Cache<String, Integer> failLoginCache = CaffeineFactory.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
	private static final Cache<Long, User> userCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build();
	@Resource
	private OAuthMapper oAuthMapper;
	@Resource
	private PhotoService photoService;
	@Resource
	private SecurityLogService securityLogService;
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean userAdd(User user, HttpServletRequest request) {
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		String userName = user.getUsername();
		String userPassword = user.getPassword();
		validateUserCredentials(userName, userPassword, request);
		String email = user.getEmail();
		String phone = user.getPhone();
		// 邮箱和手机号不能同时为空
		if (StringUtils.isAllBlank(email, phone)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱和手机号不能同时为空", request);
		}
		user.setPassword(PasswordUtil.encodePassword(userPassword));
		if (!StringUtils.isAnyBlank(email)) {
			// 邮箱注册
			// 检查邮箱格式
			if (!email.matches("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱格式错误", request);
			}
		}
		if (!StringUtils.isAnyBlank(phone)) {
			// 手机号注册
			// 检查是否为中国大陆地区的手机号格式
			if (!phone.matches("^1[3-9]\\d{9}$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "手机号格式错误", request);
			}
		}
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		generateDefaultAvatar(user, request);
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		List<SecurityType> st = List.of(SecurityType.ADD_ACCOUNT);
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
		return true;
	}

	@Override
	public User userLoginToken(String token, HttpServletRequest request) {
		if (StringUtils.isBlank(token)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		User user = checkToken(token, request);
		setLoginState(user, request);
		return user;
	}

	@Override
	public User userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		if (userCreateRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		String userName = userCreateRequest.getUsername();
		String userPassword = userCreateRequest.getPassword();
		validateUserCredentials(userName, userPassword, request);
		String email = userCreateRequest.getEmail();
		String phone = userCreateRequest.getPhone();
		String code = userCreateRequest.getCode();
		// 邮箱和手机号不能同时为空
		if (StringUtils.isAllBlank(email, phone)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱和手机号不能同时为空", request);
		}
		User user = new User();
		user.setUsername(userName);
		user.setPassword(PasswordUtil.encodePassword(userPassword));
		// 检查验证码
		if (StringUtils.isBlank(code)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "验证码为空", request);
		}
		if (!StringUtils.isAnyBlank(email)) {
			// 邮箱注册
			// 检查邮箱格式
			if (!email.matches("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱格式错误", request);
			}
			user.setEmail(email);
			String icode = UserServiceImpl.codeCache.getIfPresent(email);
			if (icode == null || !icode.equals(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "验证码错误", code, request);
			}
			UserServiceImpl.codeCache.invalidate(email);
		} else {
			// 手机号注册
			// 检查是否为中国大陆地区的手机号格式
			if (!phone.matches("^1[3-9]\\d{9}$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "手机号格式错误", phone, request);
			}
			user.setPhone(phone);
			String icode = UserServiceImpl.codeCache.getIfPresent(phone);
			if (icode == null || !icode.equals(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "验证码错误", code, request);
			}
			UserServiceImpl.codeCache.invalidate(phone);
		}
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		generateDefaultAvatar(user, request);
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		List<SecurityType> st = List.of(SecurityType.ADD_ACCOUNT);
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
		return user;
	}

	private void validateUserCredentials(String userName, String userPassword, HttpServletRequest request) {
		if (StringUtils.isAnyBlank(userName, userPassword)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		if (checkDuplicates(userName, request)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号重复", userName, request);
		}
		// userName只能存在英文、数字、下划线、横杠、点，并且长度小于16
		if (!userName.matches("^[a-zA-Z0-9_-]{1,16}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号格式错误", userName, request);
		}
		// 检查密码不过分简单。密码必须包含大小写字母、数字、特殊符号中的三种，且长度为8-30位
		if (!userPassword.matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{8,30}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "密码格式错误", userPassword, request);
		}
	}

	/**
	 * 用户注销
	 */
	@Override
	public boolean userLogout(HttpServletRequest request) {
		if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录", request);
		}
		// 移除登录态
		request.getSession().removeAttribute(USER_LOGIN_STATE);
		// 使之前的token失效
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		tokenCache.invalidate(token);
		return true;
	}

	@Override
	public User oAuthLogin(AuthUser authUser, OAuthPlatForm oAuthPlatForm, HttpServletRequest request) {
		if (authUser == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		// log.info("用户： {}", GsonProvider.normal().toJson(authUser));
		String username = authUser.getUsername();
		AuthToken authToken = authUser.getToken();
		JSONObject rawUserInfo = authUser.getRawUserInfo();
		String avatar = authUser.getAvatar();
		String email = authUser.getEmail();
		String openId = rawUserInfo.getString("id");
		String token = authToken.getAccessToken();
		AuthUserGender authUserGender = authUser.getGender();
		UserGender userGender = UserGender.valueOf(authUserGender);
		// 判断是否已经绑定过
		QueryWrapper<OAuth> oAuthQueryWrapper = new QueryWrapper<>();
		oAuthQueryWrapper.eq("platform", oAuthPlatForm.getCode());
		if (checkIsLogin(request)) {
			// bind
			User user = getLoginUser(request);
			oAuthQueryWrapper.eq("uid", user.getUid());
			OAuth oAuth = oAuthMapper.selectOne(oAuthQueryWrapper);
			if (oAuth != null) {
				// 添加通知
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.WARNING);
				if (oAuthPlatForm == OAuthPlatForm.GITHUB) {
					notifyResponse.setTitle("GitHub账号绑定失败");
					notifyResponse.setContent("已绑定GitHub账号,请勿重复绑定");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "已绑定GitHub账号,请勿重复绑定", request);
				} else if (oAuthPlatForm == OAuthPlatForm.GITEE) {
					notifyResponse.setTitle("Gitee账号绑定失败");
					notifyResponse.setContent("已绑定Gitee账号,请勿重复绑定");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "已绑定Gitee账号,请勿重复绑定", request);
				} else if (oAuthPlatForm == OAuthPlatForm.MICROSOFT) {
					notifyResponse.setTitle("Microsoft账号绑定失败");
					notifyResponse.setContent("已绑定Microsoft账号,请勿重复绑定");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "已绑定Microsoft账号,请勿重复绑定", request);
				}
			}
			QueryWrapper<OAuth> oAuthQueryWrapper1 = new QueryWrapper<>();
			oAuthQueryWrapper1.eq("platform", oAuthPlatForm.getCode());
			oAuthQueryWrapper1.eq("openId", openId);
			oAuth = oAuthMapper.selectOne(oAuthQueryWrapper1);
			if (oAuth != null) {
				// 添加通知
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				if (oAuthPlatForm == OAuthPlatForm.GITHUB) {
					notifyResponse.setTitle("GitHub账号绑定失败");
					notifyResponse.setContent("该GitHub账号已绑定其他账号");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "该GitHub账号已绑定其他账号", request);
				} else if (oAuthPlatForm == OAuthPlatForm.GITEE) {
					notifyResponse.setTitle("Gitee账号绑定失败");
					notifyResponse.setContent("该Gitee账号已绑定其他账号");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "该Gitee账号已绑定其他账号", request);
				} else if (oAuthPlatForm == OAuthPlatForm.MICROSOFT) {
					notifyResponse.setTitle("Microsoft账号绑定失败");
					notifyResponse.setContent("该Microsoft账号已绑定其他账号");
					CommonConstant.addNotifyResponse(request, notifyResponse);
					throw new BusinessException(ReturnCode.OPERATION_ERROR, "该Microsoft账号已绑定其他账号", request);
				}
			}
			oAuth = new OAuth();
			oAuth.setUid(user.getUid());
			oAuth.setPlatform(oAuthPlatForm.getCode());
			oAuth.setOpenId(openId);
			oAuth.setToken(token);
			boolean saveResult = oAuthMapper.insert(oAuth) > 0;
			if (!saveResult) {
				throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
			}
			SecurityLog securityLog = new SecurityLog();
			securityLog.setUid(user.getUid());
			securityLog.setTitle(user.getUsername());
			List<SecurityType> st = new ArrayList<>();
			securityLog.setIp(NetUtil.getIpAddress(request));
			if (oAuthPlatForm == OAuthPlatForm.GITHUB) {
				securityLog.setInfo("GitHub ID: " + openId);
				st.add(SecurityType.BIND_GITHUB);
			} else if (oAuthPlatForm == OAuthPlatForm.GITEE) {
				securityLog.setInfo("Gitee ID: " + openId);
				st.add(SecurityType.BIND_GITEE);
			}
			securityLog.setTypesByList(st);
			securityLogService.save(securityLog);
			return user;
		}
		oAuthQueryWrapper.eq("openId", openId);
		OAuth oAuth = oAuthMapper.selectOne(oAuthQueryWrapper);
		if (oAuth != null) {
			// 已经绑定过，直接登录
			User user = this.getById(oAuth.getUid());
			if (user == null) {
				throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "账户信息不存在", request);
			}
			setLoginState(user, request);
			return user;
		}
		// 未绑定，创建账户
		User user = new User();
		username = generateUserName(username, oAuthPlatForm.name().toLowerCase(), request);
		user.setUsername(username);
		String randomPassword = StringUtil.getRandomString(10);
		user.setPassword(PasswordUtil.encodePassword(randomPassword));
		user.setEmail(email);
		user.setUserGender(userGender);
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		downloadAvatar(user, avatar, request);
		// 绑定账户
		oAuth = new OAuth();
		oAuth.setUid(user.getUid());
		oAuth.setPlatform(oAuthPlatForm.getCode());
		oAuth.setOpenId(String.valueOf(openId));
		oAuth.setToken(token);
		saveResult = oAuthMapper.insert(oAuth) > 0;
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		setLoginState(user, request, false);
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		List<SecurityType> st = List.of(SecurityType.ADD_ACCOUNT, SecurityType.BIND_GITHUB);
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(request));
		if (oAuthPlatForm == OAuthPlatForm.GITHUB) {
			securityLog.setInfo("GitHub ID: " + openId);
		} else if (oAuthPlatForm == OAuthPlatForm.GITEE) {
			securityLog.setInfo("Gitee ID: " + openId);
		} else if (oAuthPlatForm == OAuthPlatForm.MICROSOFT) {
			securityLog.setInfo("Microsoft ID: " + openId);
		}
		securityLogService.save(securityLog);
		return user;
	}

	@Override
	public User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		if (userLoginRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		User user = getLoginUserIgnoreError(request);
		if (user != null) {
			return user;
		}
		String account = userLoginRequest.getAccount();
		String password = userLoginRequest.getPassword();
		// 判断是否是邮箱登录
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		if (account.matches("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
			// 检查邮箱是否存在
			queryWrapper.eq("email", account);
		} else {
			// 手机号登录
			queryWrapper.eq("phone", account);
		}
		user = this.getOne(queryWrapper);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "账户信息不存在", request);
		}
		long uid = user.getUid();
		if (checkFailLogin(account, request)) {
			throw new BusinessException(ReturnCode.TOO_MANY_REQUESTS_ERROR, "登录失败次数过多，请稍后再试", request);
		}
		// 检查密码
		if (!PasswordUtil.checkPassword(password, user.getPassword())) {
			addFailLogin(account, request);
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "密码错误", request);
		}
		setLoginState(user, request);
		return user;
	}

	public void setLoginState(User user, HttpServletRequest request, boolean check) {
		if (check) {
			checkStatus(user, request);
		}
		// 清除之前的Token
		String oldToken = (String) request.getSession().getAttribute(LOGIN_TOKEN);
		if (StringUtils.isNotBlank(oldToken)) {
			tokenCache.invalidate(oldToken);
		}
		// 登录成功，设置登录态
		request.getSession().setAttribute(USER_LOGIN_STATE, user);
		// 生成Token
		String token = UUID.randomUUID().toString();
		tokenCache.put(token, user);
		request.getSession().setAttribute(LOGIN_TOKEN, token);
	}

	public void setLoginState(User user, HttpServletRequest request) {
		setLoginState(user, request, true);
	}

	public boolean checkIsLogin(HttpServletRequest request) {
		try {
			getLoginUser(request);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取当前登录用户
	 */
	@Override
	public User getLoginUser(HttpServletRequest request) {
		if (request == null) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "请求为null", null);
		}
		// 先判断是否已登录
		Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
		User currentUser = (User) userObj;
		if (currentUser == null || currentUser.getUid() == null) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录", request);
		}
		// 从数据库查询（追求性能的话可以注释，直接走缓存）
		long uid = currentUser.getUid();
		String oldPass = currentUser.getPassword();
		currentUser = this.getById(uid);
		if (currentUser == null || !currentUser.getPassword().equals(oldPass)) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录", request);
		}
		checkStatus(currentUser, request);
		request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);
		return currentUser;
	}

	@Override
	public User getLoginUserIgnoreError(HttpServletRequest request) {
		try {
			return getLoginUser(request);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean checkDuplicates(String userName, HttpServletRequest request) {
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("username", userName);
		long count = this.count(queryWrapper);
		if (count > 0) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号重复", userName, request);
		}
		return false;
	}

	@Override
	public boolean checkDuplicatesIgnoreError(String userName, HttpServletRequest request) {
		try {
			return checkDuplicates(userName, request);
		} catch (Exception e) {
			return true;
		}
	}

	@Override
	public String generateUserName(String login, String prefix, HttpServletRequest request) {
		String username;
		if (checkDuplicatesIgnoreError(login, request)) {
			do {
				// 随机生成用户名
				username = login + "_" + NumberUtil.getRandomCode(5);
				// 判断随机后的用户名是否符合规范
				if (!username.matches("^[a-zA-Z0-9_-]{1,16}$")) {
					// 随机生成新的用户名,取UUID的前6位
					UUID un = UUID.randomUUID();
					username = prefix + "_" + un.toString().substring(0, 6);
				}
				// 判断是否重复
			} while (checkDuplicatesIgnoreError(username, request));
		} else {
			username = login;
		}
		return username;
	}

	private void clearAvatar(User user, HttpServletRequest request) {
		if (user == null) {
			return;
		}
		String avatar = user.getAvatar();
		if (StringUtils.isBlank(avatar)) {
			return;
		}
		user.setAvatar(null);
		this.updateById(user);
	}

	@Override
	@Async
	public void downloadAvatar(User user, String avatarUrl, HttpServletRequest request) {
		clearAvatar(user, request);
		try {
			OkHttpClient client = new OkHttpClient();
			okhttp3.Request req = new okhttp3.Request.Builder().url(avatarUrl).build();
			okhttp3.Response res = client.newCall(req).execute();
			InputStream inputStream = Objects.requireNonNull(res.body()).byteStream();
			String ext = Objects.requireNonNull(res.body().contentType()).subtype();
			byte[] data = res.body().bytes();
			String md5 = DigestUtils.md5DigestAsHex(data);
			String fileName = md5 + "." + ext;
			photoService.savePhotoByMd5(md5, ext, data.length);
			Path path = Paths.get("photos", fileName);
			Files.createDirectories(path.getParent());
			Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
			user.setAvatar(md5);
			this.updateById(user);
			userCache.invalidate(user.getUid());
		} catch (IOException e) {
			// 添加通知
			NotifyResponse notifyResponse = new NotifyResponse();
			notifyResponse.setType(NotifyType.ERROR);
			notifyResponse.setTitle("头像下载失败");
			notifyResponse.setContent("头像下载失败，已使用默认头像");
			CommonConstant.addNotifyResponse(request, notifyResponse);
			generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to download avatar", avatarUrl, request);
		}
	}

	@Override
	public void setupAvatar(User user, MultipartFile file, HttpServletRequest request) {
		// 获取旧头像
		String oldAvatar = user.getAvatar();
		clearAvatar(user, request);
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ReturnCode.FORBIDDEN_ERROR, "图片为空", request);
		}
		try {
			// 获取图片的长度和宽度
			BufferedImage fi = ImageIO.read(file.getInputStream());
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			Thumbnails.of(file.getInputStream()).size(460, 460).toOutputStream(stream);
			// 获取图片类型拓展名
			String ext = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));
			byte[] data = file.getBytes();
			String md5 = DigestUtils.md5DigestAsHex(data);
			String fileName = md5 + ext;
			photoService.savePhotoByMd5(md5, ext, data.length);
			Path path = Paths.get("photos", fileName);
			Files.createDirectories(path.getParent());
			byte[] bytes = stream.toByteArray();
			Files.write(path, bytes);
			user.setAvatar(md5);
			this.updateById(user);
			userCache.invalidate(user.getUid());
		} catch (IOException e) {
			// 添加通知
			NotifyResponse notifyResponse = new NotifyResponse();
			notifyResponse.setType(NotifyType.ERROR);
			notifyResponse.setTitle("头像上传失败");
			notifyResponse.setContent("头像上传失败，已使用默认头像");
			CommonConstant.addNotifyResponse(request, notifyResponse);
			generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to upload avatar", request);
		}
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		List<SecurityType> st = List.of(SecurityType.CHANGE_AVATAR);
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(request));
		// 获取新头像
		String newAvatar = user.getAvatar();
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		if (StringUtils.isNotBlank(oldAvatar)) {
			// 有旧头像
			avatarData.add(new SecurityLog.AvatarData(2, oldAvatar));
		}
		avatarData.add(new SecurityLog.AvatarData(2, newAvatar));
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
	}

	@Override
	@Async
	public void generateDefaultAvatar(User user, HttpServletRequest request) {
		clearAvatar(user, request);
		String username = user.getUsername();
		Long uid = user.getUid();
		String character = username.chars().mapToObj(c -> (char) c).filter(Character::isLetterOrDigit).findFirst().map(String::valueOf).orElse(String.valueOf(uid % 10));

		int width = 460;
		int height = 460;

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		Random random = new Random();
		Color backgroundColor = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		g.setColor(backgroundColor);
		g.fillRect(0, 0, width, height);

		Color textColor = new Color(255 - backgroundColor.getRed(), 255 - backgroundColor.getGreen(), 255 - backgroundColor.getBlue());
		g.setColor(textColor);
		g.setFont(new Font("Arial", Font.BOLD, 200));
		FontMetrics fm = g.getFontMetrics();
		int x = (width - fm.stringWidth(character)) / 2;
		int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(character, x, y);

		g.dispose();

		try {
			String md5 = DigestUtils.md5DigestAsHex((character + textColor.getBlue() + textColor.getGreen() + textColor.getRed()).getBytes());
			String ext = "png";
			String fileName = md5 + "." + ext;
			photoService.savePhotoByMd5(md5, ext, 4300);
			Path path = Paths.get("photos", fileName);
			Files.createDirectories(path.getParent());
			ImageIO.write(image, "png", path.toFile());
			user.setAvatar(md5);
			this.updateById(user);
			userCache.invalidate(user.getUid());
		} catch (IOException e) {
			// 添加通知
			NotifyResponse notifyResponse = new NotifyResponse();
			notifyResponse.setType(NotifyType.ERROR);
			notifyResponse.setTitle("头像生成失败");
			notifyResponse.setContent("头像生成失败,请重试");
			CommonConstant.addNotifyResponse(request, notifyResponse);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "Failed to generate avatar", request);
		}
	}

	@Override
	public void unbind(OAuthPlatForm oAuthPlatForm, HttpServletRequest request) {
		if (oAuthPlatForm == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "Invalid code", request);
		}
		if (!checkIsLogin(request)) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录", request);
		}
		User user = getLoginUser(request);
		QueryWrapper<OAuth> oAuthQueryWrapper = new QueryWrapper<>();
		oAuthQueryWrapper.eq("uid", user.getUid());
		oAuthQueryWrapper.eq("platform", oAuthPlatForm.getCode());
		OAuth oAuth = oAuthMapper.selectOne(oAuthQueryWrapper);
		if (oAuth == null) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "未绑定该账号", request);
		}
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		if (oAuthPlatForm == OAuthPlatForm.GITHUB) {
			securityLog.setTypesByList(List.of(SecurityType.UNBIND_GITHUB));
		} else if (oAuthPlatForm == OAuthPlatForm.GITEE) {
			securityLog.setTypesByList(List.of(SecurityType.UNBIND_GITEE));
		}
		securityLog.setInfo(String.valueOf(oAuth.getOpenId()));
		securityLog.setInfo(oAuthPlatForm.getName() + " ID: " + oAuth.getOpenId());
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
		oAuthMapper.deleteById(oAuth.getId());
	}

	@Override
	public boolean checkStatus(User user, HttpServletRequest request) {
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		if (user.getStatus() == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		int status = user.getStatus();
		UserStatus userStatus = UserStatus.valueOf(status);
		if (userStatus == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", status, request);
		}
		if (userStatus == UserStatus.DELETED) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账户已删除", status, request);
		}
		if (userStatus == UserStatus.BANNED) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账户已禁用", status, request);
		}

		return true;
	}

	public User checkToken(String token, HttpServletRequest request) {
		// 检查token有效性
		User tokenUser = tokenCache.getIfPresent(token);
		if (tokenUser == null) {
			// 添加通知
			NotifyResponse notifyResponse = new NotifyResponse();
			notifyResponse.setType(NotifyType.WARNING);
			notifyResponse.setTitle("登录状态已失效");
			notifyResponse.setContent("登录状态已失效，请重新登录");
			CommonConstant.addNotifyResponse(request, notifyResponse);
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "信息已过期，请重新登录", request);
		}
		// 检查tokenUser密码是否和数据库中的密码相同
		String oldPass = tokenUser.getPassword();
		tokenUser = this.getById(tokenUser.getUid());
		if (tokenUser == null || !tokenUser.getPassword().equals(oldPass)) {
			// 添加通知
			NotifyResponse notifyResponse = new NotifyResponse();
			notifyResponse.setType(NotifyType.WARNING);
			notifyResponse.setTitle("登录状态已失效");
			notifyResponse.setContent("登录状态已失效，请重新登录");
			CommonConstant.addNotifyResponse(request, notifyResponse);
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "信息已过期，请重新登录", request);
		}
		return tokenUser;
	}

	@Override
	public boolean init() {
		// 判定uid为0的记录是否存在
		String sql = "SELECT COUNT(*) FROM user WHERE uid=1";
		String obj;
		try {
			obj = jdbcTemplate.queryForObject(sql, String.class);
		} catch (EmptyResultDataAccessException e) {
			obj = null;
		}
		return obj == null || Integer.parseInt(obj) == 0;
	}

	// 检查用户的UID和IP地址是否在缓存中
	public boolean checkFailLogin(String account, HttpServletRequest request) {
		String ip = NetUtil.getIpAddress(request);
		Integer accountFailCount = failLoginCache.getIfPresent(account);
		Integer ipFailCount = failLoginCache.getIfPresent(ip);
		return (accountFailCount != null && accountFailCount >= 5) || (ipFailCount != null && ipFailCount >= 5);
	}

	// 将用户的UID和IP地址添加到缓存中
	public void addFailLogin(String account, HttpServletRequest request) {
		String ip = NetUtil.getIpAddress(request);
		Integer accountFailCount = failLoginCache.getIfPresent(account);
		failLoginCache.put(account, accountFailCount == null ? 1 : accountFailCount + 1);
		Integer ipFailCount = failLoginCache.getIfPresent(ip);
		failLoginCache.put(ip, ipFailCount == null ? 1 : ipFailCount + 1);
	}

	@Override
	public boolean checkEmail(String email, HttpServletRequest request) {
		if (StringUtils.isBlank(email)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱为空", request);
		}
		if (!email.matches("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱格式错误", request);
		}
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("email", email);
		long count = this.count(queryWrapper);
		if (count > 0) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱已存在", email, request);
		}
		return true;
	}

	@Override
	public User getByEmail(String email, HttpServletRequest request) {
		if (StringUtils.isBlank(email)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱为空", request);
		}
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("email", email);
		return this.getOne(queryWrapper);
	}

	@Override
	public void updatePassword(@NotNull User user, @NotNull String password, HttpServletRequest request) {
		if (!password.matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{8,30}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "密码格式错误", password, request);
		}
		// 判断修改的密码是否和原密码相同
		if (PasswordUtil.checkPassword(password, user.getPassword())) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "新密码不能和原密码相同", request);
		}
		user.setPassword(PasswordUtil.encodePassword(password));
		this.updateById(user);
		userCache.invalidate(user.getUid());
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		securityLog.setTypesByList(List.of(SecurityType.CHANGE_PASSWORD_FORGET));
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
	}

	@Override
	public void updatePassword(UpdateUserPasswordRequest updateUserPasswordRequest, HttpServletRequest request) {
		if (updateUserPasswordRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		String oldPassword = updateUserPasswordRequest.getOldPassword();
		String newPassword = updateUserPasswordRequest.getNewPassword();
		if (StringUtils.isAnyBlank(oldPassword, newPassword)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		if (!newPassword.matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{8,30}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "密码格式错误", newPassword, request);
		}
		User user = getLoginUser(request);
		if (!PasswordUtil.checkPassword(oldPassword, user.getPassword())) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "原密码错误", request);
		}
		// 判断修改的密码是否和原密码相同
		if (PasswordUtil.checkPassword(newPassword, user.getPassword())) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "新密码不能和原密码相同", request);
		}
		user.setPassword(PasswordUtil.encodePassword(newPassword));
		this.updateById(user);
		userCache.invalidate(user.getUid());
		// 使之前的token失效
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		tokenCache.invalidate(token);
		// 使之前的登录态失效
		request.getSession().removeAttribute(USER_LOGIN_STATE);
		request.getSession().removeAttribute(LOGIN_TOKEN);
		// 添加通知
		NotifyResponse notifyResponse = new NotifyResponse();
		notifyResponse.setType(NotifyType.WARNING);
		notifyResponse.setTitle("登录状态已失效");
		notifyResponse.setContent("登录状态已失效，请重新登录");
		CommonConstant.addNotifyResponse(request, notifyResponse);
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		securityLog.setTypesByList(List.of(SecurityType.CHANGE_PASSWORD));
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
	}

	@Override
	public OAuth getOAuthByUidAndPlatform(@NotNull User user, @NotNull OAuthPlatForm oAuthPlatForm, HttpServletRequest request) {
		long uid = user.getUid();
		QueryWrapper<OAuth> oAuthQueryWrapper = new QueryWrapper<>();
		oAuthQueryWrapper.eq("uid", uid);
		oAuthQueryWrapper.eq("platform", oAuthPlatForm.getCode());
		return oAuthMapper.selectOne(oAuthQueryWrapper);
	}

	@Override
	public void clearOAuthByUser(@NotNull User user, HttpServletRequest request) {
		long uid = user.getUid();
		QueryWrapper<OAuth> oAuthQueryWrapper = new QueryWrapper<>();
		oAuthQueryWrapper.eq("uid", uid);
		oAuthMapper.delete(oAuthQueryWrapper);
	}

	@Override
	public User getUserByCache(Long uid, HttpServletRequest request) {
		if (uid == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		User user = userCache.getIfPresent(uid);
		if (user == null) {
			user = this.getById(uid);
			if (user != null) {
				userCache.put(uid, user);
			}
		}
		return user;
	}

	@Override
	public User getByUsername(String username, HttpServletRequest request) {
		if (StringUtils.isBlank(username)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("username", username);
		return this.getOne(queryWrapper);
	}
}
