package cn.watchdog.license.service.impl;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.events.PhotoAddEvent;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.mapper.PhotoMapper;
import cn.watchdog.license.model.entity.Photo;
import cn.watchdog.license.service.PhotoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@Slf4j
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {
	private final ApplicationEventPublisher eventPublisher;

	public PhotoServiceImpl(ApplicationEventPublisher eventPublisher) {this.eventPublisher = eventPublisher;}

	@Override
	public void savePhotoByMd5(String md5, String ext, long size, HttpServletRequest request) {
		// 查询md5是否已经记录
		Photo photo = this.lambdaQuery().eq(Photo::getMd5, md5).one();
		PhotoAddEvent photoAddEvent = new PhotoAddEvent(this, photo, request);
		if (photo != null) {
			photoAddEvent.setAdded(true);
			eventPublisher.publishEvent(photoAddEvent);
			return;
		}
		// 保存
		photo = new Photo();
		photo.setMd5(md5);
		// 处理ext,若第一个字符为.则去掉
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		photo.setExt(ext);
		photo.setSize(size);
		eventPublisher.publishEvent(photoAddEvent);
		if (photoAddEvent.isCancelled()) {
			throw new BusinessException(ReturnCode.CANCELLED, "添加图片事件被取消", request);
		}
		this.save(photo);
	}

	@Override
	public Path getPhotoPathByMd5(String md5, HttpServletRequest request) {
		Photo photo = this.lambdaQuery().eq(Photo::getMd5, md5).one();
		if (photo == null) {
			return null;
		}
		return Path.of("photos", photo.getMd5() + "." + photo.getExt());
	}
}
