package cn.watchdog.license.constant;

import cn.watchdog.license.model.dto.NotifyResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 通用常量
 */
public interface CommonConstant {
	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(16);

	List<NotifyResponse> ROOT_NOTIFY_LIST = new ArrayList<>();

	/**
	 * 升序
	 */
	String SORT_ORDER_ASC = "ascend";

	/**
	 * 降序
	 */
	String SORT_ORDER_DESC = " descend";

	/**
	 * 通知列表
	 */
	String NOTIFY_LIST = "notifyList";

	static List<NotifyResponse> getNotifyResponse(HttpServletRequest request) {
		return (List<NotifyResponse>) request.getSession().getAttribute(NOTIFY_LIST);
	}

	static void addNotifyResponse(HttpServletRequest request, NotifyResponse notifyResponse) {
		List<NotifyResponse> notifyResponseList = getNotifyResponse(request);
		if (notifyResponseList == null) {
			notifyResponseList = new ArrayList<>();
		}
		notifyResponseList.add(notifyResponse);
		List<NotifyResponse> finalNotifyResponseList = notifyResponseList;
		scheduler.schedule(() -> request.getSession().setAttribute(NOTIFY_LIST, finalNotifyResponseList), 3, TimeUnit.SECONDS);
	}

	static void clearNotifyResponse(HttpServletRequest request) {
		request.getSession().setAttribute(NOTIFY_LIST, new ArrayList<>());
	}
}
