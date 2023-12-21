package cn.watchdog.license.service.impl;

import cn.watchdog.license.mapper.SecurityLogMapper;
import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.service.SecurityLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SecurityLogServiceImpl extends ServiceImpl<SecurityLogMapper, SecurityLog> implements SecurityLogService {
	@Resource
	private SecurityLogMapper securityLogMapper;
}
