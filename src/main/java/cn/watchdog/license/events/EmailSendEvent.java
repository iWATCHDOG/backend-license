package cn.watchdog.license.events;

import jakarta.mail.internet.MimeMessage;
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
	private boolean cancelled = false;

	public EmailSendEvent(Object source, String to, MimeMessage mimeMessage) {
		super(source);
		this.to = to;
		this.mimeMessage = mimeMessage;
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
