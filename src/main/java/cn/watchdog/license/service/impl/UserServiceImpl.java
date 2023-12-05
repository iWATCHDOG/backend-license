package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.OAuthMapper;
import cn.watchdog.license.mapper.UserMapper;
import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.OAuthPlatForm;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.NumberUtil;
import cn.watchdog.license.util.PasswordUtil;
import cn.watchdog.license.util.oauth.GithubUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.watchdog.license.constant.UserConstant.*;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	private static final Cache<String, String> emailCode = CaffeineFactory.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
	@Resource
	private UserMapper userMapper;
	@Resource
	private OAuthMapper oAuthMapper;
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
			if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
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
		return true;
	}


	@Override
	public boolean userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
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
		if (!StringUtils.isAnyBlank(email)) {
			// 邮箱注册
			// 检查邮箱格式
			if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱格式错误", email, request);
			}
			// 检查邮箱验证码
			if (StringUtils.isBlank(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱验证码为空", request);
			}
			String emailCodeCache = emailCode.getIfPresent(email);
			if (emailCodeCache == null || !emailCodeCache.equals(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱验证码错误", code, request);
			}
			user.setEmail(email);
			emailCode.invalidate(email);
		} else {
			// 手机号注册
			// 检查是否为中国大陆地区的手机号格式
			if (!phone.matches("^1[3-9]\\d{9}$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "手机号格式错误", phone, request);
			}
			user.setPhone(phone);
		}
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		generateDefaultAvatar(user, request);
		return true;
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
		// 检查密码不过分简单。大小写字母、数字、特殊符号中至少包含两个，且长度大于8小于30。
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
		return true;
	}

	@Override
	public User oAuthLogin(GithubUser githubUser, HttpServletRequest request) {
		if (githubUser == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		OAuthPlatForm oAuthPlatForm = OAuthPlatForm.GITHUB;
		String login = githubUser.getLogin();
		long id = githubUser.getId();
		String node_id = githubUser.getNode_id();
		String avatar_url = githubUser.getAvatar_url();
		String email = githubUser.getEmail();
		// 判断是否已经绑定过
		QueryWrapper<OAuth> oAuthQueryWrapper = new QueryWrapper<>();
		oAuthQueryWrapper.eq("platform", oAuthPlatForm.getCode());
		if (checkIsLogin(request)) {
			// bind
			User user = getLoginUser(request);
			oAuthQueryWrapper.eq("uid", user.getUid());
			OAuth oAuth = oAuthMapper.selectOne(oAuthQueryWrapper);
			if (oAuth != null) {
				throw new BusinessException(ReturnCode.OPERATION_ERROR, "已绑定GitHub账号,请勿重复绑定", request);
			}
			QueryWrapper<OAuth> oAuthQueryWrapper1 = new QueryWrapper<>();
			oAuthQueryWrapper1.eq("platform", oAuthPlatForm.getCode());
			oAuthQueryWrapper1.eq("openId", id);
			oAuth = oAuthMapper.selectOne(oAuthQueryWrapper1);
			if (oAuth != null) {
				throw new BusinessException(ReturnCode.OPERATION_ERROR, "该GitHub账号已绑定其他账号", request);
			}
			oAuth = new OAuth();
			oAuth.setUid(user.getUid());
			oAuth.setPlatform(oAuthPlatForm.getCode());
			oAuth.setOpenId(String.valueOf(id));
			oAuth.setToken(node_id);
			boolean saveResult = oAuthMapper.insert(oAuth) > 0;
			if (!saveResult) {
				throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
			}
			return user;
		}
		oAuthQueryWrapper.eq("openId", id);
		OAuth oAuth = oAuthMapper.selectOne(oAuthQueryWrapper);
		if (oAuth != null) {
			// 已经绑定过，直接登录
			User user = userMapper.selectById(oAuth.getUid());
			if (user == null) {
				throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "账户信息不存在", request);
			}
			checkStatus(user, request);
			// 登录成功，设置登录态
			request.getSession().setAttribute(USER_LOGIN_STATE, user);
			return user;
		}
		// 未绑定，创建账户
		User user = new User();
		String username = generateUserName(login, oAuthPlatForm.name().toLowerCase(), request);
		user.setUsername(username);
		UUID uuid = UUID.randomUUID();
		user.setPassword(PasswordUtil.encodePassword(uuid.toString()));
		user.setEmail(email);
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		downloadAvatar(user, avatar_url, request);
		// 绑定账户
		oAuth = new OAuth();
		oAuth.setUid(user.getUid());
		oAuth.setPlatform(oAuthPlatForm.getCode());
		oAuth.setOpenId(String.valueOf(id));
		oAuth.setToken(node_id);
		saveResult = oAuthMapper.insert(oAuth) > 0;
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
		}
		// 登录成功，设置登录态
		request.getSession().setAttribute(USER_LOGIN_STATE, user);
		return user;
	}

	@Override
	public User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		if (userLoginRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空", request);
		}
		if (checkIsLogin(request)) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "已登录", request);
		}
		User user;
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
		user = userMapper.selectOne(queryWrapper);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "账户信息不存在", request);
		}
		// 检查密码
		if (!PasswordUtil.checkPassword(password, user.getPassword())) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "密码错误", request);
		}
		checkStatus(user, request);
		// 登录成功，设置登录态
		request.getSession().setAttribute(USER_LOGIN_STATE, user);
		return user;
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
		long count = userMapper.selectCount(queryWrapper);
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
		Path path = Paths.get(avatar);
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.error("Failed to delete avatar: {}", avatar);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "Failed to delete avatar", request);
		}
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
			String fileName = "avatar." + ext;
			Path path = Paths.get("avatars", String.valueOf(user.getUid()), fileName);
			Files.createDirectories(path.getParent());
			Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
			user.setAvatar(path.toString());
			userMapper.updateById(user);
		} catch (IOException e) {
			generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to download avatar", avatarUrl, request);
		}
	}

	@Override
	public void setupAvatar(User user, MultipartFile file, HttpServletRequest request) {
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
			String fileName = "avatar" + ext;
			Path path = Paths.get("avatars", String.valueOf(user.getUid()), fileName);
			Files.createDirectories(path.getParent());
			byte[] bytes = stream.toByteArray();
			Files.write(path, bytes);
			user.setAvatar(path.toString());
			userMapper.updateById(user);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to upload avatar", request);
		}
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
			String fileName = "avatar.png";
			Path path = Paths.get("avatars", String.valueOf(user.getUid()), fileName);
			Files.createDirectories(path.getParent());
			ImageIO.write(image, "png", path.toFile());
			user.setAvatar(path.toString());
			userMapper.updateById(user);
		} catch (IOException e) {
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
}
