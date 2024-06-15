package cn.watchdog.license.service.impl.chart;

import cn.watchdog.license.model.entity.User;
import cn.watchdog.license.service.ChartDataSourceService;
import cn.watchdog.license.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserCreateService implements ChartDataSourceService {

	@Resource
	private UserService userService;

	@Override
	public long getCountForDate(Date date) {
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda().between(User::getCreateTime, date, new Date(date.getTime() + 24 * 60 * 60 * 1000));
		return userService.count(queryWrapper);
	}
}