package cn.watchdog.license.exception;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.util.gson.GsonProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@Resource
	private LogService logService;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Object>> businessExceptionHandler(BusinessException e) {
		Log l = new Log();
		l.setHttpCode(e.getStatus());
		String requestId = UUID.randomUUID().toString();
		l.setRequestId(requestId);
		l.setMethod(e.getMethod());
		l.setParams(e.getParams());
		l.setUrl(e.getUrl());
		String cookieStr = GsonProvider.normal().toJson(e.getCookies());
		l.setCookies(cookieStr);
		l.setIp(e.getIp());
		l.setUserAgent(e.getUserAgent());
		l.setResult(e.getMessage());
		logService.addLog(l, e.getRequest());
		log.error("businessException, id: {}, message: {}", requestId, e.getMessage(), e);
		ResponseEntity<BaseResponse<Object>> ret = ResultUtil.failed(e.getCode(), e.getMessage(), e.getStatus());
		BaseResponse<Object> body = ret.getBody();
		BaseResponse.RequestInfo requestInfo = new BaseResponse.RequestInfo();
		requestInfo.setRequestId(requestId);
		requestInfo.setTimestamp(System.currentTimeMillis());
		requestInfo.setCost(-1);
		assert body != null;
		body.setRequestInfo(requestInfo);
		return ret;
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<BaseResponse<Object>> runtimeExceptionHandler(RuntimeException e) {
		Log l = new Log();
		l.setHttpCode(500);
		String requestId = UUID.randomUUID().toString();
		l.setRequestId(requestId);
		l.setResult(e.getMessage());
		logService.addLog(l, null);
		log.error("runtimeException, id: {}, message: {}", requestId, e.getMessage(), e);
		ResponseEntity<BaseResponse<Object>> ret = ResultUtil.failed(ReturnCode.SYSTEM_ERROR, e.getMessage(), 500);
		BaseResponse<Object> body = ret.getBody();
		BaseResponse.RequestInfo requestInfo = new BaseResponse.RequestInfo();
		requestInfo.setRequestId(requestId);
		requestInfo.setTimestamp(System.currentTimeMillis());
		requestInfo.setCost(-1);
		assert body != null;
		body.setRequestInfo(requestInfo);
		return ret;
	}
}
