package cn.watchdog.license.events.user;

import cn.watchdog.license.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class UserAddSuccessEvent extends UserAddEvent {
	public UserAddSuccessEvent(Object source, User user, HttpServletRequest request) {
		super(source, user, request);
	}

	public UserAddSuccessEvent(Object source, UserAddEvent event) {
		super(source, event.getUser(), event.getRequest());
	}
}
