package cn.watchdog.license.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserCreateRequest implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	private String username;
	private String password;
	private String email;
	private String phone;
	private String code;
}
