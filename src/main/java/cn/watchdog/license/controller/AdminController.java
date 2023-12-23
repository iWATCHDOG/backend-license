package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.LogQueryRequest;
import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import cn.watchdog.license.model.dto.user.UserQueryRequest;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
	@Resource
	private UserService userService;
	@Resource
	private PermissionService permissionService;
	@Resource
	private LogService logService;
	@Resource
	private SecurityLogService securityLogService;

	@PostMapping("/user/add")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> userAdd(User user, HttpServletRequest request) {
		user.setUid(null);
		user.setCreateTime(null);
		user.setUpdateTime(null);
		user.setAvailable(null);
		return ResultUtil.ok(userService.userAdd(user, request));
	}

	/**
	 * 重置用户密码
	 */
	@PostMapping("/user/reset/password/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> resetPassword(@PathVariable("uid") Long uid, String password, HttpServletRequest request) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户不存在", uid, request);
		}
		String encodePassword = PasswordUtil.encodePassword(password);
		user.setPassword(encodePassword);
		userService.updateById(user);
		return ResultUtil.ok(true);
	}

	/**
	 * 删除用户
	 */
	@DeleteMapping("/user/remove/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> userRemove(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户不存在", uid, request);
		}
		userService.removeById(uid);
		return ResultUtil.ok(true);
	}

	/**
	 * 设置当前用户状态
	 */
	@PostMapping("/user/set/status/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> userSetStatus(@PathVariable("uid") Long uid, Integer status, HttpServletRequest request) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户不存在", uid, request);
		}
		UserStatus userStatus = UserStatus.valueOf(status);
		user.setStatus(userStatus.getCode());
		userService.updateById(user);
		return ResultUtil.ok(true);
	}

	/**
	 * 分页获取Log列表
	 */
	@GetMapping("/log/list")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Page<Log>>> getLogListPage(LogQueryRequest logQueryRequest, HttpServletRequest request) {
		if (logQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		Log logQuery = new Log();
		BeanUtils.copyProperties(logQueryRequest, logQuery);
		long current = logQueryRequest.getCurrent();
		long size = logQueryRequest.getPageSize();
		String sortField = logQueryRequest.getSortField();
		String sortOrder = logQueryRequest.getSortOrder();
		Long id = logQueryRequest.getId();
		Long uid = logQueryRequest.getUid();
		String requestId = logQueryRequest.getRequestId();
		String ip = logQueryRequest.getIp();
		// 默认以id排序
		if (sortField == null) {
			sortField = "id";
		}
		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, request);
		}

		// 支持模糊搜索
		logQuery.setId(null);
		logQuery.setUid(null);
		logQuery.setRequestId(null);
		logQuery.setIp(null);
		QueryWrapper<Log> queryWrapper = new QueryWrapper<>(logQuery);
		queryWrapper.eq(id != null, "id", id);
		queryWrapper.eq(uid != null, "uid", uid);
		queryWrapper.like(StringUtils.isNotBlank(requestId), "requestId", requestId);
		queryWrapper.like(StringUtils.isNotBlank(ip), "ip", ip);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<Log> logPage = logService.page(new Page<>(current, size), queryWrapper);
		return ResultUtil.ok(logPage);
	}

	/**
	 * 分页获取用户列表
	 */
	@GetMapping("/user/list/page")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Page<UserVO>>> getUserListPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
		if (userQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		User userQuery = new User();
		BeanUtils.copyProperties(userQueryRequest, userQuery);
		long current = userQueryRequest.getCurrent();
		long size = userQueryRequest.getPageSize();
		String sortField = userQueryRequest.getSortField();
		String sortOrder = userQueryRequest.getSortOrder();
		String userName = userQueryRequest.getUsername();
		String email = userQueryRequest.getEmail();
		String phone = userQueryRequest.getPhone();
		// 默认以uid排序
		if (sortField == null) {
			sortField = "uid";
		}
		// userName、email、phone 需支持模糊搜索
		userQuery.setUsername(null);
		userQuery.setEmail(null);
		userQuery.setPhone(null);
		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, request);
		}
		QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
		queryWrapper.like(StringUtils.isNotBlank(userName), "username", userName);
		queryWrapper.like(StringUtils.isNotBlank(email), "email", email);
		queryWrapper.like(StringUtils.isNotBlank(phone), "phone", phone);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
		Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
		List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
			UserVO userVO = new UserVO();
			BeanUtils.copyProperties(user, userVO);
			return userVO;
		}).collect(Collectors.toList());
		userVOPage.setRecords(userVOList);
		return ResultUtil.ok(userVOPage);
	}

	/**
	 * 增加权限
	 */
	@PostMapping("/permission/add")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> addPermission(PermissionAddRequest permissionAddRequest, HttpServletRequest request) {
		permissionService.addPermission(permissionAddRequest, request);
		return ResultUtil.ok(true);
	}

	/**
	 * 删除权限
	 */
	@DeleteMapping("/permission/remove")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> removePermission(PermissionRemoveRequest permissionRemoveRequest, HttpServletRequest request) {
		permissionService.removePermission(permissionRemoveRequest);
		return ResultUtil.ok(true);
	}

	/**
	 * 查询Log
	 */
	@GetMapping("/log/{requestId}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Log>> getLog(@PathVariable("requestId") String requestId, HttpServletRequest request) {
		Log l = logService.getLog(requestId, request);
		return ResultUtil.ok(l);
	}

	@GetMapping("/count/user")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Long>> countUser(HttpServletRequest request) {
		long count = userService.count();
		return ResultUtil.ok(count);
	}

	@GetMapping("/count/log")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Long>> countLog(HttpServletRequest request) {
		long count = logService.count();
		return ResultUtil.ok(count);
	}

	@GetMapping("/count/security")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Long>> countSecurityLog(HttpServletRequest request) {
		long count = securityLogService.count();
		return ResultUtil.ok(count);
	}
}
