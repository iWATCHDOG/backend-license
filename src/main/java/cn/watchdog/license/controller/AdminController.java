package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.DataCenter;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.LogQueryRequest;
import cn.watchdog.license.model.dto.blacklist.AddBlackListRequest;
import cn.watchdog.license.model.dto.blacklist.BlacklistQueryRequest;
import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import cn.watchdog.license.model.dto.permission.PermissionQueryRequest;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import cn.watchdog.license.model.dto.user.UserQueryRequest;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.model.vo.PermissionVO;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.BlacklistService;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.NetUtil;
import cn.watchdog.license.util.PasswordUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.watchdog.license.common.DataCenter.*;

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
	private BlacklistService blacklistService;
	@Resource
	private SecurityLogService securityLogService;

	@PostMapping("/user/add")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> userAdd(User user, HttpServletRequest request) {
		user.setUid(null);
		user.setCreateTime(null);
		user.setUpdateTime(null);
		user.setAvailable(null);
		boolean save = userService.userAdd(user, request);
		if (save) {
			User cu = userService.getLoginUser(request);
			SecurityLog securityLog = new SecurityLog();
			securityLog.setUid(cu.getUid());
			securityLog.setTitle("[管理员] " + cu.getUsername());
			securityLog.setTypesByList(List.of(SecurityType.ADD_ACCOUNT, SecurityType.ADMIN_OPERATION));
			String info = "添加用户：" + user.getUsername() +
					", 密码：" + user.getPassword() +
					"，邮箱：" + user.getEmail() +
					"，手机号：" + user.getPhone() +
					"，状态：" + UserStatus.valueOf(user.getStatus()).getDesc() + " uid:" + user.getUid();
			securityLog.setInfo(info);
			securityLog.setIp(NetUtil.getIpAddress(request));
			List<SecurityLog.AvatarData> avatarData = List.of(
					new SecurityLog.AvatarData(1, cu.getUid()),
					new SecurityLog.AvatarData(1, user.getUid())
			);
			SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
			securityLog.initAvatar(avatar);
			securityLogService.save(securityLog);
		}
		return ResultUtil.ok(save);
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
		boolean save = userService.updateById(user);
		if (save) {
			User cu = userService.getLoginUser(request);
			SecurityLog securityLog = new SecurityLog();
			securityLog.setUid(cu.getUid());
			securityLog.setTitle("[管理员] " + cu.getUsername());
			securityLog.setTypesByList(List.of(SecurityType.CHANGE_PASSWORD, SecurityType.ADMIN_OPERATION));
			String un = "{uid:" + user.getUid() + "}";
			String info = "重置用户密码：" + un +
					"，新密码：" + password + " uid:" + uid;
			securityLog.setInfo(info);
			securityLog.setIp(NetUtil.getIpAddress(request));
			List<SecurityLog.AvatarData> avatarData = List.of(
					new SecurityLog.AvatarData(1, cu.getUid()),
					new SecurityLog.AvatarData(1, user.getUid())
			);
			SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
			securityLog.initAvatar(avatar);
			securityLogService.save(securityLog);
		}
		return ResultUtil.ok(save);
	}

	/**
	 * 删除用户
	 */
	@DeleteMapping("/user/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> userRemove(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getById(uid);
		if (user == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "用户不存在", uid, request);
		}
		if (user.getUid() == 1) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "不能删除超级管理员", request);
		}
		User cu = userService.getLoginUser(request);
		if (Objects.equals(user.getUid(), cu.getUid())) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "不能删除自己", request);
		}
		userService.clearOAuthByUser(user, request);
		user.setUserStatus(UserStatus.DELETED);
		userService.updateById(user);
		boolean save = userService.removeById(uid);
		if (save) {
			SecurityLog securityLog = new SecurityLog();
			securityLog.setUid(cu.getUid());
			String cun = "{uid:" + cu.getUid() + "}";
			securityLog.setTitle("[管理员] " + cun);
			securityLog.setTypesByList(List.of(SecurityType.DELETE_USER, SecurityType.ADMIN_OPERATION));
			String un = "{uid:" + user.getUid() + "}";
			String info = "删除用户：" + un + " uid:" + uid;
			securityLog.setInfo(info);
			securityLog.setIp(NetUtil.getIpAddress(request));
			List<SecurityLog.AvatarData> avatarData = List.of(
					new SecurityLog.AvatarData(1, cu.getUid()),
					new SecurityLog.AvatarData(1, user.getUid())
			);
			SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
			securityLog.initAvatar(avatar);
			securityLogService.save(securityLog);
		}
		return ResultUtil.ok(save);
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
		boolean save = userService.updateById(user);
		if (save) {
			User cu = userService.getLoginUser(request);
			SecurityLog securityLog = new SecurityLog();
			securityLog.setUid(cu.getUid());
			String cun = "{uid:" + cu.getUid() + "}";
			securityLog.setTitle("[管理员] " + cun);
			securityLog.setTypesByList(List.of(SecurityType.UPDATE_PROFILE, SecurityType.ADMIN_OPERATION));
			String un = "{uid:" + user.getUid() + "}";
			String info = "设置用户状态：" + un +
					"，状态：" + userStatus.getDesc() + " uid:" + uid;
			securityLog.setInfo(info);
			securityLog.setIp(NetUtil.getIpAddress(request));
			List<SecurityLog.AvatarData> avatarData = List.of(
					new SecurityLog.AvatarData(1, cu.getUid()),
					new SecurityLog.AvatarData(1, user.getUid())
			);
			SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
			securityLog.initAvatar(avatar);
			securityLogService.save(securityLog);
		}
		return ResultUtil.ok(save);
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
		logQuery.setRequestId(null);
		logQuery.setIp(null);
		QueryWrapper<Log> queryWrapper = new QueryWrapper<>(logQuery);
		queryWrapper.like(StringUtils.isNotBlank(requestId), "requestId", requestId);
		queryWrapper.like(StringUtils.isNotBlank(ip), "ip", ip);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<Log> logPage = logService.page(new Page<>(current, size), queryWrapper);
		Page<Log> ret = new PageDTO<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
		List<Log> records = logPage.getRecords().stream().peek(l -> {
			l.setParams(null);
			l.setResult(null);
		}).toList();
		ret.setRecords(records);
		return ResultUtil.ok(ret);
	}

	/**
	 * 分页获取用户列表
	 */
	@GetMapping("/user/list")
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
			long uid = user.getUid();
			Permission ret = permissionService.getMaxPriorityGroupP(uid, request);
			userVO.setGroup(ret);
			return userVO;
		}).collect(Collectors.toList());
		userVOPage.setRecords(userVOList);
		return ResultUtil.ok(userVOPage);
	}

	@GetMapping("/permission/list")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Page<PermissionVO>>> getPermissionListPage(PermissionQueryRequest permissionQueryRequest, HttpServletRequest request) {
		if (permissionQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		Permission permissionQuery = new Permission();
		BeanUtils.copyProperties(permissionQueryRequest, permissionQuery);
		long current = permissionQueryRequest.getCurrent();
		long size = permissionQueryRequest.getPageSize();
		String sortField = permissionQueryRequest.getSortField();
		String sortOrder = permissionQueryRequest.getSortOrder();
		String permission = permissionQueryRequest.getPermission();
		// 取消expiry的搜索
		permissionQuery.setExpiry(null);
		// 默认以id排序
		if (sortField == null) {
			sortField = "id";
		}
		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, request);
		}
		// permission 需支持模糊搜索
		permissionQuery.setPermission(null);
		QueryWrapper<Permission> queryWrapper = new QueryWrapper<>(permissionQuery);
		queryWrapper.like(StringUtils.isNotBlank(permission), "permission", permission);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<Permission> permissionPage = permissionService.page(new Page<>(current, size), queryWrapper);
		Page<PermissionVO> permissionVOPage = new PageDTO<>(permissionPage.getCurrent(), permissionPage.getSize(), permissionPage.getTotal());
		// 筛选出有效的权限
		List<PermissionVO> permissionVOList = permissionPage.getRecords().stream()
				.filter(p -> permissionService.checkPermission(p, request))
				.map(p -> {
					PermissionVO permissionVO = new PermissionVO();
					BeanUtils.copyProperties(p, permissionVO);
					// 设置username
					User user = userService.getById(p.getUid());
					if (user != null) {
						permissionVO.setUsername(user.getUsername());
					}
					return permissionVO;
				}).collect(Collectors.toList());
		permissionVOPage.setRecords(permissionVOList);
		return ResultUtil.ok(permissionVOPage);
	}

	/**
	 * 增加权限
	 */
	@PostMapping("/permission/")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> addPermission(PermissionAddRequest permissionAddRequest, HttpServletRequest request) {
		permissionService.addPermission(permissionAddRequest, true, request);
		return ResultUtil.ok(true);
	}

	/**
	 * 修改权限
	 */
	@PutMapping("/permission/")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> updatePermission(PermissionUpdateRequest permissionUpdateRequest, HttpServletRequest request) {
		permissionService.updatePermission(permissionUpdateRequest, true, request);
		return ResultUtil.ok(true);
	}

	/**
	 * 删除权限
	 */
	@DeleteMapping("/permission/")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> removePermission(PermissionRemoveRequest permissionRemoveRequest, HttpServletRequest request) {
		permissionService.removePermission(permissionRemoveRequest, true, request);
		return ResultUtil.ok(true);
	}

	@DeleteMapping("/permission/{id}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> removePermission(@PathVariable("id") Long id, HttpServletRequest request) {
		permissionService.removePermission(id, true, request);
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
		// 筛选出非删除状态的用户
		long count = userService.lambdaQuery().eq(User::getStatus, UserStatus.NORMAL.getCode()).count();
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

	@GetMapping("/count/blacklist")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Long>> countBlacklist(HttpServletRequest request) {
		long count = blacklistService.count();
		return ResultUtil.ok(count);
	}

	@PostMapping("/blacklist")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> addBlacklist(AddBlackListRequest addBlackListRequest, HttpServletRequest request) {
		blacklistService.addBlacklist(addBlackListRequest, request);
		return ResultUtil.ok(true);
	}

	@DeleteMapping("/blacklist/{id}")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Boolean>> removeBlacklist(@PathVariable("id") Long id, HttpServletRequest request) {
		blacklistService.removeBlacklist(id, request);
		return ResultUtil.ok(true);
	}

	@GetMapping("/blacklist/list")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Page<Blacklist>>> getBlackListPage(BlacklistQueryRequest blacklistQueryRequest, HttpServletRequest request) {
		if (blacklistQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误", request);
		}
		Blacklist blacklistQuery = new Blacklist();
		BeanUtils.copyProperties(blacklistQueryRequest, blacklistQuery);
		long current = blacklistQueryRequest.getCurrent();
		long size = blacklistQueryRequest.getPageSize();
		String sortField = blacklistQueryRequest.getSortField();
		String sortOrder = blacklistQueryRequest.getSortOrder();
		String ip = blacklistQueryRequest.getIp();
		// 默认以id排序
		if (sortField == null) {
			sortField = "id";
		}
		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, request);
		}

		// 支持模糊搜索
		blacklistQueryRequest.setIp(null);
		QueryWrapper<Blacklist> queryWrapper = new QueryWrapper<>(blacklistQuery);
		queryWrapper.like(StringUtils.isNotBlank(ip), "ip", ip);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
		Page<Blacklist> blacklistPage = blacklistService.page(new Page<>(current, size), queryWrapper);
		return ResultUtil.ok(blacklistPage);
	}

	@RequestMapping("/download/user")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<String>> downloadUser(HttpServletRequest request, HttpServletResponse response) {
		// 当前时间格式化为 年月日时分秒
		String key = getKey();

		// 创建文件夹
		String fileName = "user_%s.xlsx".formatted(key);
		String path = "./data/download/user/" + fileName;
		EasyExcel.write(path, User.class)
				.sheet("user")
				.doWrite(() -> {
					List<User> userList = DataCenter.userCache.getIfPresent(key);
					if (userList == null) {
						userList = userService.lambdaQuery().orderByDesc(User::getUid).last("limit 5000").list();
						DataCenter.userCache.put(key, userList);
					}
					return userList;
				});
		File file = new File(path);
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "文件不存在", request);
		}
		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			log.error("下载文件失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "下载文件失败", request);
		}
		return ResultUtil.ok(fileName);
	}

	@RequestMapping("/download/log")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<String>> downloadLog(HttpServletRequest request, HttpServletResponse response) {
		// 当前时间格式化为 年月日时分秒
		String key = getKey();

		// 创建文件夹
		String fileName = "log_%s.xlsx".formatted(key);
		String path = "./data/download/log/" + fileName;
		EasyExcel.write(path, Log.class)
				.sheet("log")
				.doWrite(() -> {
					List<Log> logList = DataCenter.logCache.getIfPresent(key);
					if (logList == null) {
						logList = logService.lambdaQuery().orderByDesc(Log::getId).last("limit 1000").list();
						DataCenter.logCache.put(key, logList);
					}
					return logList;
				});
		File file = new File(path);
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "文件不存在", request);
		}
		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			log.error("下载文件失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "下载文件失败", request);
		}
		return ResultUtil.ok(fileName);
	}

	@RequestMapping("/download/security-log")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<String>> downloadSecurityLog(HttpServletRequest request, HttpServletResponse response) {
		// 当前时间格式化为 年月日时分秒
		String key = getKey();

		// 创建文件夹
		String fileName = "security_log_%s.xlsx".formatted(key);
		String path = "./data/download/security-log/" + fileName;
		EasyExcel.write(path, SecurityLog.class)
				.sheet("security_log")
				.doWrite(() -> {
					List<SecurityLog> securityLogList = DataCenter.securityLogCache.getIfPresent(key);
					if (securityLogList == null) {
						securityLogList = securityLogService.lambdaQuery().orderByDesc(SecurityLog::getId).last("limit 1000").list();
						DataCenter.securityLogCache.put(key, securityLogList);
					}
					return securityLogList;
				});
		File file = new File(path);
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "文件不存在", request);
		}
		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			log.error("下载文件失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "下载文件失败", request);
		}
		return ResultUtil.ok(fileName);
	}

	@RequestMapping("/download/blacklist")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<String>> downloadBlacklist(HttpServletRequest request, HttpServletResponse response) {
		// 当前时间格式化为 年月日时分秒
		String key = getKey();

		// 创建文件夹
		String fileName = "blacklist_%s.xlsx".formatted(key);
		String path = "./data/download/blacklist/" + fileName;
		EasyExcel.write(path, Blacklist.class)
				.sheet("blacklist")
				.doWrite(() -> {
					List<Blacklist> blacklistList = DataCenter.blacklistCache.getIfPresent(key);
					if (blacklistList == null) {
						blacklistList = blacklistService.lambdaQuery().orderByDesc(Blacklist::getId).last("limit 1000").list();
						DataCenter.blacklistCache.put(key, blacklistList);
					}
					return blacklistList;
				});
		File file = new File(path);
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "文件不存在", request);
		}
		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			log.error("下载文件失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "下载文件失败", request);
		}
		return ResultUtil.ok(fileName);
	}

	@RequestMapping("/download/permission")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<String>> downloadPermission(HttpServletRequest request, HttpServletResponse response) {
		// 当前时间格式化为 年月日时分秒
		String key = getKey();

		// 创建文件夹
		String fileName = "permission_%s.xlsx".formatted(key);
		String path = "./data/download/permission/" + fileName;
		EasyExcel.write(path, Permission.class)
				.sheet("permission")
				.doWrite(() -> {
					List<Permission> permissionList = DataCenter.permissionCache.getIfPresent(key);
					if (permissionList == null) {
						permissionList = permissionService.lambdaQuery().orderByDesc(Permission::getId).last("limit 2000").list();
						DataCenter.permissionCache.put(key, permissionList);
					}
					return permissionList;
				});
		File file = new File(path);
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "文件不存在", request);
		}
		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			log.error("下载文件失败", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "下载文件失败", request);
		}
		return ResultUtil.ok(fileName);
	}
}
