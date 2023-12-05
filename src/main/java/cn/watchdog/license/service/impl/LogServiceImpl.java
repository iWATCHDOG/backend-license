package cn.watchdog.license.service.impl;

import cn.watchdog.license.mapper.LogMapper;
import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.LogService;
import cn.watchdog.license.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
	@Resource
	private UserService userService;

	@Override
	@Async
	public void addLog(Log log, HttpServletRequest request) {
		Log l = new Log();
		BeanUtils.copyProperties(log, l);
		if (l.getHttpCode() == null) {
			l.setHttpCode(200);
		}
		if (l.getCost() == null) {
			l.setCost(-1L);
		}
		if (l.getUid() == null) {
			User user = userService.getLoginUserIgnoreError(request);
			if (user != null) {
				l.setUid(user.getUid());
			}
		}
		this.save(l);
	}
}
