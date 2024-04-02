package cn.watchdog.license.service;

import cn.watchdog.license.model.dto.blacklist.AddBlackListRequest;
import cn.watchdog.license.model.entity.Blacklist;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface BlacklistService extends IService<Blacklist> {
	boolean isBlacklist(String ip, HttpServletRequest request);

	void addBlacklist(AddBlackListRequest addBlackListRequest, HttpServletRequest request);

	void removeBlacklist(Long id, HttpServletRequest request);
}
