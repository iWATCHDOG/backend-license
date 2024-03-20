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
	Set<Permission> getUserPermissions(long uid, HttpServletRequest request);

	boolean checkPermission(long uid, String permission, HttpServletRequest request);

	boolean checkPermission(Permission permission, HttpServletRequest request);

	Permission getPermission(long uid, String permission, HttpServletRequest request);

	void updatePermission(PermissionUpdateRequest permissionUpdateRequest, boolean admin, HttpServletRequest request);

	void addPermission(long uid, String permission, long expiry, boolean admin, HttpServletRequest request);

	void addPermission(PermissionAddRequest permissionAddRequest, boolean admin, HttpServletRequest request);

	void removePermission(long uid, String permission, boolean admin, HttpServletRequest request);

	void removePermission(long id, boolean admin, HttpServletRequest request);

	void removePermission(PermissionRemoveRequest permissionRemoveRequest, boolean admin, HttpServletRequest request);

	void updatePermission(long uid, String permission, long expiry, boolean admin, HttpServletRequest request);

	List<Permission> getGroups(long uid, HttpServletRequest request);

	Group getMaxPriorityGroup(long uid, HttpServletRequest request);

	Permission getMaxPriorityGroupP(long uid, HttpServletRequest request);
}
