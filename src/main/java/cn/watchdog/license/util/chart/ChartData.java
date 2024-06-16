package cn.watchdog.license.util.chart;

import lombok.Data;

@Data
public class ChartData {
	private String data;
	private Long count;

	public ChartData(String date, Long count) {
		this.data = date;
		this.count = count;
	}
}
