package cn.watchdog.license.events.permission;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PermissionRemoveByUidEvent extends ApplicationEvent implements Cancellable {
	private final long uid;
	private final String permission;
	@Setter
	private boolean admin;
	private boolean cancelled = false;

	public PermissionRemoveByUidEvent(Object source, PermissionRemoveRequest permissionRemoveRequest) {
		super(source);
		this.uid = permissionRemoveRequest.getUid();
		this.permission = permissionRemoveRequest.getPermission();
	}

	public PermissionRemoveByUidEvent(Object source, long uid, String permission, boolean admin) {
		super(source);
		this.uid = uid;
		this.permission = permission;
		this.admin = admin;
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
