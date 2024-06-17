package cn.watchdog.license.events;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class EmailSendEvent extends ApplicationEvent implements Cancellable {
	@Setter
	private String to;
	@Setter
	private MimeMessage mimeMessage;
	private final HttpServletRequest request;
	private boolean cancelled = false;

	public EmailSendEvent(Object source, String to, MimeMessage mimeMessage, HttpServletRequest request) {
		super(source);
		this.to = to;
		this.mimeMessage = mimeMessage;
		this.request = request;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
