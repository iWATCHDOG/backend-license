package cn.watchdog.license.service;

import cn.watchdog.license.model.entity.Log;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;

public interface LogService extends IService<Log> {
	@Async
	public void addLog(Log log, HttpServletRequest request);
}
