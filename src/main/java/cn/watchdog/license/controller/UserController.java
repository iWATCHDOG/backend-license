package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.common.StatusCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.CaptchaResult;
import cn.watchdog.license.model.dto.user.UpdateUserPasswordRequest;
import cn.watchdog.license.model.dto.user.UpdateUserProfileRequest;
import cn.watchdog.license.model.dto.user.UserCreateRequest;
import cn.watchdog.license.model.dto.user.UserLoginRequest;
import cn.watchdog.license.model.dto.user.UserSecurityLogQueryRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.model.enums.UserGender;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.model.vo.SecurityLogVO;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.MailService;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.PhotoService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.service.impl.UserServiceImpl;
import cn.watchdog.license.util.NetUtil;
import cn.watchdog.license.util.StringUtil;
import cn.watchdog.license.util.captcha.TencentCaptchaUtil;
import cn.watchdog.license.util.gson.GsonProvider;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static cn.watchdog.license.constant.CommonConstant.*;
import static cn.watchdog.license.constant.UserConstant.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
	public static TencentCaptchaUtil TENCENT_CAPTCHA_UTIL = null;
	@Resource
	private UserService userService;
	@Resource
	private PermissionService permissionService;
	@Resource
	private SecurityLogService securityLogService;
	@Resource
	private PhotoService photoService;
	@Resource
	private MailService mailService;
	@Value("${tencent.secret-id}")
	private String tencentSecretId;
	@Value("${tencent.secret-key}")
	private String tencentSecretKey;
	@Value("${captcha.app-id}")
	private String captchaAppId;
	@Value("${captcha.secret-key}")
	private String captchaAppSecretKey;
	@Value("${captcha.enable}")
	private boolean captchaEnable;

	public void checkCaptcha(HttpServletRequest request) {
		if (!captchaEnable) {
			return;
		}
		if (TENCENT_CAPTCHA_UTIL == null) {
			// init
			TENCENT_CAPTCHA_UTIL = new TencentCaptchaUtil(tencentSecretId, tencentSecretKey, captchaAppSecretKey);
		}
		// 获取Header里的captcha
		String captcha = request.getHeader(CAPTCHA_HEADER);
		// 将captcha转换为CaptchaResult
		CaptchaResult captchaResult = GsonProvider.normal().fromJson(captcha, CaptchaResult.class);
		if (captchaResult == null) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "请进行人机验证", request);
		}
		// 验证captcha
		try {
			TENCENT_CAPTCHA_UTIL.isCaptchaValid(captchaResult, Long.parseLong(captchaAppId), request);
		} catch (TencentCloudSDKException e) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, e.getMessage(), request);
		}
	}

	@PostMapping("/create")
	public ResponseEntity<BaseResponse<UserVO>> userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		checkCaptcha(request);
		User user = userService.userCreate(userCreateRequest, request);
		UserVO userVO = user.toUserVO();
		// 获取Token
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		userVO.setToken(token);
		long uid = user.getUid();
		Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
		userVO.setGroup(ret);
		return ResultUtil.ok(userVO);
	}

	@PostMapping("create/email")
	public ResponseEntity<BaseResponse<Boolean>> userCreateEmail(String email, HttpServletRequest request) {
		checkCaptcha(request);
		boolean check = userService.checkEmail(email, request);
		if (!check) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "邮箱已被注册", email, request);
		}
		String code = StringUtil.getRandomString(6);
		mailService.getEmailCode(email, code);
		UserServiceImpl.codeCache.put(email, code);
		return ResultUtil.ok(true);
	}

	@PostMapping("forget/email")
	public ResponseEntity<BaseResponse<String>> forgetPasswordEmail(String email, HttpServletRequest request) {
		checkCaptcha(request);
		User user = userService.getByEmail(email, request);
		if (user != null) {
			String token = UUID.randomUUID().toString();
			mailService.forgetPassword(email, token);
			UserServiceImpl.forgetPasswordCache.put(token, user);
		}
		return ResultUtil.ok(email);
	}

	@PostMapping("forget/password")
	public ResponseEntity<BaseResponse<Boolean>> forgetPassword(String password, HttpServletRequest request) {
		checkCaptcha(request);
		String token = request.getHeader(FORGET_TOKEN);
		if (StringUtils.isAllBlank(token, password)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		User user = UserServiceImpl.forgetPasswordCache.getIfPresent(token);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "token无效", token, request);
		}
		userService.updatePassword(user, password, request);
		UserServiceImpl.forgetPasswordCache.invalidate(token);
		return ResultUtil.ok(true);
	}

	@GetMapping("check/forget")
	public ResponseEntity<BaseResponse<String>> checkForgetPasswordToken(String token, HttpServletRequest request) {
		User user = UserServiceImpl.forgetPasswordCache.getIfPresent(token);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "token无效", token, request);
		}
		return ResultUtil.ok(user.getEmail());
	}

	@GetMapping("/login")
	public ResponseEntity<BaseResponse<UserVO>> userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		checkCaptcha(request);
		User user = userService.userLogin(userLoginRequest, request);
		UserVO userVO = user.toUserVO();
		// 获取Token
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		userVO.setToken(token);
		long uid = user.getUid();
		Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
		userVO.setGroup(ret);
		return ResultUtil.ok(userVO);
	}

	@GetMapping("/login/token")
	public ResponseEntity<BaseResponse<UserVO>> userLoginToken(HttpServletRequest request) {
		// 获取header里的token
		String token = request.getHeader(LOGIN_TOKEN);
		User user = userService.userLoginToken(token, request);
		UserVO userVO = user.toUserVO();
		// 获取Token
		String t = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		userVO.setToken(t);
		long uid = user.getUid();
		Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
		userVO.setGroup(ret);
		return ResultUtil.ok(userVO);
	}

	/**
	 * 用户注销
	 */
	@PostMapping("/logout")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> userLogout(HttpServletRequest request) {
		boolean result = userService.userLogout(request);
		return ResultUtil.ok(result);
	}


	/**
	 * 获取当前登录用户
	 */
	@GetMapping("/get/login")
	public ResponseEntity<BaseResponse<UserVO>> getLoginUser(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		UserVO userVO = user.toUserVO();
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		userVO.setToken(token);
		long uid = user.getUid();
		Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
		userVO.setGroup(ret);
		return ResultUtil.ok(userVO);
	}

	/**
	 * 上传头像
	 */
	@PostMapping("/upload/avatar")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> uploadAvatar(@RequestBody MultipartFile avatar, HttpServletRequest request) {
		// 获取所有Request Body
		User user = userService.getLoginUser(request);
		userService.setupAvatar(user, avatar, request);
		return ResultUtil.ok(true);
	}

	/**
	 * 更新用户信息
	 */
	@PostMapping("/update/profile")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		String username = updateUserProfileRequest.getUsername();
		Integer gender = updateUserProfileRequest.getGender();
		boolean update = false;
		SecurityLog securityLog = new SecurityLog();
		StringBuilder info = new StringBuilder();
		if (StringUtils.isNotBlank(username)) {
			if (!username.equals(user.getUsername())) {
				if (!username.matches("^[a-zA-Z0-9_-]{1,16}$")) {
					throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户名格式错误", request);
				}
				update = true;
				info.append("将用户名从").append(user.getUsername()).append("修改为").append(username);
				user.setUsername(username);
			}
		}
		if (gender != null) {
			UserGender userGender = UserGender.valueOf(gender);
			if (userGender.getCode() != user.getGender()) {
				update = true;
				if (!info.isEmpty()) {
					info.append(",");
				}
				info.append("将性别从").append(user.getUserGender().getName()).append("修改为").append(userGender.getName());
				user.setGender(userGender.getCode());
			}
		}
		if (update) {
			securityLog.setInfo(info.toString());
			securityLog.setUid(user.getUid());
			securityLog.setTitle(user.getUsername());
			securityLog.setTypesByList(List.of(SecurityType.UPDATE_PROFILE));
			securityLog.setIp(NetUtil.getIpAddress(request));
			securityLogService.save(securityLog);
			userService.updateById(user);
		}
		return ResultUtil.ok(update);
	}

	@PostMapping("/update/password")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> updateUserPassword(UpdateUserPasswordRequest updateUserPasswordRequest, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		userService.updatePassword(updateUserPasswordRequest, request);
		return ResultUtil.ok(true);
	}

	/**
	 * 获取头像
	 */
	@GetMapping("/get/avatar/{uid}")
	public ResponseEntity<InputStreamResource> getAvatar(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByCache(uid, request);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", uid, request);
		}
		Path path = photoService.getPhotoPathByMd5(user.getAvatar());
		if (path == null) {
			userService.generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "头像文件不存在", uid, request);
		}
		File file = new File(path.toString());
		if (!file.exists()) {
			userService.generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "头像文件不存在", uid, request);
		}
		try {
			InputStream is = new FileInputStream(file);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file.toPath()));
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getName());
			InputStreamResource inputStreamResource = new InputStreamResource(is);
			return new ResponseEntity<>(inputStreamResource, headers, StatusCode.OK);
		} catch (Throwable e) {
			userService.generateDefaultAvatar(user, request);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "预览系统异常", request);
		}
	}

	/**
	 * 获取用户名
	 */
	@GetMapping("/get/username/{uid}")
	public ResponseEntity<BaseResponse<String>> getUsername(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByCache(uid, request);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", uid, request);
		}
		return ResultUtil.ok(user.getUsername());
	}

	/**
	 * 获取公开的用户信息
	 */
	@GetMapping("/get/profile/{uid}")
	public ResponseEntity<BaseResponse<UserVO>> getPublicUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByCache(uid, request);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", uid, request);
		}
		UserVO userVO = user.toUserVO();
		userVO.setPhone(null);
		return ResultUtil.ok(userVO);
	}

	@GetMapping("/get/profile/username/{username}")
	public ResponseEntity<BaseResponse<UserVO>> getPublicUser(@PathVariable("username") String username, HttpServletRequest request) {
		User user = userService.getByUsername(username, request);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", username, request);
		}
		UserVO userVO = user.toUserVO();
		userVO.setPhone(null);
		return ResultUtil.ok(userVO);
	}

	/**
	 * 获取用户组列表
	 */
	@GetMapping("/get/group")
	@AuthCheck()
	public ResponseEntity<BaseResponse<List<Permission>>> getUserGroup(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		List<Permission> ret = permissionService.getGroups(uid, request);
		return ResultUtil.ok(ret);
	}

	/**
	 * 获取最高权限的用户组
	 */
	@GetMapping("/get/group/max")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Permission>> getMaxPriorityGroup(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
		return ResultUtil.ok(ret);
	}

	/**
	 * 注销
	 */
	@DeleteMapping("")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> userDelete(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		userLogout(request);
		userService.clearOAuthByUser(user, request);
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		securityLog.setTypesByList(List.of(SecurityType.DELETE_USER));
		securityLog.setIp(NetUtil.getIpAddress(request));
		securityLogService.save(securityLog);
		user.setUserStatus(UserStatus.DELETED);
		userService.updateById(user);
		// userService.removeById(user);
		return ResultUtil.ok(true);
	}

	@GetMapping("/security/log")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Page<SecurityLogVO>>> getSecurityLogList(UserSecurityLogQueryRequest userSecurityLogQueryRequest, HttpServletRequest request) {
		if (userSecurityLogQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		User user = userService.getLoginUser(request);
		SecurityLog securityLogQuery = new SecurityLog();
		BeanUtils.copyProperties(userSecurityLogQueryRequest, securityLogQuery);
		long current = userSecurityLogQueryRequest.getCurrent();
		long size = userSecurityLogQueryRequest.getPageSize();
		String sortField = userSecurityLogQueryRequest.getSortField();
		String sortOrder = userSecurityLogQueryRequest.getSortOrder();
		Long id = userSecurityLogQueryRequest.getId();
		Long uid = userSecurityLogQueryRequest.getUid();
		// 如果有uid参数，查询是否有权限*
		boolean e = permissionService.checkPermission(user.getUid(), "*", request);
		if (uid != null) {
			if (!uid.equals(user.getUid())) {
				// 如果不是查询自己的日志，需要权限
				if (!e) {
					// 没有权限
					uid = user.getUid();
				}
			}
		} else {
			if (!e) {
				uid = user.getUid();
			}
		}
		// 默认以uid排序
		if (sortField == null) {
			sortField = "id";
		}
		securityLogQuery.setId(null);
		securityLogQuery.setUid(uid);
		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, request);
		}
		QueryWrapper<SecurityLog> queryWrapper = new QueryWrapper<>(securityLogQuery);
		queryWrapper.like(id != null, "id", id);
		queryWrapper.eq(uid != null, "uid", uid);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<SecurityLog> securityLogPage = securityLogService.page(new Page<>(current, size), queryWrapper);
		Page<SecurityLogVO> securityLogVOPage = new Page<>();
		securityLogVOPage.setCurrent(securityLogPage.getCurrent());
		securityLogVOPage.setSize(securityLogPage.getSize());
		securityLogVOPage.setTotal(securityLogPage.getTotal());
		securityLogVOPage.setPages(securityLogPage.getPages());
		securityLogVOPage.setRecords(securityLogPage.getRecords().stream().map(SecurityLog::toVO).toList());
		return ResultUtil.ok(securityLogVOPage);
	}
}
