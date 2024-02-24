package cn.watchdog.license.constant;

import cn.watchdog.license.controller.RootController;
import cn.watchdog.license.model.dto.NotifyResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用常量
 */
public interface CommonConstant {
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
	 * captcha的header名字
	 */
	String CAPTCHA_HEADER = "captcha";

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
		long count = RootController.count++;
		notifyResponse.setId(count);
		notifyResponseList.add(notifyResponse);
		request.getSession().setAttribute(NOTIFY_LIST, notifyResponseList);
	}

	static void clearNotifyResponse(HttpServletRequest request) {
		request.getSession().setAttribute(NOTIFY_LIST, new ArrayList<>());
	}
}
