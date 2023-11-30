package cn.watchdog.license.exception;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
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
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Object>> businessExceptionHandler(BusinessException e) {
		String requestId = UUID.randomUUID().toString();
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
		String requestId = UUID.randomUUID().toString();
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
