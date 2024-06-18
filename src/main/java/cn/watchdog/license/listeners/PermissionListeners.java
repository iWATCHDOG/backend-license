package cn.watchdog.license.listeners;

import cn.watchdog.license.events.permission.PermissionAddEvent;
import cn.watchdog.license.events.permission.PermissionAddSuccessEvent;
import cn.watchdog.license.events.permission.PermissionRemoveByUidEvent;
import cn.watchdog.license.events.permission.PermissionRemoveEvent;
import cn.watchdog.license.events.permission.PermissionRemoveSuccessEvent;
import cn.watchdog.license.events.permission.PermissionUpdateEvent;
import cn.watchdog.license.events.permission.PermissionUpdateSuccessEvent;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.NetUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PermissionListeners {
	@Resource
	private SecurityLogService securityLogService;

	@Resource
	private UserService userService;

	@EventListener
	public void onPermissionAdd(PermissionAddEvent event) {
	}

	@EventListener
	public void onPermissionAddSuccess(PermissionAddSuccessEvent event) {
		long uid = event.getUid();
		boolean admin = event.isAdmin();
		long expiry = event.getExpiry();
		String permission = event.getPermission();
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(uid);
		securityLog.setTitle("添加权限");
		List<SecurityType> st = new ArrayList<>(List.of(SecurityType.ADD_PERMISSION));
		if (admin) {
			st.add(SecurityType.ADMIN_OPERATION);
		}
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		String ep = "{date:" + expiry + "}";
		String sp = "{permission:" + permission + "}";
		securityLog.setInfo("添加权限：" + sp + "，过期时间：" + ep);
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		avatarData.add(new SecurityLog.AvatarData(1, uid));
		User cu = userService.getLoginUser(event.getRequest());
		if (admin) {
			avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
		}
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
		if (admin) {
			securityLog.setId(null);
			securityLog.setUid(cu.getUid());
			securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
			String du = "{uid:" + cu.getUid() + "}";
			securityLog.setInfo("添加权限：" + sp + "，过期时间：" + ep + "，操作人：" + du);
			securityLogService.save(securityLog);
		}
	}

	@EventListener
	public void onPermissionRemoveByUid(PermissionRemoveByUidEvent event) {

	}

	@EventListener
	public void onPermissionRemove(PermissionRemoveEvent event) {

	}

	@EventListener
	public void onPermissionRemoveSuccess(PermissionRemoveSuccessEvent event) {
		Permission p = event.getPermission();
		long uid = p.getUid();
		boolean admin = event.isAdmin();
		boolean expired = event.isExpired();
		String permission = p.getPermission();
		SecurityLog securityLog = new SecurityLog();
		String sp = "{permission:" + permission + "}";
		securityLog.setUid(uid);
		if (expired) {
			securityLog.setTitle("权限过期");
			securityLog.setInfo("权限过期：" + sp);
		} else {
			securityLog.setTitle("移除权限");
			securityLog.setInfo("移除权限：" + sp);
		}
		List<SecurityType> st = new ArrayList<>(List.of(SecurityType.REMOVE_PERMISSION));
		if (admin) {
			st.add(SecurityType.ADMIN_OPERATION);
		}
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		avatarData.add(new SecurityLog.AvatarData(1, uid));
		User cu = userService.getLoginUser(event.getRequest());
		if (admin) {
			avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
		}
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
		if (admin) {
			securityLog.setId(null);
			securityLog.setUid(cu.getUid());
			securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
			securityLog.setTitles("移除权限", uid);
			String du = "{uid:" + cu.getUid() + "}";
			securityLog.setInfo("移除权限：" + sp + "，操作人：" + du);
			securityLogService.save(securityLog);
		}
	}

	@EventListener
	public void onPermissionUpdate(PermissionUpdateEvent event) {

	}

	@EventListener
	public void onPermissionUpdateSuccess(PermissionUpdateSuccessEvent event) {
		String oldPermission = event.getOldPermission();
		String newPermission = event.getNewPermission();
		long uid = event.getUid();
		boolean admin = event.isAdmin();
		long oldExpiry = event.getOldExpiry();
		long newExpiry = event.getNewExpiry();
		// 构造日志
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(uid);
		securityLog.setTitles("修改权限", oldPermission);
		List<SecurityType> st = new ArrayList<>(List.of(SecurityType.UPDATE_PERMISSION));
		if (admin) {
			st.add(SecurityType.ADMIN_OPERATION);
		}
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
		StringBuilder info = new StringBuilder();
		// 如果newPermission不为空且不等于oldPermission，则修改
		if (!StringUtils.isAnyBlank(newPermission) && !oldPermission.equalsIgnoreCase(newPermission)) {
			String ops = "{permission:" + oldPermission + "}";
			String nps = "{permission:" + newPermission + "}";
			String sp = "{old:" + ops + ",new:" + nps + "}";
			info.append("修改权限：").append(sp);
		}
		if (oldExpiry != newExpiry) {
			String oe = "{date:" + oldExpiry + "}";
			String ne = "{date:" + newExpiry + "}";
			String se = "{old:" + oe + ",new:" + ne + "}";
			if (!info.toString().isBlank()) {
				info.append("，");
			}
			info.append("修改过期时间：").append(se);
		}
		if (!info.isEmpty()) {
			securityLog.setInfo(info.toString());
		}
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		avatarData.add(new SecurityLog.AvatarData(1, uid));
		User cu = userService.getLoginUser(event.getRequest());
		if (admin) {
			avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
		}
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
		if (admin) {
			securityLog.setId(null);
			securityLog.setUid(cu.getUid());
			securityLog.setIp(NetUtil.getIpAddress(event.getRequest()));
			securityLog.setTitles("修改权限", uid, oldPermission);
			info.append("，操作人：").append(cu.getUsername());
			securityLog.setInfo(info.toString());
			securityLogService.save(securityLog);
		}
	}
}
