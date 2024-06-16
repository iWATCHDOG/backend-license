package cn.watchdog.license.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
	private static final long MAX_REQUESTS_PER_PERIOD = 40; // 设置最大请求次数
	private static final long PERIOD_IN_MILLIS = 1000; // 1秒

	private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

	public boolean isOverLimit(String ipAddress) {
		return requestCounts.compute(ipAddress, (ip, counter) -> {
			// 如果counter为null或者当前时间与counter的时间戳之差大于 {PERIOD_IN_MILLIS} 秒，则重置counter
			if (counter == null || System.currentTimeMillis() - counter.timestamp > PERIOD_IN_MILLIS) {
				return new RequestCounter(1, System.currentTimeMillis());
			}

			// 如果当前时间与counter的时间戳之差小于 {PERIOD_IN_MILLIS} 秒，则判断当前请求次数是否大于 {MAX_REQUESTS_PER_PERIOD} 次
			if (counter.count.incrementAndGet() > MAX_REQUESTS_PER_PERIOD) {
				return counter;
			}

			return new RequestCounter(counter.count.get(), counter.timestamp);
		}).count.get() > MAX_REQUESTS_PER_PERIOD;
	}

	private static class RequestCounter {
		private final AtomicInteger count;
		private final long timestamp;

		RequestCounter(int count, long timestamp) {
			this.count = new AtomicInteger(count);
			this.timestamp = timestamp;
		}
	}
}
