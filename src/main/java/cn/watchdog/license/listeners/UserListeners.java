package cn.watchdog.license.listeners;

import cn.watchdog.license.events.user.UserLoginEvent;
import cn.watchdog.license.service.SecurityLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void userLogin(UserLoginEvent event) {
		// do something
	}
}
