package cn.watchdog.license.service.impl.chart;

import cn.watchdog.license.model.entity.Log;
import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.LogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogChartService implements ChartDataSourceService {

	@Resource
	private LogService service;

	@Override
	public long getCountForDate(Date date) {
		QueryWrapper<Log> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda().between(Log::getCreateTime, date, new Date(date.getTime() + 24 * 60 * 60 * 1000));
		return service.count(queryWrapper);
	}
}