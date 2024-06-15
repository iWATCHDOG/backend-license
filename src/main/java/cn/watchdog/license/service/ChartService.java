package cn.watchdog.license.service;

import cn.watchdog.license.util.chart.ChartData;

import java.util.List;

public interface ChartService {
	List<ChartData> getCreateUserChart(int days);
}
