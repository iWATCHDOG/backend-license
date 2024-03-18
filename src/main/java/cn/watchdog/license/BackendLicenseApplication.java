package cn.watchdog.license;

import cn.watchdog.license.filter.RateLimitingFilter;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static cn.watchdog.license.command.ReleaseCommand.*;

@SpringBootApplication
@MapperScan("cn.watchdog.license.mapper")
@EnableScheduling
@EnableAsync
@EnableCaching
@Slf4j
public class BackendLicenseApplication {
	public static void main(String[] args) {
		boolean run = true;
		for (String arg : args) {
			if (arg.startsWith("release")) {
				release();
				run = false;
			}
		}
		if (run) {
			SpringApplication.run(BackendLicenseApplication.class, args);
		}
	}

	@Bean
	public Filter rateLimitingFilter() {
		return new RateLimitingFilter();
	}
}
