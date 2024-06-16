package cn.watchdog.license.events.permission;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PermissionUpdateEvent extends ApplicationEvent implements Cancellable {
	@Setter
	private PermissionUpdateRequest permissionUpdateRequest;
	@Setter
	private boolean admin;
	private boolean cancelled = false;

	public PermissionUpdateEvent(Object source, PermissionUpdateRequest permissionUpdateRequest, boolean admin) {
		super(source);
		this.permissionUpdateRequest = permissionUpdateRequest;
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
