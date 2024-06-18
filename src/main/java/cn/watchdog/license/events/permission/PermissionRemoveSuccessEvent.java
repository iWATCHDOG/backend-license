package cn.watchdog.license.events.permission;

import cn.watchdog.license.model.entity.Permission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@Slf4j
public class PermissionRemoveSuccessEvent extends PermissionRemoveEvent {
	private boolean expired = false;

	public PermissionRemoveSuccessEvent(Object source, Permission ipermission, HttpServletRequest request) {
		super(source, ipermission, request);
	}

	public PermissionRemoveSuccessEvent(Object source, PermissionRemoveEvent event) {
		super(source, event.getPermission(), event.getRequest());
	}
}
