package cn.watchdog.license.common;

/**
 * 错误码
 */
public interface StatusCode {
	int OK = 200;
	int CREATED = 201;
	int BAD_REQUEST = 400;
	int UNAUTHORIZED = 401;
	int FORBIDDEN = 403;
	int NOT_FOUND = 404;
	int TOO_MANY_REQUESTS = 429;
	int INTERNAL_SERVER_ERROR = 500;
}
