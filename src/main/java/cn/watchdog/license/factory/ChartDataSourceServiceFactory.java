package cn.watchdog.license.factory;

import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.impl.chart.UserCreateService;
import cn.watchdog.license.util.chart.ChartType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;

@Component
public class ChartDataSourceServiceFactory {

	private final EnumMap<ChartType, ChartDataSourceService> serviceMap = new EnumMap<>(ChartType.class);

	@Resource
	public void registerServices(UserCreateService userCreateService) {
		serviceMap.put(ChartType.USER_CREATE, userCreateService);
		// 注册更多的服务
	}

	public ChartDataSourceService getService(ChartType chatType) {
		return serviceMap.get(chatType);
	}
}