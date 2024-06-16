package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.events.LogAddEvent;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.LogMapper;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.crypto.AESUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
	private final ApplicationEventPublisher eventPublisher;
	@Resource
	private UserService userService;

	public LogServiceImpl(ApplicationEventPublisher eventPublisher) {this.eventPublisher = eventPublisher;}

	@Override
	@Async
	public void addLog(Log log, HttpServletRequest request) {
		Log l = new Log();
		BeanUtils.copyProperties(log, l);
		if (l.getHttpCode() == null) {
			l.setHttpCode(200);
		}
		if (l.getCost() == null) {
			l.setCost(-1L);
		}
		if (l.getUid() == null) {
			User user = userService.getLoginUserIgnoreError(request);
			if (user != null) {
				l.setUid(user.getUid());
			}
		}
		// 加密敏感数据
		String params = l.getParams();
		String url = l.getUrl();
		String result = l.getResult();
		if (url.startsWith("/admin/log")) {
			result = null;
		}
		try {
			params = AESUtil.encrypt(params);
			l.setParams(params);
			if (result != null) {
				result = AESUtil.encrypt(result);
			}
			l.setResult(result);
		} catch (Exception ignored) {
		}
		LogAddEvent event = new LogAddEvent(this, l);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "日志记录被取消", request);
		}
		this.save(l);
	}

	@Override
	public Log getLog(String requestId, HttpServletRequest request) {
		QueryWrapper<Log> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("requestId", requestId);
		Log l = this.getOne(queryWrapper);
		if (l == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "requestId not found", request);
		}
		// 解密敏感数据
		String params = l.getParams();
		String result = l.getResult();
		try {
			params = AESUtil.decrypt(params);
			l.setParams(params);
			result = AESUtil.decrypt(result);
			l.setResult(result);
		} catch (Exception ignored) {
		}
		return l;
	}
}
