package cn.watchdog.license.aop;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.mapper.SecurityLogMapper;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.NetUtil;
import cn.watchdog.license.util.gson.GsonProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 请求响应日志 AOP
 **/
@Aspect
@Component
@Slf4j
public class LogInterceptor {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(16);
	@Resource
	private LogService logService;
	@Resource
	private UserService userService;
	@Resource
	private SecurityLogMapper securityLogMapper;

	/**
	 * 执行拦截
	 */
	@Around("execution(* cn.watchdog.license.controller.*.*(..))")
	public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
		// 计时
		StopWatch stopWatch = new StopWatch();
		long time = System.currentTimeMillis();
		stopWatch.start();
		// 获取请求路径
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		// 获取User-Agent
		String userAgent = request.getHeader("User-Agent");
		// 获取请求方法
		String method = request.getMethod();
		// 获取ip
		String ip = NetUtil.getIpAddress(request);
		// 生成请求唯一 id
		String requestId = UUID.randomUUID().toString();
		String url = request.getRequestURI();
		// 获取cookies
		Cookie[] cookies = request.getCookies();
		// 获取请求参数
		Object[] args = point.getArgs();
		String reqParam = null;
		for (Object arg : args) {
			if (arg instanceof HttpServletRequest) {
				HttpServletRequest req = (HttpServletRequest) arg;
				Enumeration<String> parameterNames = req.getParameterNames();
				Map<String, String> map = new HashMap<>();
				while (parameterNames.hasMoreElements()) {
					String name = parameterNames.nextElement();
					String value = req.getParameter(name);
					map.put(name, value);
				}
				reqParam = GsonProvider.normal().toJson(map);
				break;
			}
		}
		// 获取请求Headers
		Map<String, String> headers = new HashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader(headerName);
			headers.put(headerName, headerValue);
		}
		// 输出请求日志
		log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url, ip, reqParam);
		Log l = new Log();
		l.setRequestId(requestId);
		l.setMethod(method);
		l.setIp(ip);
		l.setUrl(url);
		l.setParams(reqParam);
		l.setHeaders(GsonProvider.normal().toJson(headers));
		// 执行原方法
		Object result = point.proceed();
		// 输出响应日志
		stopWatch.stop();
		long totalTimeMillis = stopWatch.getTotalTimeMillis();
		log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
		l.setCost(totalTimeMillis);
		boolean saveLog = true;
		try {
			if (result instanceof ResponseEntity) {
				ResponseEntity<BaseResponse<Object>> responseEntity = (ResponseEntity<BaseResponse<Object>>) result;
				BaseResponse<Object> baseResponse = responseEntity.getBody();
				if (baseResponse != null) {
					BaseResponse.RequestInfo requestInfo = new BaseResponse.RequestInfo();
					requestInfo.setRequestId(requestId);
					requestInfo.setTimestamp(time);
					requestInfo.setCost(totalTimeMillis);
					baseResponse.setRequestInfo(requestInfo);
					Object data = baseResponse.getData();
					if (url.startsWith("/notify")) {
						// 如果是notify请求，再判定是否有必要记录日志
						// 如果是GET请求，判定是否有返回值;如果不是GET请求，则记录日志
						if (method.equals("GET")) {
							// 尝试解析返回值，将data转为List<NotifyResponse>；若解析成功并且list的size大于0，则记录日志
							try {
								String dataStr = GsonProvider.normal().toJson(data);
								List list = GsonProvider.normal().fromJson(dataStr, List.class);
								saveLog = list != null && !list.isEmpty();
							} catch (Throwable ignored) {
								saveLog = false;
							}
						}
					}
				}
				String resultStr = GsonProvider.normal().toJson(result);
				l.setResult(resultStr);
				l.setHttpCode(responseEntity.getStatusCode().value());
			}
		} catch (Throwable ignored) {
		}
		if (url.startsWith("/ping")) {
			// 如果是ping请求，不记录日志
			saveLog = false;
		} else if (url.startsWith("/admin/count")) {
			// 如果是admin count请求，不记录日志
			saveLog = false;
		} else if (url.startsWith("/user/get/avatar/")) {
			// 如果是avatar请求，不记录日志
			saveLog = false;
		}
		if (saveLog) {
			User user = userService.getLoginUserIgnoreError(request);
			if (user != null) {
				l.setUid(user.getUid());
			}
			logService.addLog(l, request);
		}
		return result;
	}
}
