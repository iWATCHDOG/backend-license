package cn.watchdog.license.events.permission;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.Permission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PermissionRemoveEvent extends ApplicationEvent implements Cancellable {
	private final Permission permission;
	private final HttpServletRequest request;
	@Setter
	private boolean admin;
	private boolean cancelled = false;

	public PermissionRemoveEvent(Object source, Permission permission, HttpServletRequest request) {
		super(source);
		this.permission = permission;
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
