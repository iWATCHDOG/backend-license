package cn.watchdog.license.events;

import cn.watchdog.license.model.entity.Log;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class LogAddEvent extends ApplicationEvent implements Cancellable {
	private final HttpServletRequest request;
	@Setter
	private Log l;
	private boolean cancelled = false;

	public LogAddEvent(Object source, Log log, HttpServletRequest request) {
		super(source);
		this.l = log;
		this.request = request;
	}

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
