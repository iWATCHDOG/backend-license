package cn.watchdog.license.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class NotifyRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private String title;

	private String content;

	private int type;
}
