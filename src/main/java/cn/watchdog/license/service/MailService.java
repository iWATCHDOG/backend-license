package cn.watchdog.license.service;

import org.springframework.scheduling.annotation.Async;

public interface MailService {
	@Async
	void getEmailCode(String to, String code);

	@Async
	void forgetPassword(String to, String token);
}
