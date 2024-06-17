package cn.watchdog.license.listeners;

import cn.watchdog.license.events.LogAddEvent;
import cn.watchdog.license.service.SecurityLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void logAdd(LogAddEvent event) {
		// do something
	}
}
