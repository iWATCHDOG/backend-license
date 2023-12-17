package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.service.MailService;
import cn.watchdog.license.util.CaffeineFactory;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
	public static final Cache<String, Boolean> emailSent = CaffeineFactory.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();
	@Resource
	private JavaMailSender javaMailSender;
	@Resource
	private TemplateEngine templateEngine;
	@Value("${spring.mail.username:username@gmail.com}")
	private String from;
	@Value("${website.url}")
	private String websiteUrl;

	@Async
	@Override
	public void getEmailCode(String to, String code) {
		checkEmail(to);
		MimeMessage message = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject("License-邮箱验证码");
			helper.setCc(from);
			Context context = new Context();
			context.setVariable("code", code);
			String text = templateEngine.process("EmailCode", context);
			helper.setText(text, true);
			javaMailSender.send(message);
		} catch (Exception e) {
			log.error(e.toString());
		}
	}

	@Async
	@Override
	public void forgetPassword(String to, String token) {
		checkEmail(to);
		MimeMessage message = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject("License-找回密码");
			helper.setCc(from);
			Context context = new Context();
			String link = websiteUrl + "/forget/" + token;
			context.setVariable("link", link);
			String text = templateEngine.process("ForgetPassword", context);
			helper.setText(text, true);
			javaMailSender.send(message);
		} catch (Exception e) {
			log.error(e.toString());
		}
	}

	public void checkEmail(String to) {
		Boolean sent = emailSent.getIfPresent(to);
		if (sent != null) {
			throw new BusinessException(ReturnCode.TOO_MANY_REQUESTS_ERROR, "邮箱发送过于频繁", null);
		}
	}
}
