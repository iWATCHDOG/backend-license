package cn.watchdog.license.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OAuthInfoResult implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private Integer type;
	private Boolean enable;
	private String openId;
}
