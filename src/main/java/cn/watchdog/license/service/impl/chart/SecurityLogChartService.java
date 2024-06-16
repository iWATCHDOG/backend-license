package cn.watchdog.license.service.impl.chart;

import cn.watchdog.license.model.entity.SecurityLog;
import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.SecurityLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SecurityLogChartService implements ChartDataSourceService {

	@Resource
	private SecurityLogService service;

	@Override
	public long getCountForDate(Date date) {
		QueryWrapper<SecurityLog> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda().between(SecurityLog::getCreateTime, date, new Date(date.getTime() + 24 * 60 * 60 * 1000));
		return service.count(queryWrapper);
	}
}