package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.UserQueryRequest;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.vo.UserVO;
import cn.watchdog.license.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
	 * 分页获取用户列表
	 */
	@GetMapping("/user/list/page")
	@AuthCheck(must = "*")
	public ResponseEntity<BaseResponse<Page<UserVO>>> getUserListPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
		if (userQueryRequest == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数错误");
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
			throw new BusinessException(ReturnCode.PARAMS_ERROR);
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

}