package cn.watchdog.license.listeners;

import cn.watchdog.license.events.UserLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserListeners {
	@EventListener
	public void userLogin(UserLoginEvent event) {
		// do something
	}
}
