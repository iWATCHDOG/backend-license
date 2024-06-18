package cn.watchdog.license.listeners;

import cn.watchdog.license.events.PhotoAddEvent;
import cn.watchdog.license.service.SecurityLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhotoListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void onPhotoAdd(PhotoAddEvent event) {

	}
}
