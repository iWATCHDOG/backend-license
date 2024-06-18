package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.events.permission.PermissionAddEvent;
import cn.watchdog.license.events.permission.PermissionAddSuccessEvent;
import cn.watchdog.license.events.permission.PermissionRemoveByUidEvent;
import cn.watchdog.license.events.permission.PermissionRemoveEvent;
import cn.watchdog.license.events.permission.PermissionRemoveSuccessEvent;
import cn.watchdog.license.events.permission.PermissionUpdateEvent;
import cn.watchdog.license.events.permission.PermissionUpdateSuccessEvent;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.PermissionMapper;
import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.enums.Group;
import cn.watchdog.license.service.PermissionService;
import cn.watchdog.license.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
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
				PermissionRemoveSuccessEvent event = new PermissionRemoveSuccessEvent(this, p, request);
				event.setExpired(true);
				event.setAdmin(false);
				eventPublisher.publishEvent(event);
				if (event.isCancelled()) {
					throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
				}
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
		PermissionUpdateEvent event = new PermissionUpdateEvent(this, permissionUpdateRequest, admin, request);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "修改权限被取消", request);
		}
		// id 和 uid 不能修改
		long id = permissionUpdateRequest.getId();
		String newPermission = permissionUpdateRequest.getPermission();
		long newExpiry = permissionUpdateRequest.getExpiry();
		Permission permissionQuery = this.getById(id);
		if (permissionQuery != null) {
			long uid = permissionQuery.getUid();
			userPermissions.invalidate(uid);
			String oldPermission = permissionQuery.getPermission();
			long oldExpiry = permissionQuery.getExpiry();
			// 如果newPermission不为空且不等于oldPermission，则修改
			if (!StringUtils.isAnyBlank(newPermission) && !oldPermission.equalsIgnoreCase(newPermission)) {
				permissionQuery.setPermission(newPermission);
			}
			if (oldExpiry != newExpiry) {
				permissionQuery.setExpiry(newExpiry);
			}
			PermissionUpdateSuccessEvent eventSuccess = new PermissionUpdateSuccessEvent(this, event);
			eventSuccess.setOldPermission(oldPermission);
			eventSuccess.setNewPermission(newPermission);
			eventSuccess.setOldExpiry(oldExpiry);
			eventSuccess.setNewExpiry(newExpiry);
			eventSuccess.setUid(uid);
			eventPublisher.publishEvent(eventSuccess);
			if (eventSuccess.isCancelled()) {
				throw new BusinessException(ReturnCode.CANCELLED, "修改权限被取消", request);
			}
			this.updateById(permissionQuery);
			userPermissions.invalidate(uid);
		} else {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "权限不存在", request);
		}
	}

	@Override
	public void addPermission(long uid, String permission, long expiry, boolean admin, HttpServletRequest request) {
		PermissionAddEvent event = new PermissionAddEvent(this, uid, permission, expiry, admin, request);
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
			PermissionAddSuccessEvent eventSuccess = new PermissionAddSuccessEvent(this, event);
			eventPublisher.publishEvent(eventSuccess);
			if (eventSuccess.isCancelled()) {
				throw new BusinessException(ReturnCode.CANCELLED, "添加权限被取消", request);
			}
			boolean saveResult = this.save(permissionQuery);
			if (!saveResult) {
				throw new BusinessException(ReturnCode.SYSTEM_ERROR, "添加失败，数据库错误", request);
			}
		} else {
			throw new BusinessException(ReturnCode.DATA_EXISTED, "权限已存在(" + permissionQuery.getId() + ")", request);
		}
		userPermissions.invalidate(uid);
	}

	@Override
	public void addPermission(PermissionAddRequest permissionAddRequest, boolean admin, HttpServletRequest request) {
		addPermission(permissionAddRequest.getUid(), permissionAddRequest.getPermission(), permissionAddRequest.getExpiry(), admin, request);
	}

	@Override
	public void removePermission(long uid, String permission, boolean admin, HttpServletRequest request) {
		PermissionRemoveByUidEvent event = new PermissionRemoveByUidEvent(this, uid, permission, admin, request);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
		}
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission, request);
		if (permissionQuery != null) {
			PermissionRemoveSuccessEvent eventSuccess = new PermissionRemoveSuccessEvent(this, permissionQuery, request);
			eventSuccess.setExpired(false);
			eventSuccess.setAdmin(admin);
			eventPublisher.publishEvent(eventSuccess);
			if (eventSuccess.isCancelled()) {
				throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
			}
			this.removeById(permissionQuery.getId());
		}
	}

	@Override
	public void removePermission(long id, boolean admin, HttpServletRequest request) {
		Permission permissionQuery = this.getById(id);
		PermissionRemoveEvent event = new PermissionRemoveEvent(this, permissionQuery, request);
		event.setAdmin(admin);
		eventPublisher.publishEvent(event);
		if (event.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
		}
		if (permissionQuery != null) {
			PermissionRemoveSuccessEvent eventSuccess = new PermissionRemoveSuccessEvent(this, event);
			eventPublisher.publishEvent(eventSuccess);
			if (eventSuccess.isCancelled()) {
				throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
			}
			this.removeById(permissionQuery.getId());
			userPermissions.invalidate(permissionQuery.getUid());
		}
	}

	@Override
	public void removePermission(PermissionRemoveRequest permissionRemoveRequest, boolean admin, HttpServletRequest request) {
		removePermission(permissionRemoveRequest.getUid(), permissionRemoveRequest.getPermission(), admin, request);
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
				PermissionRemoveSuccessEvent event = new PermissionRemoveSuccessEvent(this, permission, request);
				event.setExpired(true);
				event.setAdmin(false);
				eventPublisher.publishEvent(event);
				if (event.isCancelled()) {
					throw new BusinessException(ReturnCode.CANCELLED, "移除权限被取消", request);
				}
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
