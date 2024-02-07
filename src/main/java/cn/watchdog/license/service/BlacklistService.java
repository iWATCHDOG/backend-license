package cn.watchdog.license.service;

import cn.watchdog.license.model.entity.Blacklist;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface BlacklistService extends IService<Blacklist> {
	boolean isBlacklist(String ip);

	void addBlacklist(Long id, HttpServletRequest request);
}
