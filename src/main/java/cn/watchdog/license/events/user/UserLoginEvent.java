package cn.watchdog.license.events.user;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class UserLoginEvent extends ApplicationEvent implements Cancellable {
	private final User user;
	@Setter
	private String token;
	@Setter
	private OAuth oAuth;

	private boolean cancelled = false;

	public UserLoginEvent(Object source, User user) {
		super(source);
		this.user = user;
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
