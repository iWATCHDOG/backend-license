package cn.watchdog.license.model.dto;

import cn.watchdog.license.model.enums.NotifyType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class NotifyResponse implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private long id;

	private String title;

	private String content;

	private int type;

	public void setType(NotifyType type) {
		this.type = type.getValue();
	}
}
