package cn.watchdog.license.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
	private static final long MAX_REQUESTS_PER_PERIOD = 15 * 5; // 设置最大请求次数
	private static final long PERIOD_IN_MILLIS = 15000; // 15秒

	private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

	public boolean isOverLimit(String ipAddress) {
		return requestCounts.compute(ipAddress, (ip, counter) -> {
			if (counter == null || System.currentTimeMillis() - counter.timestamp > PERIOD_IN_MILLIS) {
				return new RequestCounter(1, System.currentTimeMillis());
			}

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
