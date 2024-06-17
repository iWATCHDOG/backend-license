package cn.watchdog.license.listeners;

import cn.watchdog.license.events.permission.PermissionAddEvent;
import cn.watchdog.license.events.permission.PermissionRemoveByUidEvent;
import cn.watchdog.license.events.permission.PermissionRemoveEvent;
import cn.watchdog.license.events.permission.PermissionUpdateEvent;
import cn.watchdog.license.service.SecurityLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PermissionListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void permissionAdd(PermissionAddEvent event) {
		// do something
	}

	@EventListener
	public void permissionRemoveByUid(PermissionRemoveByUidEvent event) {

	}

	@EventListener
	public void permissionRemove(PermissionRemoveEvent event) {

	}

	@EventListener
	public void permissionUpdate(PermissionUpdateEvent event) {

	}
}
