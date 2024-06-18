package cn.watchdog.license.events.permission;

import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class PermissionAddSuccessEvent extends PermissionAddEvent {
	public PermissionAddSuccessEvent(Object source, PermissionAddRequest permissionAddRequest, HttpServletRequest request) {
		super(source, permissionAddRequest, request);
	}

	public PermissionAddSuccessEvent(Object source, long uid, String permission, long expiry, boolean admin, HttpServletRequest request) {
		super(source, uid, permission, expiry, admin, request);
	}

	public PermissionAddSuccessEvent(Object source, PermissionAddEvent event) {
		super(source, event.getUid(), event.getPermission(), event.getExpiry(), event.isAdmin(), event.getRequest());
	}
}
