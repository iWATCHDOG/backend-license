package cn.watchdog.license.events.user;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class UserAddEvent extends ApplicationEvent implements Cancellable {
	private final User user;
	private final HttpServletRequest request;
	private boolean cancelled = false;

	public UserAddEvent(Object source, User user, HttpServletRequest request) {
		super(source);
		this.user = user;
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
