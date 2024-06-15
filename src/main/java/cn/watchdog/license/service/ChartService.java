package cn.watchdog.license.service;

import cn.watchdog.license.util.chart.ChartData;
import cn.watchdog.license.util.chart.ChartType;

import java.util.List;

public interface ChartService {
	List<ChartData> getChartDataForType(ChartType chatType, int days);
}
