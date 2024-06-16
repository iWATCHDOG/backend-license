package cn.watchdog.license.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ChartRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private String type;
	private Integer days;
}
