package cn.watchdog.license.listeners;

import cn.watchdog.license.events.blacklist.BlacklistAddEvent;
import cn.watchdog.license.events.blacklist.BlacklistRemoveEvent;
import cn.watchdog.license.model.entity.Blacklist;
import cn.watchdog.license.model.entity.Log;
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
public class BlacklistListeners {
	@Resource
	private SecurityLogService securityLogService;

	@EventListener
	public void onBlacklistAdded(BlacklistAddEvent event) {
		Blacklist blacklist = event.getBlacklist();
		Log l = event.getLog();
		if (l == null) {
			log.error("添加黑名单失败，未找到日志信息");
			return;
		}
		Long id = l.getId();
		String ip = l.getIp();
		String reason = blacklist.getReason();
		User cu = event.getCurrentUser();
		if (cu == null) {
			log.error("添加黑名单失败，未找到当前用户信息");
			return;
		}
		// 记录黑名单添加日志
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(cu.getUid());
		securityLog.setTitle("[管理员] 添加黑名单");
		securityLog.setTypesByList(List.of(SecurityType.ADD_BLACKLIST, SecurityType.ADMIN_OPERATION));
		String info = "Log ID：" + id + " IP：" + ip + " 原因：" + reason;
		securityLog.setInfo(info);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		List<SecurityLog.AvatarData> avatarData = List.of(new SecurityLog.AvatarData(1, cu.getUid()));
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
	}

	@EventListener
	public void onBlacklistRemove(BlacklistRemoveEvent event) {
		Blacklist blacklist = event.getBlacklist();
		if (blacklist == null) {
			log.error("移除黑名单失败，未找到黑名单信息");
			return;
		}
		User cu = event.getCurrentUser();
		if (cu == null) {
			log.error("移除黑名单失败，未找到当前用户信息");
			return;
		}
		String ip = blacklist.getIp();
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(cu.getUid());
		securityLog.setTitle("[管理员] 移除黑名单");
		securityLog.setTypesByList(List.of(SecurityType.REMOVE_BLACKLIST, SecurityType.ADMIN_OPERATION));
		String info = "IP：" + ip;
		securityLog.setInfo(info);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		List<SecurityLog.AvatarData> avatarData = List.of(new SecurityLog.AvatarData(1, cu.getUid()));
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
	}
}
