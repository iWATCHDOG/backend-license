package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.events.blacklist.BlacklistAddEvent;
import cn.watchdog.license.events.blacklist.BlacklistRemoveEvent;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.BlacklistMapper;
import cn.watchdog.license.model.dto.blacklist.AddBlackListRequest;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.BlacklistService;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BlacklistServiceImpl extends ServiceImpl<BlacklistMapper, Blacklist> implements BlacklistService {
	private static final Cache<String, Boolean> blackListCache = CaffeineFactory.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
	private final ApplicationEventPublisher eventPublisher;
	@Resource
	private LogService logService;
	@Resource
	private SecurityLogService securityLogService;
	@Resource
	private UserService userService;

	public BlacklistServiceImpl(ApplicationEventPublisher eventPublisher) {this.eventPublisher = eventPublisher;}

	@Override
	public boolean isBlacklist(String ip, HttpServletRequest request) {
		Boolean isBlacklist = blackListCache.getIfPresent(ip);
		if (isBlacklist == null) {
			Blacklist blacklistQuery = new Blacklist();
			QueryWrapper<Blacklist> queryWrapper = new QueryWrapper<>(blacklistQuery);
			queryWrapper.eq("ip", ip);
			isBlacklist = this.getOne(queryWrapper) != null;
			blackListCache.put(ip, isBlacklist);
		}
		return Boolean.TRUE.equals(isBlacklist);
	}

	@Override
	public void addBlacklist(AddBlackListRequest addBlackListRequest, HttpServletRequest request) {
		User cu = userService.getLoginUser(request);
		Long id = addBlackListRequest.getLog();
		if (id == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "id不能为空", request);
		}
		Log l = logService.getById(id);
		if (l == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "id不存在", request);
		}
		String ip = l.getIp();
		if (ip == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "ip不能为空", request);
		}
		blackListCache.invalidate(ip);
		if (this.isBlacklist(ip, request)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "ip已在黑名单中!", request);
		}
		String reason = addBlackListRequest.getReason();
		if (StringUtils.isAllBlank(reason)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "原因不能为空", request);
		}
		Blacklist blacklist = new Blacklist();
		blacklist.setLog(id);
		blacklist.setIp(ip);
		blacklist.setReason(reason);
		BlacklistAddEvent event = new BlacklistAddEvent(this, blacklist, l, cu, request);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "添加黑名单被取消", request);
		}
		this.save(blacklist);
		blackListCache.put(ip, true);
	}

	@Override
	public void removeBlacklist(Long id, HttpServletRequest request) {
		User cu = userService.getLoginUser(request);
		if (id == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "id不能为空", request);
		}
		BlacklistRemoveEvent event = new BlacklistRemoveEvent(this, id, cu, request);
		Blacklist blacklist = this.getById(id);
		event.setBlacklist(blacklist);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "移除黑名单被取消", request);
		}
		if (blacklist == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "id不存在", request);
		}
		String ip = blacklist.getIp();
		this.removeById(blacklist);
		blackListCache.invalidate(ip);
	}
}
