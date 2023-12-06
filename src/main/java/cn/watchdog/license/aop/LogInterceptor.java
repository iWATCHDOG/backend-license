package cn.watchdog.license.aop;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.gson.GsonProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求响应日志 AOP
 **/
@Aspect
@Component
@Slf4j
public class LogInterceptor {
	@Resource
	private LogService logService;
	@Resource
	private UserService userService;

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
		String ip = request.getRemoteAddr();
		// 生成请求唯一 id
		String requestId = UUID.randomUUID().toString();
		String url = request.getRequestURI();
		// 获取cookies
		Cookie[] cookies = request.getCookies();
		// 获取请求参数
		Object[] args = point.getArgs();
		String reqParam = "[" + StringUtils.join(args, ", ") + "]";
		// 输出请求日志
		log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url, ip, reqParam);
		Log l = new Log();
		l.setRequestId(requestId);
		l.setUserAgent(userAgent);
		l.setMethod(method);
		l.setIp(ip);
		l.setUrl(url);
		l.setParams(reqParam);
		String cookieStr = GsonProvider.normal().toJson(cookies);
		l.setCookies(cookieStr);
		// 执行原方法
		Object result = point.proceed();
		// 输出响应日志
		stopWatch.stop();
		long totalTimeMillis = stopWatch.getTotalTimeMillis();
		log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
		l.setCost(totalTimeMillis);
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
				}
				String resultStr = GsonProvider.normal().toJson(result);
				l.setResult(resultStr);
				l.setHttpCode(responseEntity.getStatusCode().value());
			}
		} catch (Throwable ignored) {
		}
		// 当前登录用户
		User user = userService.getLoginUserIgnoreError(request);
		if (user != null) {
			l.setUid(user.getUid());
		}
		logService.addLog(l, request);
		return result;
	}
}
