package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.UserCreateRequest;
import cn.watchdog.license.model.dto.UserLoginRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping("/login")
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
}
