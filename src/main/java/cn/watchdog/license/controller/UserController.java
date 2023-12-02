package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.common.StatusCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
	@Resource
	private UserService userService;

	@PostMapping("/create")
	public ResponseEntity<BaseResponse<Boolean>> userCreate(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		return ResultUtil.ok(userService.userCreate(userCreateRequest, request));
	}

	@GetMapping("/login")
	public ResponseEntity<BaseResponse<UserVO>> userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		User user = userService.userLogin(userLoginRequest, request);
		UserVO userVO = user.toUserVO();
		return ResultUtil.ok(userVO);
	}

	/**
	 * 用户注销
	 */
	@PostMapping("/logout")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> userLogout(HttpServletRequest request) {
		if (request == null) {

			throw new BusinessException(ReturnCode.PARAMS_ERROR);
		}
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
		return ResultUtil.ok(userVO);
	}

	/**
	 * 上传头像
	 */
	@PostMapping("/upload/avatar")
	@AuthCheck()
	public ResponseEntity<BaseResponse<Boolean>> uploadAvatar(@RequestBody MultipartFile avatar, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		userService.setupAvatar(user, avatar);
		return ResultUtil.ok(true);
	}

	/**
	 * 获取头像
	 */
	@GetMapping("/get/avatar/{uid}")
	public ResponseEntity<InputStreamResource> getAvatar(@PathVariable("uid") Long uid) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "用户不存在");
		}
		File file = new File(user.getAvatar());
		if (!file.exists()) {
			userService.generateDefaultAvatar(user);
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "头像文件不存在");
		}
		try {
			InputStream is = new FileInputStream(file);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file.toPath()));
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getName());
			InputStreamResource inputStreamResource = new InputStreamResource(is);
			return new ResponseEntity<>(inputStreamResource, headers, StatusCode.OK);
		} catch (Throwable e) {
			userService.generateDefaultAvatar(user);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "预览系统异常");
		}
	}
}