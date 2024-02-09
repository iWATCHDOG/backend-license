package cn.watchdog.license.filter;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.common.StatusCode;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.BlacklistService;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.NetUtil;
import cn.watchdog.license.util.RateLimiter;
import cn.watchdog.license.util.gson.GsonProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class RateLimitingFilter implements Filter {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(16);
	private final RateLimiter rateLimiter = new RateLimiter();
	@Resource
	private LogService logService;
	@Resource
	private BlacklistService blacklistService;
	@Resource
	private UserService userService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String ipAddress = NetUtil.getIpAddress(httpServletRequest);
		HttpServletResponse httpServletResponse = ((HttpServletResponse) response);

		// 获取User-Agent
		String userAgent = httpServletRequest.getHeader("User-Agent");
		// 获取请求方法
		String method = httpServletRequest.getMethod();
		// 生成请求唯一 id
		String id = UUID.randomUUID().toString();
		String url = httpServletRequest.getRequestURI();
		// 获取cookies
		Cookie[] cookies = httpServletRequest.getCookies();
		// 获取args
		String args = GsonProvider.normal().toJson(httpServletRequest.getParameterMap());
		// 检查是否在黑名单
		if (blacklistService.isBlacklist(ipAddress)) {
			log.warn("request was rejected because of blacklist. id: {}, ip: {}, url: {}, method: {}, args: {}", id, ipAddress, url, method, args);
			httpServletResponse.setStatus(StatusCode.FORBIDDEN);
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			BaseResponse<String> baseResponse = new BaseResponse<>(ReturnCode.FORBIDDEN_ERROR);
			BaseResponse.RequestInfo requestInfo = new BaseResponse.RequestInfo();
			requestInfo.setRequestId(id);
			requestInfo.setTimestamp(System.currentTimeMillis());
			requestInfo.setCost(-1);
			baseResponse.setRequestInfo(requestInfo);
			httpServletResponse.getWriter().write(GsonProvider.normal().toJson(baseResponse));
			Log l = new Log();
			l.setRequestId(id);
			l.setUserAgent(userAgent);
			l.setMethod(method);
			l.setIp(ipAddress);
			l.setUrl(url);
			l.setParams(args);
			String cookieStr = GsonProvider.normal().toJson(cookies);
			l.setCookies(cookieStr);
			l.setCost((long) -1);
			l.setResult("黑名单");
			l.setHttpCode(StatusCode.FORBIDDEN);
			scheduler.submit(() -> {
				// 当前登录用户
				User user = userService.getLoginUserIgnoreError(httpServletRequest);
				if (user != null) {
					l.setUid(user.getUid());
				}
				logService.addLog(l, httpServletRequest);
			});
			return;
		}

		if (rateLimiter.isOverLimit(ipAddress)) {
			log.warn("request was rejected because of rate limiting. id: {}, ip: {}, url: {}, method: {}, args: {}", id, ipAddress, url, method, args);
			httpServletResponse.setStatus(StatusCode.TOO_MANY_REQUESTS);
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			BaseResponse<String> baseResponse = new BaseResponse<>(ReturnCode.TOO_MANY_REQUESTS_ERROR);
			BaseResponse.RequestInfo requestInfo = new BaseResponse.RequestInfo();
			requestInfo.setRequestId(id);
			requestInfo.setTimestamp(System.currentTimeMillis());
			requestInfo.setCost(-1);
			baseResponse.setRequestInfo(requestInfo);
			httpServletResponse.getWriter().write(GsonProvider.normal().toJson(baseResponse));
			Log l = new Log();
			l.setRequestId(id);
			l.setUserAgent(userAgent);
			l.setMethod(method);
			l.setIp(ipAddress);
			l.setUrl(url);
			l.setParams(args);
			String cookieStr = GsonProvider.normal().toJson(cookies);
			l.setCookies(cookieStr);
			l.setCost((long) -1);
			l.setResult("请求过于频繁");
			l.setHttpCode(StatusCode.TOO_MANY_REQUESTS);
			scheduler.submit(() -> {
				// 当前登录用户
				User user = userService.getLoginUserIgnoreError(httpServletRequest);
				if (user != null) {
					l.setUid(user.getUid());
				}
				logService.addLog(l, httpServletRequest);
			});
			return;
		}

		chain.doFilter(request, response);
	}
}
