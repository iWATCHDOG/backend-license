package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.BlacklistMapper;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.service.BlacklistService;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BlacklistServiceImpl extends ServiceImpl<BlacklistMapper, Blacklist> implements BlacklistService {
	private static final Cache<String, Boolean> blackListCache = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
	@Resource
	private LogService logService;

	@Override
	public boolean isBlacklist(String ip) {
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
	public void addBlacklist(Long id, HttpServletRequest request) {
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
		if (this.isBlacklist(ip)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "ip已在黑名单中!", request);
		}
		Blacklist blacklist = new Blacklist();
		blacklist.setIp(ip);
		this.save(blacklist);
		blackListCache.put(ip, true);
	}
}
