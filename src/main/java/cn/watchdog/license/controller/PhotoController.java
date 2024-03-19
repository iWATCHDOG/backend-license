package cn.watchdog.license.controller;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.common.StatusCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.service.PhotoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/photo")
@Slf4j
public class PhotoController {
	@Resource
	private PhotoService photoService;

	@GetMapping("/{md5}")
	public ResponseEntity<InputStreamResource> getPhotoByMd5(@PathVariable("md5") String md5, HttpServletRequest request) {
		if (StringUtils.isAnyBlank(md5)) {
			throw new BusinessException(ReturnCode.PARAMS_ERROR, "参数不完全", request);
		}
		Path path = photoService.getPhotoPathByMd5(md5, request);
		if (path == null) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "图片不存在", request);
		}

		File file = new File(path.toString());
		if (!file.exists()) {
			throw new BusinessException(ReturnCode.NOT_FOUND_ERROR, "图片文件不存在", request);
		}
		try {
			InputStream is = new FileInputStream(file);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file.toPath()));
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getName());
			InputStreamResource inputStreamResource = new InputStreamResource(is);
			return new ResponseEntity<>(inputStreamResource, headers, StatusCode.OK);
		} catch (Throwable e) {
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "预览系统异常", request);
		}
	}
}