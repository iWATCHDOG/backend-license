package cn.watchdog.license.service.impl;

import cn.watchdog.license.mapper.UserMapper;
import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	@Resource
	private UserMapper userMapper;
}
