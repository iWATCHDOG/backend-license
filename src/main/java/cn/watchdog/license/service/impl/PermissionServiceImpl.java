package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.events.permission.PermissionAddEvent;
import cn.watchdog.license.events.permission.PermissionRemoveByUidEvent;
import cn.watchdog.license.events.permission.PermissionRemoveEvent;
import cn.watchdog.license.events.permission.PermissionUpdateEvent;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.PermissionMapper;
import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.Group;
import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.service.SecurityLogService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.NetUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
	private static final Cache<Long, Set<Permission>> userPermissions = CaffeineFactory.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
	private final ApplicationEventPublisher eventPublisher;
	@Resource
	private SecurityLogService securityLogService;

	@Resource
	private UserService userService;

	public PermissionServiceImpl(ApplicationEventPublisher eventPublisher) {this.eventPublisher = eventPublisher;}

	@Override
	public Set<Permission> getUserPermissions(long uid, HttpServletRequest request) {
		Set<Permission> permissions = userPermissions.getIfPresent(uid);
		if (permissions == null) {
			Permission permissionQuery = new Permission();
			QueryWrapper<Permission> queryWrapper = new QueryWrapper<>(permissionQuery);
			queryWrapper.eq("uid", uid);
			List<Permission> lps = this.list(queryWrapper);
			permissions = Set.copyOf(lps);
			userPermissions.put(uid, permissions);
		}
		return permissions;
	}


	@Override
	public boolean checkPermission(long uid, String permission, HttpServletRequest request) {
		Set<Permission> permissions = getUserPermissions(uid, request);
		for (Permission p : permissions) {
			if (p.getExpiry() == 0 || p.getExpiry() > System.currentTimeMillis()) {
				if (p.getPermission().isBlank()) {
					return true;
				} else if (p.getPermission().equals("*")) {
					return true;
				} else if (p.getPermission().equals("group." + Group.ADMIN.name())) {
					return true;
				} else if (p.getPermission().equalsIgnoreCase(permission)) {
					return true;
				}
			} else {
				removePermissionSecurityLog(p.getUid(), p.getPermission(), true, false, request);
				this.removeById(p.getId());
				userPermissions.invalidate(uid);
			}
		}
		return false;
	}

	@Override
	public boolean checkPermission(Permission permission, HttpServletRequest request) {
		return checkPermission(permission.getUid(), permission.getPermission(), request);
	}

	@Override
	public Permission getPermission(long uid, String permission, HttpServletRequest request) {
		Set<Permission> permissions = getUserPermissions(uid, request);
		for (Permission p : permissions) {
			if (p.getPermission().equalsIgnoreCase(permission)) {
				return p;
			}
		}
		return null;

	}

	@Override
	public void updatePermission(PermissionUpdateRequest permissionUpdateRequest, boolean admin, HttpServletRequest request) {
		PermissionUpdateEvent event = new PermissionUpdateEvent(this, permissionUpdateRequest, admin);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "修改权限被取消", request);
		}
		// id 和 uid 不能修改
		long id = permissionUpdateRequest.getId();
		String newPermission = permissionUpdateRequest.getPermission();
		long newExpiry = permissionUpdateRequest.getExpiry();
		SecurityLog securityLog = new SecurityLog();
		Permission permissionQuery = this.getById(id);
		if (permissionQuery != null) {
			long uid = permissionQuery.getUid();
			userPermissions.invalidate(uid);
			securityLog.setUid(uid);
			String oldPermission = permissionQuery.getPermission();
			long oldExpiry = permissionQuery.getExpiry();
			// 构造日志
			securityLog.setTitles("修改权限", oldPermission);
			List<SecurityType> st = new ArrayList<>(List.of(SecurityType.UPDATE_PERMISSION));
			if (admin) {
				st.add(SecurityType.ADMIN_OPERATION);
			}
			securityLog.setTypesByList(st);
			securityLog.setIp(NetUtil.getIpAddress(request));
			StringBuilder info = new StringBuilder();
			// 如果newPermission不为空且不等于oldPermission，则修改
			if (!StringUtils.isAnyBlank(newPermission) && !oldPermission.equalsIgnoreCase(newPermission)) {
				String ops = "{permission:" + oldPermission + "}";
				String nps = "{permission:" + newPermission + "}";
				String sp = "{old:" + ops + ",new:" + nps + "}";
				permissionQuery.setPermission(newPermission);
				info.append("修改权限：").append(sp);
			}
			if (oldExpiry != newExpiry) {
				permissionQuery.setExpiry(newExpiry);
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
				this.updateById(permissionQuery);
			}
			this.updateById(permissionQuery);
			List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
			avatarData.add(new SecurityLog.AvatarData(1, uid));
			User cu = userService.getLoginUser(request);
			if (admin) {
				avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
			}
			SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
			securityLog.initAvatar(avatar);
			securityLogService.save(securityLog);
			if (admin) {
				securityLog.setId(null);
				securityLog.setUid(cu.getUid());
				securityLog.setIp(NetUtil.getIpAddress(request));
				securityLog.setTitles("修改权限", uid, oldPermission);
				info.append("，操作人：").append(cu.getUsername());
				securityLog.setInfo(info.toString());
				securityLogService.save(securityLog);
			}
			userPermissions.invalidate(uid);
		} else {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "权限不存在", request);
		}
	}

	@Override
	public void addPermission(long uid, String permission, long expiry, boolean admin, HttpServletRequest request) {
		PermissionAddEvent event = new PermissionAddEvent(this, uid, permission, expiry, admin);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "添加权限被取消", request);
		}
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission, request);
		if (permissionQuery == null) {
			permissionQuery = new Permission();
			permissionQuery.setUid(uid);
			permissionQuery.setPermission(permission);
			permissionQuery.setExpiry(expiry);
			boolean saveResult = this.save(permissionQuery);
			if (!saveResult) {
				throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
			}
		} else {
			throw new BusinessException(ReturnCode.DATA_EXISTED, "权限已存在(" + permissionQuery.getId() + ")", request);
		}
		SecurityLog securityLog = new SecurityLog();
		securityLog.setUid(uid);
		securityLog.setTitle("添加权限");
		List<SecurityType> st = new ArrayList<>(List.of(SecurityType.ADD_PERMISSION));
		if (admin) {
			st.add(SecurityType.ADMIN_OPERATION);
		}
		securityLog.setTypesByList(st);
		securityLog.setIp(NetUtil.getIpAddress(request));
		String ep = "{date:" + expiry + "}";
		String sp = "{permission:" + permission + "}";
		securityLog.setInfo("添加权限：" + sp + "，过期时间：" + ep);
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		avatarData.add(new SecurityLog.AvatarData(1, uid));
		User cu = userService.getLoginUser(request);
		if (admin) {
			avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
		}
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
		if (admin) {
			securityLog.setId(null);
			securityLog.setUid(cu.getUid());
			securityLog.setIp(NetUtil.getIpAddress(request));
			String du = "{uid:" + cu.getUid() + "}";
			securityLog.setInfo("添加权限：" + sp + "，过期时间：" + ep + "，操作人：" + du);
			securityLogService.save(securityLog);
		}
		userPermissions.invalidate(uid);
	}

	@Override
	public void addPermission(PermissionAddRequest permissionAddRequest, boolean admin, HttpServletRequest request) {
		addPermission(permissionAddRequest.getUid(), permissionAddRequest.getPermission(), permissionAddRequest.getExpiry(), admin, request);
	}

	@Override
	public void removePermission(long uid, String permission, boolean admin, HttpServletRequest request) {
		PermissionRemoveByUidEvent event = new PermissionRemoveByUidEvent(this, uid, permission, admin);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
		}
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission, request);
		if (permissionQuery != null) {
			removePermissionSecurityLog(permissionQuery.getUid(), permissionQuery.getPermission(), false, admin, request);
			this.removeById(permissionQuery.getId());
		}
	}

	@Override
	public void removePermission(long id, boolean admin, HttpServletRequest request) {
		Permission permissionQuery = this.getById(id);
		PermissionRemoveEvent event = new PermissionRemoveEvent(this, permissionQuery);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
		}
		if (permissionQuery != null) {
			removePermissionSecurityLog(permissionQuery.getUid(), permissionQuery.getPermission(), false, admin, request);
			this.removeById(permissionQuery.getId());
			userPermissions.invalidate(permissionQuery.getUid());
		}
	}

	@Override
	public void removePermission(PermissionRemoveRequest permissionRemoveRequest, boolean admin, HttpServletRequest request) {
		removePermission(permissionRemoveRequest.getUid(), permissionRemoveRequest.getPermission(), admin, request);
	}

	public void removePermissionSecurityLog(long uid, String permission, boolean expired, boolean admin, HttpServletRequest request) {
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
		securityLog.setIp(NetUtil.getIpAddress(request));
		List<SecurityLog.AvatarData> avatarData = new ArrayList<>();
		avatarData.add(new SecurityLog.AvatarData(1, uid));
		User cu = userService.getLoginUser(request);
		if (admin) {
			avatarData.add(new SecurityLog.AvatarData(1, cu.getUid()));
		}
		SecurityLog.Avatar avatar = new SecurityLog.Avatar(avatarData);
		securityLog.initAvatar(avatar);
		securityLogService.save(securityLog);
		if (admin) {
			securityLog.setId(null);
			securityLog.setUid(cu.getUid());
			securityLog.setIp(NetUtil.getIpAddress(request));
			securityLog.setTitles("移除权限", uid);
			String du = "{uid:" + cu.getUid() + "}";
			securityLog.setInfo("移除权限：" + sp + "，操作人：" + du);
			securityLogService.save(securityLog);
		}
	}

	@Override
	public List<Permission> getGroups(long uid, HttpServletRequest request) {
		Set<Permission> permissions = getUserPermissions(uid, request);
		List<Permission> groups = new ArrayList<>();
		for (Permission permission : permissions) {
			if (permission.getExpiry() == 0 || permission.getExpiry() > System.currentTimeMillis()) {
				if (permission.getPermission().startsWith("group.")) {
					groups.add(permission);
				}
				if (permission.getPermission().equals("*")) {
					groups.add(permission);
				}
			} else {
				removePermissionSecurityLog(permission.getUid(), permission.getPermission(), true, false, request);
				this.removeById(permission.getId());
				userPermissions.invalidate(uid);
			}
		}
		return groups;
	}

	@Override
	public Group getMaxPriorityGroup(long uid, HttpServletRequest request) {
		List<Permission> groups = getGroups(uid, request);
		Group maxPriorityGroup = Group.DEFAULT;
		for (Permission group : groups) {
			try {
				if (group.getPermission().equals("*")) {
					return Group.ADMIN;
				}
				Group g = Group.valueOf(group.getPermission().substring(6).toUpperCase());
				if (g.getPriority() > maxPriorityGroup.getPriority()) {
					maxPriorityGroup = g;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return maxPriorityGroup;
	}

	@Override
	public Permission getMaxPriorityGroupP(long uid, HttpServletRequest request) {
		List<Permission> groups = getGroups(uid, request);
		Group maxPriorityGroup = Group.DEFAULT;
		Permission i = null;
		for (Permission group : groups) {
			try {
				if (group.getPermission().equals("*")) {
					return group;
				}
				Group g = Group.valueOf(group.getPermission().substring(6).toUpperCase());
				if (g.getPriority() > maxPriorityGroup.getPriority()) {
					maxPriorityGroup = g;
					i = group;
				} else if (g == Group.DEFAULT) {
					i = group;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return i;
	}
}
