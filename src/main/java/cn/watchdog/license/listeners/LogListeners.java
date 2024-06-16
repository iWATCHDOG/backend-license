package cn.watchdog.license.listeners;

import cn.watchdog.license.events.LogAddEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogListeners {
	@EventListener
	public void logAdd(LogAddEvent event) {
		// do something
	}
}
