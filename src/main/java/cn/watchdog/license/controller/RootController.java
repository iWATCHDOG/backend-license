package cn.watchdog.license.controller;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.util.VersionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
public class RootController {
	@GetMapping("/version")
	public ResponseEntity<BaseResponse<VersionUtil.Version>> getVersion(HttpServletRequest request) {
		return ResultUtil.ok(VersionUtil.getVersion());
	}

	@GetMapping("/ping")
	public ResponseEntity<BaseResponse<String>> ping(HttpServletRequest request) {
		return ResultUtil.ok("pong");
	}
}
