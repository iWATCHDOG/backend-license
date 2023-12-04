package cn.watchdog.license.aop;

import cn.watchdog.license.common.BaseResponse;
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
		HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		// 生成请求唯一 id
		String requestId = UUID.randomUUID().toString();
		String url = httpServletRequest.getRequestURI();
		// 获取请求参数
		Object[] args = point.getArgs();
		String reqParam = "[" + StringUtils.join(args, ", ") + "]";
		// 输出请求日志
		log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
				httpServletRequest.getRemoteHost(), reqParam);
		// 执行原方法
		Object result = point.proceed();
		// 输出响应日志
		stopWatch.stop();
		long totalTimeMillis = stopWatch.getTotalTimeMillis();
		log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
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
			}
		} catch (Throwable ignored) {
		}
		return result;
	}
}
