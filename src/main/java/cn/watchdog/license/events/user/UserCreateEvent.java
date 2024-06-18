package cn.watchdog.license.events.user;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.dto.user.UserCreateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class UserCreateEvent extends ApplicationEvent implements Cancellable {
	private final UserCreateRequest userCreateRequest;
	private final HttpServletRequest request;
	private boolean cancelled = false;

	public UserCreateEvent(Object source, UserCreateRequest userCreateRequest, HttpServletRequest request) {
		super(source);
		this.userCreateRequest = userCreateRequest;
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
