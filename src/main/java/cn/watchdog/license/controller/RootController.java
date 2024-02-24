package cn.watchdog.license.controller;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.model.dto.NotifyRequest;
import cn.watchdog.license.model.dto.NotifyResponse;
import cn.watchdog.license.model.enums.NotifyType;
import cn.watchdog.license.util.VersionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
@Slf4j
public class RootController {
	public static long count = 1;

	@GetMapping("/version")
	public ResponseEntity<BaseResponse<VersionUtil.Version>> getVersion(HttpServletRequest request) {
		return ResultUtil.ok(VersionUtil.getVersion());
	}

	@GetMapping("/ping")
	public ResponseEntity<BaseResponse<String>> ping(HttpServletRequest request) {
		return ResultUtil.ok("pong");
	}

	@GetMapping("/notify")
	public ResponseEntity<BaseResponse<List<NotifyResponse>>> notify(HttpServletRequest request) {
		List<NotifyResponse> list = CommonConstant.getNotifyResponse(request);
		CommonConstant.clearNotifyResponse(request);
		return ResultUtil.ok(list);
	}

	@PostMapping("/notify")
	public ResponseEntity<BaseResponse<String>> notify(NotifyRequest notifyRequest, HttpServletRequest request) {
		NotifyResponse notifyResponse = new NotifyResponse();
		notifyResponse.setType(NotifyType.WARNING);
		notifyResponse.setTitle(notifyRequest.getTitle());
		notifyResponse.setContent(notifyRequest.getContent());
		CommonConstant.addNotifyResponse(request, notifyResponse);
		return ResultUtil.ok("ok");
	}

}
