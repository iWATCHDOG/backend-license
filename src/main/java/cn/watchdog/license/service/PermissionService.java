package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.PermissionAddRequest;
import cn.watchdog.license.model.dto.PermissionRemoveRequest;
import cn.watchdog.license.model.entity.Permission;
import cn.watchdog.license.model.enums.Group;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

public interface PermissionService extends IService<Permission> {
	Set<Permission> getUserPermissions(long uid);

	boolean checkPermission(long uid, String permission);

	Permission getPermission(long uid, String permission);

	void addPermission(long uid, String permission, long expiry);

	void addPermission(PermissionAddRequest permissionAddRequest);

	void removePermission(long uid, String permission);

	void removePermission(PermissionRemoveRequest permissionRemoveRequest);

	void updatePermission(long uid, String permission, long expiry);

	List<Permission> getGroups(long uid);

	Group getMaxPriorityGroup(long uid);

	Permission getMaxPriorityGroupP(long uid);
}
