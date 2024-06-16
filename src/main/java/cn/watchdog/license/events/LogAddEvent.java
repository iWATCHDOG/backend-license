package cn.watchdog.license.events;

import cn.watchdog.license.model.entity.Log;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class LogAddEvent extends ApplicationEvent implements Cancellable {
	@Setter
	private Log l;
	private boolean cancelled = false;

	public LogAddEvent(Object source, Log log) {
		super(source);
		this.l = log;
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
