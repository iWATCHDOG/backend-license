package cn.watchdog.license.events.blacklist;

import cn.watchdog.license.events.Cancellable;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class BlacklistRemoveEvent extends ApplicationEvent implements Cancellable {
	private final long id;
	private final HttpServletRequest request;
	@Setter
	@Nullable
	private Blacklist blacklist;
	@Setter
	@Nullable
	private User currentUser;
	private boolean cancelled = false;

	public BlacklistRemoveEvent(Object source, long id, @Nullable Blacklist blacklist, @Nullable User currentUser, HttpServletRequest request) {
		super(source);
		this.id = id;
		this.blacklist = blacklist;
		this.request = request;
		this.currentUser = currentUser;
	}

	public BlacklistRemoveEvent(Object source, @NotNull Blacklist blacklist, @Nullable User currentUser, HttpServletRequest request) {
		super(source);
		this.id = blacklist.getId();
		this.blacklist = blacklist;
		this.currentUser = currentUser;
		this.request = request;
	}

	public BlacklistRemoveEvent(Object source, long id, @Nullable User currentUser, HttpServletRequest request) {
		super(source);
		this.id = id;
		this.request = request;
		this.currentUser = currentUser;
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
