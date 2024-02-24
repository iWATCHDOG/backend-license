package cn.watchdog.license.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CaptchaResult implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private int ret;
	private String ticket;
	private Long CaptchaAppId;
	private String bizState;
	private String randstr;
}
