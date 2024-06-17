package cn.watchdog.license.events.permission;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PermissionAddEvent extends ApplicationEvent implements Cancellable {
	@Setter
	private long uid;
	@Setter
	private String permission;
	@Setter
	private long expiry;
	@Setter
	private boolean admin;
	private final HttpServletRequest request;
	private boolean cancelled = false;

	public PermissionAddEvent(Object source, PermissionAddRequest permissionAddRequest, HttpServletRequest request) {
		super(source);
		this.uid = permissionAddRequest.getUid();
		this.permission = permissionAddRequest.getPermission();
		this.expiry = permissionAddRequest.getExpiry();
		this.request = request;
	}

	public PermissionAddEvent(Object source, long uid, String permission, long expiry, boolean admin, HttpServletRequest request) {
		super(source);
		this.uid = uid;
		this.permission = permission;
		this.expiry = expiry;
		this.admin = admin;
		this.request = request;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
