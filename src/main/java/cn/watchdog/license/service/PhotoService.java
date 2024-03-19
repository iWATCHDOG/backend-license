package cn.watchdog.license.service;

import cn.watchdog.license.model.entity.Photo;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Path;

public interface PhotoService extends IService<Photo> {
	void savePhotoByMd5(String md5, String ext, long size, HttpServletRequest request);

	Path getPhotoPathByMd5(String md5, HttpServletRequest request);
}
