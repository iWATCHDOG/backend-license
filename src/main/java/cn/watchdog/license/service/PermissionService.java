package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.permission.PermissionAddRequest;
import cn.watchdog.license.model.dto.permission.PermissionRemoveRequest;
import cn.watchdog.license.model.dto.permission.PermissionUpdateRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.enums.Group;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

public interface PermissionService extends IService<Permission> {
	Set<Permission> getUserPermissions(long uid);

	boolean checkPermission(long uid, String permission);

	Permission getPermission(long uid, String permission);

	void updatePermission(PermissionUpdateRequest permissionUpdateRequest, boolean admin, HttpServletRequest request);

	void addPermission(long uid, String permission, long expiry, boolean admin, HttpServletRequest request);

	void addPermission(PermissionAddRequest permissionAddRequest, boolean admin, HttpServletRequest request);

	void removePermission(long uid, String permission, boolean admin, HttpServletRequest request);

	void removePermission(long id, boolean admin, HttpServletRequest request);

	void removePermission(PermissionRemoveRequest permissionRemoveRequest, boolean admin, HttpServletRequest request);

	void updatePermission(long uid, String permission, long expiry, boolean admin);

	List<Permission> getGroups(long uid);

	Group getMaxPriorityGroup(long uid);

	Permission getMaxPriorityGroupP(long uid);
}
