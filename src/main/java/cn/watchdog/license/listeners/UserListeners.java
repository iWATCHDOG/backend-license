package cn.watchdog.license.listeners;

import cn.watchdog.license.events.user.UserAddEvent;
import cn.watchdog.license.events.user.UserAddSuccessEvent;
import cn.watchdog.license.events.user.UserLoginEvent;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.util.NetUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class UserListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void onUserLogin(UserLoginEvent event) {
		// do something
	}

	@EventListener
	public void onUserAdd(UserAddEvent event) {

	}

	@EventListener
	public void onUserAddSuccess(UserAddSuccessEvent event) {
		User user = event.getUser();
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(user.getUid());
		securityLog.setTitle(user.getUsername());
		List<SecurityType> st = List.of(SecurityType.ADD_ACCOUNT);
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		securityLogService.save(securityLog);
	}
}
