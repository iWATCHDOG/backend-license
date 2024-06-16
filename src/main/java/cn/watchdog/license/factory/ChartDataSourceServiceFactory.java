package cn.watchdog.license.factory;

import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.impl.chart.LogChartService;
import cn.watchdog.license.service.impl.chart.SecurityLogChartService;
import cn.watchdog.license.service.impl.chart.UserCreateChartService;
import cn.watchdog.license.util.chart.ChartType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;

@Component
public class ChartDataSourceServiceFactory {

	private final EnumMap<ChartType, ChartDataSourceService> serviceMap = new EnumMap<>(ChartType.class);

	@Resource
	private UserCreateChartService userCreateChartService;
	@Resource
	private SecurityLogChartService securityLogChartService;
	@Resource
	private LogChartService logChartService;

	@PostConstruct
	public void registerServices() {
		serviceMap.put(ChartType.USER_CREATE, userCreateChartService);
		serviceMap.put(ChartType.SECURITY_LOG, securityLogChartService);
		serviceMap.put(ChartType.LOG, logChartService);
	}

	public ChartDataSourceService getService(ChartType chatType) {
		return serviceMap.get(chatType);
	}
}