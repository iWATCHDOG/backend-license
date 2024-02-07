package cn.watchdog.license.model.dto.permission;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PermissionUpdateRequest implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	private Long id;
	private String permission;
	private Long expiry;
}
