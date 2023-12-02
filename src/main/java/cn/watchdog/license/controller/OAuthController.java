package cn.watchdog.license.controller;

import cn.watchdog.license.common.BaseResponse;
import cn.watchdog.license.common.ResultUtil;
import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.util.oauth.GithubUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	@GetMapping("/github")
	public ResponseEntity<BaseResponse<String>> github(HttpServletRequest request) {
		String url = String.format("https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s", githubClientId, githubRedirectUri);
		return ResultUtil.ok(url);
	}

	@GetMapping("/github/callback")
	public ResponseEntity<BaseResponse<GithubUser>> githubCallback(HttpServletRequest request) {
		String code = request.getParameter("code");

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> params = new HashMap<>();
		params.put("client_id", githubClientId);
		params.put("client_secret", githubClientSecret);
		params.put("code", code);
		params.put("redirect_uri", githubRedirectUri);

		HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);
		ResponseEntity<String> responseEntity = restTemplate.exchange("https://github.com/login/oauth/access_token", HttpMethod.POST, requestEntity, String.class);

		String accessToken = parseAccessToken(Objects.requireNonNull(responseEntity.getBody()));

		HttpHeaders userHeaders = new HttpHeaders();
		userHeaders.set("Authorization", "token " + accessToken);
		HttpEntity<String> userEntity = new HttpEntity<>(userHeaders);
		ResponseEntity<String> userResponseEntity = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, userEntity, String.class);

		ObjectMapper mapper = new ObjectMapper();
		try {
			GithubUser githubUser = mapper.readValue(userResponseEntity.getBody(), GithubUser.class);
			return ResultUtil.ok(githubUser);
		} catch (JsonProcessingException e) {
			throw new BusinessException(ReturnCode.OPERATION_ERROR, "Failed to parse Github user info");
		}
	}

	private String parseAccessToken(String response) {
		String tokenPrefix = "access_token=";
		int start = response.indexOf(tokenPrefix) + tokenPrefix.length();
		int end = response.indexOf("&", start);
		if (end == -1) { // If there is no "&" after the start, take the rest of the string
			end = response.length();
		}
		return response.substring(start, end);
	}
}
