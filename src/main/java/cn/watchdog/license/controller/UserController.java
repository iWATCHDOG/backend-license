package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.common.StatusCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.user.UpdateUserProfileRequest;
import cn.watchdog.license.model.dto.user.UserCreateRequest;
import cn.watchdog.license.model.dto.user.UserLoginRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.UserGender;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.MailService;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.service.impl.UserServiceImpl;
import cn.watchdog.license.util.StringUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.UUID;

import static cn.watchdog.license.constant.UserConstant.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
	@Resource
	private UserService userService;
	@Resource
	private PermissionService permissionService;
	@Resource
	private MailService mailService;

	@PostMapping("/create")
	public ResponseEntity<BaseResponse<Boolean>> userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		return ResultUtil.ok(userService.userCreate(userCreateRequest, request));
	}

	@PostMapping("create/email")
	public ResponseEntity<BaseResponse<Boolean>> userCreateEmail(String email, HttpServletRequest request) {
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
		User user = userService.userLogin(userLoginRequest, request);
		UserVO userVO = user.toUserVO();
		// 获取Token
		String token = request.getSession().getAttribute(LOGIN_TOKEN).toString();
		userVO.setToken(token);
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
		if (StringUtils.isNotBlank(username)) {
			if (!username.equals(user.getUsername())) {
				if (!username.matches("^[a-zA-Z0-9_-]{1,16}$")) {
					throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户名格式错误", request);
				}
				update = true;
				user.setUsername(username);
			}
		}
		if (gender != null) {
			UserGender userGender = UserGender.valueOf(gender);
			if (userGender.getCode() != user.getGender()) {
				update = true;
				user.setGender(userGender.getCode());
			}

		}
		if (update) {
			userService.updateById(user);
		}
		return ResultUtil.ok(update);
	}

	/**
	 * 获取头像
	 */
	@GetMapping("/get/avatar/{uid}")
	public ResponseEntity<InputStreamResource> getAvatar(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", uid, request);
		}
		File file = new File(user.getAvatar());
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
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在", uid, request);
		}
		return ResultUtil.ok(user.getUsername());
	}

	/**
	 * 获取用户组列表
	 */
	@GetMapping("/get/group")
	@AuthCheck()
	public ResponseEntity<BaseResponse<List<Permission>>> getUserGroup(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		List<Permission> ret = permissionService.getGroups(uid);
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
		Permission ret = permissionService.getMaxPriorityGroupP(uid);
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
		userService.removeById(user);
		return ResultUtil.ok(true);
	}
}
