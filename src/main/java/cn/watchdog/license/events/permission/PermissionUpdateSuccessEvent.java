package cn.watchdog.license.events.permission;

import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@Slf4j
public class PermissionUpdateSuccessEvent extends PermissionUpdateEvent {
	private String oldPermission;
	private String newPermission;
	private long newExpiry;
	private long oldExpiry;
	private long uid;

	public PermissionUpdateSuccessEvent(Object source, PermissionUpdateRequest permissionUpdateRequest, boolean admin, HttpServletRequest request) {
		super(source, permissionUpdateRequest, admin, request);
	}

	public PermissionUpdateSuccessEvent(Object source, PermissionUpdateEvent event) {
		super(source, event.getPermissionUpdateRequest(), event.isAdmin(), event.getRequest());
	}
}
