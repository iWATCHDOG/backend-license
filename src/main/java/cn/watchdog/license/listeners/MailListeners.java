package cn.watchdog.license.listeners;

import cn.watchdog.license.events.EmailSendEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailListeners {
	@EventListener
	public void emailSend(EmailSendEvent event) {

	}
}
