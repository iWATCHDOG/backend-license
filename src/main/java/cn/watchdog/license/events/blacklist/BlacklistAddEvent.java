package cn.watchdog.license.events.blacklist;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class BlacklistAddEvent extends ApplicationEvent implements Cancellable {
	private final HttpServletRequest request;
	@Setter
	private Blacklist blacklist;
	@Setter
	@Nullable
	private Log l;
	@Setter
	@Nullable
	private User currentUser;
	private boolean cancelled = false;

	public BlacklistAddEvent(Object source, Blacklist blacklist, @Nullable Log l, @Nullable User currentUser, HttpServletRequest request) {
		super(source);
		this.blacklist = blacklist;
		this.l = l;
		this.currentUser = currentUser;
		this.request = request;
	}

	public BlacklistAddEvent(Object source, Blacklist blacklist, @Nullable Log l, HttpServletRequest request) {
		super(source);
		this.blacklist = blacklist;
		this.l = l;
		this.request = request;
	}

	public BlacklistAddEvent(Object source, Blacklist blacklist, HttpServletRequest request) {
		super(source);
		this.blacklist = blacklist;
		this.request = request;
	}

	@Nullable
	public Log getLog() {
		return l;
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
