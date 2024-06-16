package cn.watchdog.license.events.permission;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.Permission;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PermissionRemoveEvent extends ApplicationEvent implements Cancellable {
	private final Permission id;
	@Setter
	private boolean admin;
	private boolean cancelled = false;

	public PermissionRemoveEvent(Object source, Permission id) {
		super(source);
		this.id = id;
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
