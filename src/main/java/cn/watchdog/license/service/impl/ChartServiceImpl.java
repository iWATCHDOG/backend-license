package cn.watchdog.license.service.impl;

import cn.watchdog.license.factory.ChartDataSourceServiceFactory;
import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.ChartService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.chart.ChartData;
import cn.watchdog.license.util.chart.ChartType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChartServiceImpl implements ChartService {
	private static final EnumMap<ChartType, Cache<String, Long>> shortTermCacheMap = new EnumMap<>(ChartType.class);
	private static final EnumMap<ChartType, Cache<String, Long>> longTermCacheMap = new EnumMap<>(ChartType.class);

	static {
		// 初始化短期缓存（今天的数据，30秒过期）
		for (ChartType chatType : ChartType.values()) {
			shortTermCacheMap.put(chatType, CaffeineFactory.newBuilder()
					.expireAfterWrite(30, TimeUnit.SECONDS).build());
		}

		// 初始化长期缓存（非今天的数据，永不过期）
		for (ChartType chatType : ChartType.values()) {
			longTermCacheMap.put(chatType, Caffeine.newBuilder()
					.build());
		}
	}

	@Resource
	private ChartDataSourceServiceFactory dataSourceServiceFactory;

	@Override
	public List<ChartData> getChartDataForType(ChartType chatType, int days) {
		List<ChartData> chartDataList = new ArrayList<>();
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDateTime now = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

		for (int i = 0; i < days; i++) {
			LocalDateTime targetDateTime = now.minusDays(i);
			Date date = toDateFromLocalDateTime(targetDateTime, zoneId);
			String key = String.format("%tF", date);
			long count = retrieveCountForDate(chatType, key, date, i == 0);
			chartDataList.add(new ChartData(key, count));
		}

		return chartDataList;
	}

	private Date toDateFromLocalDateTime(LocalDateTime dateTime, ZoneId zoneId) {
		return Date.from(dateTime.atZone(zoneId).toInstant());
	}

	private long retrieveCountForDate(ChartType chartType, String key, Date date, boolean isToday) {
		Cache<String, Long> cache = isToday ? shortTermCacheMap.get(chartType) : longTermCacheMap.get(chartType);
		Long count = cache.getIfPresent(key);

		if (count == null) {
			count = fetchCountFromService(chartType, date);
			cache.put(key, count);
		}

		return count;
	}

	private long fetchCountFromService(ChartType chartType, Date date) {
		ChartDataSourceService service = dataSourceServiceFactory.getService(chartType);
		if (service == null) {
			log.warn("No service found for ChartType: {}", chartType);
			return 0;
		}
		return service.getCountForDate(date);
	}
}