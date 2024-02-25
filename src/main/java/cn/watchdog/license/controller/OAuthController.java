package cn.watchdog.license.controller;

import cn.watchdog.license.annotation.AuthCheck;
import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.constant.CommonConstant;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.NotifyResponse;
import cn.watchdog.license.model.entity.OAuth;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.model.enums.NotifyType;
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
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthMicrosoftRequest;
import me.zhyd.oauth.request.AuthQqRequest;
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
	@Value("${oauth.github.enable}")
	private boolean githubEnable;
	@Value("${oauth.github.client-id}")
	private String githubClientId;
	@Value("${oauth.github.redirect-uri}")
	private String githubRedirectUri;
	@Value("${oauth.github.client-secret}")
	private String githubClientSecret;
	@Value("${oauth.gitee.enable}")
	private boolean giteeEnable;
	@Value("${oauth.gitee.client-id}")
	private String giteeClientId;
	@Value("${oauth.gitee.redirect-uri}")
	private String giteeRedirectUri;
	@Value("${oauth.gitee.client-secret}")
	private String giteeClientSecret;
	@Value("${oauth.microsoft.enable}")
	private boolean microsoftEnable;
	@Value("${oauth.microsoft.client-id}")
	private String microsoftClientId;
	@Value("${oauth.microsoft.redirect-uri}")
	private String microsoftRedirectUri;
	@Value("${oauth.microsoft.client-secret}")
	private String microsoftClientSecret;
	@Value("${oauth.microsoft.tenant-id}")
	private String microsoftTenantId;
	@Value("${website.url}")
	private String websiteUrl;
	@Value("${oauth.qq.enable}")
	private boolean qqEnable;
	@Value("${oauth.qq.client-id}")
	private String qqClientId;
	@Value("${oauth.qq.redirect-uri}")
	private String qqRedirectUri;
	@Value("${oauth.qq.client-secret}")
	private String qqClientSecret;
	@Value("${oauth.bilibili.enable}")
	private boolean bilibiliEnable;
	@Resource
	private UserService userService;

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

	public AuthGithubRequest getAuthGithubRequest() {
		return new AuthGithubRequest(AuthConfig.builder()
				.clientId(githubClientId)
				.clientSecret(githubClientSecret)
				.redirectUri(githubRedirectUri)
				.build());
	}

	@GetMapping("/github")
	public ResponseEntity<BaseResponse<String>> github(HttpServletRequest request, HttpServletResponse response) {
		AuthGithubRequest authGithubRequest = getAuthGithubRequest();
		String url = authGithubRequest.authorize(AuthStateUtils.createState());
		// 直接重定向到url
		try {
			if (githubEnable) {
				response.sendRedirect(url);
			} else {
				response.sendRedirect(websiteUrl);
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				notifyResponse.setTitle("Github登录暂时不可用");
				notifyResponse.setContent("由于技术和测试原因，Github登录暂时不可用，我们正在努力解决这个问题，给您带来的不便我们深感抱歉。");
				CommonConstant.addNotifyResponse(request, notifyResponse);
			}
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to Github", request);
		}
		return ResultUtil.ok(url);
	}

	@RequestMapping("/github/callback")
	public ResponseEntity<BaseResponse<String>> githubCallback(AuthCallback callback, HttpServletRequest request, HttpServletResponse response) {
		try {
			AuthGithubRequest authGithubRequest = getAuthGithubRequest();
			AuthResponse<AuthUser> authResponse = authGithubRequest.login(callback);
			AuthUser authUser = authResponse.getData();
			User user = userService.oAuthLogin(authUser, OAuthPlatForm.GITHUB, request);
		} catch (Throwable ignored) {
		}
		// 重定向到前端页面
		try {
			response.sendRedirect(websiteUrl);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to website", request);
		}
		return ResultUtil.ok("Request is being processed");
	}

	public AuthGiteeRequest getAuthGiteeRequest() {
		return new AuthGiteeRequest(AuthConfig.builder()
				.clientId(giteeClientId)
				.clientSecret(giteeClientSecret)
				.redirectUri(giteeRedirectUri)
				.build());
	}

	@GetMapping("/gitee")
	public ResponseEntity<BaseResponse<String>> gitee(HttpServletRequest request, HttpServletResponse response) {
		AuthGiteeRequest authGiteeRequest = getAuthGiteeRequest();
		String url = authGiteeRequest.authorize(AuthStateUtils.createState());
		// 直接重定向到url
		try {
			if (giteeEnable) {
				response.sendRedirect(url);
			} else {
				response.sendRedirect(websiteUrl);
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				notifyResponse.setTitle("Gitee登录暂时不可用");
				notifyResponse.setContent("由于技术和测试原因，Gitee登录暂时不可用，我们正在努力解决这个问题，给您带来的不便我们深感抱歉。");
				CommonConstant.addNotifyResponse(request, notifyResponse);
			}
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to Gitee", request);
		}
		return ResultUtil.ok(url);
	}

	@RequestMapping("/gitee/callback")
	public ResponseEntity<BaseResponse<String>> giteeCallback(AuthCallback callback, HttpServletRequest request, HttpServletResponse response) {
		try {
			AuthGiteeRequest authGiteeRequest = getAuthGiteeRequest();
			AuthResponse<AuthUser> authResponse = authGiteeRequest.login(callback);
			AuthUser authUser = authResponse.getData();
			User user = userService.oAuthLogin(authUser, OAuthPlatForm.GITEE, request);
		} catch (Throwable ignored) {
		}
		// 重定向到前端页面
		try {
			response.sendRedirect(websiteUrl);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to website", request);
		}

		return ResultUtil.ok("Request is being processed");
	}

	public AuthMicrosoftRequest getAuthMicrosoftRequest() {
		return new AuthMicrosoftRequest(AuthConfig.builder()
				.clientId(microsoftClientId)
				.clientSecret(microsoftClientSecret)
				.redirectUri(microsoftRedirectUri)
				.tenantId(microsoftTenantId)
				.build());
	}

	@GetMapping("/microsoft")
	public ResponseEntity<BaseResponse<String>> microsoft(HttpServletRequest request, HttpServletResponse response) {
		AuthMicrosoftRequest authMicrosoftRequest = getAuthMicrosoftRequest();
		String url = authMicrosoftRequest.authorize(AuthStateUtils.createState());
		// 直接重定向到url
		try {
			if (microsoftEnable) {
				response.sendRedirect(url);
			} else {
				response.sendRedirect(websiteUrl);
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				notifyResponse.setTitle("微软登录暂时不可用");
				notifyResponse.setContent("由于技术和测试原因，微软登录暂时不可用，我们正在努力解决这个问题，给您带来的不便我们深感抱歉。");
				CommonConstant.addNotifyResponse(request, notifyResponse);
			}
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to Microsoft", request);
		}
		return ResultUtil.ok(url);
	}

	@RequestMapping("/microsoft/callback")
	public ResponseEntity<BaseResponse<String>> microsoftCallback(AuthCallback callback, HttpServletRequest request, HttpServletResponse response) {
		try {
			AuthMicrosoftRequest authMicrosoftRequest = getAuthMicrosoftRequest();
			AuthResponse<AuthUser> authResponse = authMicrosoftRequest.login(callback);
			AuthUser authUser = authResponse.getData();
			User user = userService.oAuthLogin(authUser, OAuthPlatForm.MICROSOFT, request);
		} catch (Throwable ignored) {
		}
		// 重定向到前端页面
		try {
			response.sendRedirect(websiteUrl);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to website", request);
		}

		return ResultUtil.ok("Request is being processed");
	}

	public AuthQqRequest getAuthQqRequest() {
		return new AuthQqRequest(AuthConfig.builder()
				.clientId(qqClientId)
				.clientSecret(qqClientSecret)
				.redirectUri(qqRedirectUri)
				.build());
	}

	@GetMapping("/qq")
	public ResponseEntity<BaseResponse<String>> qq(HttpServletRequest request, HttpServletResponse response) {
		AuthQqRequest authQqRequest = getAuthQqRequest();
		String url = authQqRequest.authorize(AuthStateUtils.createState());
		// 直接重定向到url
		try {
			if (qqEnable) {
				response.sendRedirect(url);
			} else {
				response.sendRedirect(websiteUrl);
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				notifyResponse.setTitle("QQ登录暂时不可用");
				notifyResponse.setContent("由于技术和测试原因，QQ登录暂时不可用，我们正在努力解决这个问题，给您带来的不便我们深感抱歉。");
				CommonConstant.addNotifyResponse(request, notifyResponse);
			}
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to QQ", request);
		}
		return ResultUtil.ok(url);
	}

	@RequestMapping("/qq/callback")
	public ResponseEntity<BaseResponse<String>> qqCallback(AuthCallback callback, HttpServletRequest request, HttpServletResponse response) {
		try {
			AuthQqRequest authQqRequest = getAuthQqRequest();
			AuthResponse<AuthUser> authResponse = authQqRequest.login(callback);
			AuthUser authUser = authResponse.getData();
			User user = userService.oAuthLogin(authUser, OAuthPlatForm.QQ, request);
		} catch (Throwable ignored) {
		}
		// 重定向到前端页面
		try {
			response.sendRedirect(websiteUrl);
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to website", request);
		}
		return ResultUtil.ok("Request is being processed");
	}

	@GetMapping("/bilibili")
	public ResponseEntity<BaseResponse<String>> bilibili(HttpServletRequest request, HttpServletResponse response) {
		// 直接重定向到url
		try {
			if (bilibiliEnable) {
			} else {
				response.sendRedirect(websiteUrl);
				NotifyResponse notifyResponse = new NotifyResponse();
				notifyResponse.setType(NotifyType.ERROR);
				notifyResponse.setTitle("哔哩哔哩登录暂时不可用");
				notifyResponse.setContent("由于技术和测试原因，哔哩哔哩登录暂时不可用，我们正在努力解决这个问题，给您带来的不便我们深感抱歉。");
				CommonConstant.addNotifyResponse(request, notifyResponse);
			}
		} catch (IOException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to redirect to QQ", request);
		}
		return ResultUtil.ok("Request is being processed");
	}
}
