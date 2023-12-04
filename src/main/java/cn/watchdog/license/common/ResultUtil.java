package cn.watchdog.license.common;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

/**
 * 返回工具类
 */
public class ResultUtil {
	/**
	 * 成功
	 */
	public static <T> BaseResponse<T> success(T data) {
		ReturnCode returnCode = ReturnCode.SUCCESS;
		return new BaseResponse<>(returnCode.getCode(), data, returnCode.getMessage());
	}

	public static <T> ResponseEntity<BaseResponse<T>> ok(T data) {
		return ResponseEntity.ok(success(data));
	}

	/**
	 * 失败
	 */
	private static <T> BaseResponse<T> error(ReturnCode returnCode) {
		return new BaseResponse<>(returnCode);
	}

	private static <T> BaseResponse<T> error(ReturnCode returnCode, T data) {
		return new BaseResponse<>(returnCode, data);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, int httpCode) {
		return new ResponseEntity<>(error(returnCode), null, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, T data, int httpCode) {
		return new ResponseEntity<>(error(returnCode, data), null, httpCode);
	}

	/**
	 * 失败
	 */
	private static <T> BaseResponse<T> error(int code, String message) {
		return new BaseResponse<>(code, null, message);
	}

	private static <T> BaseResponse<T> error(int code, T data, String message) {
		return new BaseResponse<>(code, data, message);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(int errorCode, String message, int httpCode) {
		return new ResponseEntity<>(error(errorCode, message), null, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(int errorCode, T data, String message, int httpCode) {
		return new ResponseEntity<>(error(errorCode, data, message), null, httpCode);
	}

	/**
	 * 失败
	 */
	private static <T> BaseResponse<T> error(ReturnCode returnCode, String message) {
		return new BaseResponse<>(returnCode.getCode(), null, message);
	}

	private static <T> BaseResponse<T> error(ReturnCode returnCode, T data, String message) {
		return new BaseResponse<>(returnCode.getCode(), data, message);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, String message, int httpCode) {
		return new ResponseEntity<>(error(returnCode, message), null, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, T data, String message, int httpCode) {
		return new ResponseEntity<>(error(returnCode, data, message), null, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> of(ReturnCode returnCode, T data, MultiValueMap<String, String> headers, int httpCode) {
		return new ResponseEntity<>(new BaseResponse<>(returnCode.getCode(), data, returnCode.getMessage()), headers, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> of(ReturnCode returnCode, T data, int httpCode) {
		return of(returnCode, data, null, httpCode);
	}

}
