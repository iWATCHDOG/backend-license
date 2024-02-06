package cn.watchdog.license;

import cn.watchdog.license.filter.RateLimitingFilter;
import jakarta.servlet.Filter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.watchdog.license.mapper")
@EnableScheduling
@EnableAsync
@EnableCaching
public class BackendLicenseApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendLicenseApplication.class, args);
	}

	@Bean
	public Filter rateLimitingFilter() {
		return new RateLimitingFilter();
	}
}
