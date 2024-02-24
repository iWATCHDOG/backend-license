package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.OAuthPlatForm;
import cn.watchdog.license.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/oauth")
@Slf4j
public class OAuthController {
	@Value("${oauth.github.client-id}")
	private String githubClientId;
	@Value("${oauth.github.redirect-uri}")
	private String githubRedirectUri;
	@Value("${oauth.github.client-secret}")
	private String githubClientSecret;
	@Value("${website.url}")
	private String websiteUrl;
	@Resource
	private UserService userService;

	@GetMapping("/github")
	public ResponseEntity<BaseResponse<String>> github(HttpServletRequest request, HttpServletResponse response) {
		AuthGithubRequest authGithubRequest = getAuthGithubRequest();
		String url = authGithubRequest.authorize(AuthStateUtils.createState());
		// 直接重定向到url
		try {
			response.sendRedirect(url);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to Github", request);
		}
		return ResultUtil.ok(url);
	}

	public AuthGithubRequest getAuthGithubRequest() {
		return new AuthGithubRequest(AuthConfig.builder()
				.clientId(githubClientId)
				.clientSecret(githubClientSecret)
				.redirectUri(githubRedirectUri)
				.build());
	}

	@GetMapping("/info")
	@AuthCheck()
	public ResponseEntity<BaseResponse<String>> oauth(Integer type, HttpServletRequest request, HttpServletResponse response) {
		OAuthPlatForm oAuthPlatForm = OAuthPlatForm.valueOf(type);
		if (oAuthPlatForm == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "Invalid type", request);
		}
		User user = userService.getLoginUser(request);
		OAuth oAuth = userService.getOAuthByUidAndPlatform(user, oAuthPlatForm, request);
		if (oAuth == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "OAuth not found", request);
		}
		return ResultUtil.ok(oAuth.getOpenId());
	}

	@DeleteMapping("/unbind")
	public ResponseEntity<BaseResponse<Boolean>> unbind(int type, HttpServletRequest request) {
		OAuthPlatForm oAuthPlatForm = OAuthPlatForm.valueOf(type);
		if (oAuthPlatForm == null) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "Invalid code", request);
		}
		userService.unbind(oAuthPlatForm, request);
		return ResultUtil.ok(true);
	}

	@RequestMapping("/github/callback")
	public ResponseEntity<BaseResponse<String>> githubCallback(AuthCallback callback, HttpServletRequest request, HttpServletResponse response) {
		AuthGithubRequest authGithubRequest = getAuthGithubRequest();
		AuthResponse<AuthUser> authResponse = authGithubRequest.login(callback);
		AuthUser authUser = authResponse.getData();
		User user = userService.oAuthLogin(authUser, OAuthPlatForm.GITHUB, request);
		// 重定向到前端页面
		try {
			response.sendRedirect(websiteUrl);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to website", request);
		}
		return ResultUtil.ok("Request is being processed");
	}
}
