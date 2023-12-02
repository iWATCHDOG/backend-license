package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.UserMapper;
import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static cn.watchdog.license.constant.UserConstant.*;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	private static final Cache<String, String> emailCode = CaffeineFactory.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	@Resource
	private UserMapper userMapper;

	@Override
	public boolean userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		if (userCreateRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空");
		}
		String userName = userCreateRequest.getUsername();
		String userPassword = userCreateRequest.getPassword();
		if (StringUtils.isAnyBlank(userName, userPassword)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空");
		}
		if (checkDuplicates(userName)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号重复");
		}
		// userName只能存在英文、数字、下划线、横杠、点，并且长度小于16
		if (!userName.matches("^[a-zA-Z0-9_-]{1,16}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号格式错误");
		}
		// 检查密码不过分简单。大小写字母、数字、特殊符号中至少包含两个，且长度大于8小于30。
		if (!userCreateRequest.getPassword().matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{8,30}$")) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "密码格式错误");
		}
		String email = userCreateRequest.getEmail();
		String phone = userCreateRequest.getPhone();
		String code = userCreateRequest.getCode();
		// 邮箱和手机号不能同时为空
		if (StringUtils.isAllBlank(email, phone)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱和手机号不能同时为空");
		}
		User user = new User();
		user.setUsername(userName);
		user.setPassword(PasswordUtil.encodePassword(userCreateRequest.getPassword()));
		if (!StringUtils.isAnyBlank(email)) {
			// 邮箱注册
			// 检查邮箱格式
			if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱格式错误");
			}
			// 检查邮箱验证码
			if (StringUtils.isBlank(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱验证码为空");
			}
			String emailCodeCache = emailCode.getIfPresent(email);
			if (emailCodeCache == null || !emailCodeCache.equals(code)) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱验证码错误");
			}
			user.setEmail(email);
			emailCode.invalidate(email);
		} else {
			// 手机号注册
			// 检查是否为中国大陆地区的手机号格式
			if (!phone.matches("^1[3-9]\\d{9}$")) {
				throw new BusinessException(ReturnCode.PARAMS_ERROR, "手机号格式错误");
			}
			user.setPhone(phone);
		}
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误");
		}
		return true;
	}

	/**
	 * 用户注销
	 */
	@Override
	public boolean userLogout(HttpServletRequest request) {
		if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录");
		}
		// 移除登录态
		request.getSession().removeAttribute(USER_LOGIN_STATE);
		return true;
	}

	@Override
	public User userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		if (userLoginRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数为空");
		}
		if (checkIsLogin(request)) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "已登录");
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
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "账户信息不存在");
		}
		// 检查密码
		if (!PasswordUtil.checkPassword(password, user.getPassword())) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "密码错误");
		}
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
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录");
		}
		// 从数据库查询（追求性能的话可以注释，直接走缓存）
		long uid = currentUser.getUid();
		String oldPass = currentUser.getPassword();
		currentUser = this.getById(uid);
		if (currentUser == null || !currentUser.getPassword().equals(oldPass)) {
			throw new BusinessException(ReturnCode.NOT_LOGIN_ERROR, "未登录");
		}

		request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);
		return currentUser;
	}


	@Override
	public boolean checkDuplicates(String userName) {
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("username", userName);
		long count = userMapper.selectCount(queryWrapper);
		if (count > 0) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "账号重复");
		}
		return false;
	}
}
