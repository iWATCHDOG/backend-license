package cn.watchdog.license.service.impl;

import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.ChartService;
import cn.watchdog.license.service.UserService;
import cn.watchdog.license.util.CaffeineFactory;
import cn.watchdog.license.util.chart.ChartData;
import cn.watchdog.license.util.chart.ChatType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChartServiceImpl implements ChartService {
	// 缓存用户数据（只缓存今天的数据）
	private static final Cache<ChatType, Long> chartCache = CaffeineFactory.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
	private static final HashMap<String, Long> userCache = new HashMap<>();
	@Resource
	private UserService userService;

	@Override
	public List<ChartData> getCreateUserChart(int days) {
		List<ChartData> chartDataList = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			Date date = new Date(System.currentTimeMillis() - (long) i * 24 * 60 * 60 * 1000);
			String key = String.format("%tF", date);
			long count = 0;
			// 如果是今天的数据，从缓存中获取
			if (i == 0) {
				Long todayCount = chartCache.getIfPresent(ChatType.USER_CREATE);
				if (todayCount != null) {
					count = todayCount;
				} else {
					QueryWrapper<User> queryWrapper = new QueryWrapper<>();
					queryWrapper.lambda().ge(User::getCreateTime, key + " 00:00:00").lt(User::getCreateTime, key + " 23:59:59");
					count = userService.count(queryWrapper);
					chartCache.put(ChatType.USER_CREATE, count);
				}
			} else {
				// 不是今天的数据，直接从缓存中获取
				Long oc = userCache.get(key);
				if (oc == null) {
					QueryWrapper<User> queryWrapper = new QueryWrapper<>();
					queryWrapper.lambda().ge(User::getCreateTime, new Date(date.getTime())).lt(User::getCreateTime, new Date(date.getTime() + 24 * 60 * 60 * 1000));
					oc = userService.count(queryWrapper);
					userCache.put(key, oc);
				} else {
					count = oc;
				}
			}
			chartDataList.add(new ChartData(key, count));
		}
		return chartDataList;
	}
}
