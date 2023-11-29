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
	private static <T> BaseResponse<T> success(T data) {
		return new BaseResponse<>(ReturnCode.SUCCESS.getCode(), data, "操作成功");
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

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, int httpCode) {
		return new ResponseEntity<>(error(returnCode), null, httpCode);
	}

	/**
	 * 失败
	 */
	private static BaseResponse error(int code, String message) {
		return new BaseResponse(code, null, message);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(int errorCode, String message, int httpCode) {
		return new ResponseEntity<>(error(errorCode, message), null, httpCode);
	}

	/**
	 * 失败
	 */
	private static BaseResponse error(ReturnCode returnCode, String message) {
		return new BaseResponse(returnCode.getCode(), null, message);
	}

	public static <T> ResponseEntity<BaseResponse<T>> failed(ReturnCode returnCode, String message, int httpCode) {
		return new ResponseEntity<>(error(returnCode, message), null, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> of(ReturnCode returnCode, T data, MultiValueMap<String, String> headers, int httpCode) {
		return new ResponseEntity<>(new BaseResponse<>(returnCode.getCode(), data, returnCode.getMessage()), headers, httpCode);
	}

	public static <T> ResponseEntity<BaseResponse<T>> of(ReturnCode returnCode, T data, int httpCode) {
		return of(returnCode, data, null, httpCode);
	}

}
