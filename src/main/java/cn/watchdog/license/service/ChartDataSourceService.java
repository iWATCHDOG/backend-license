package cn.watchdog.license.service;

import java.util.Date;

public interface ChartDataSourceService {
	/**
	 * 获取特定日期的数据统计
	 *
	 * @param date 统计的日期
	 * @return 数据统计结果
	 */
	long getCountForDate(Date date);
}