package cn.watchdog.license.events;

import cn.watchdog.license.model.entity.Photo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PhotoAddEvent extends ApplicationEvent implements Cancellable {
	private final HttpServletRequest request;
	@Setter
	private Photo photo;
	/**
	 * 是否已经添加
	 * 如果已经添加则不再添加，并且{cancelled}事件也不会被触发
	 */
	@Setter
	private boolean added = false;
	private boolean cancelled = false;

	public PhotoAddEvent(Object source, Photo photo, HttpServletRequest request) {
		super(source);
		this.photo = photo;
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
