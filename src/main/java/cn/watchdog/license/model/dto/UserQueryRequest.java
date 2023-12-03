package cn.watchdog.license.model.dto;

import cn.watchdog.license.common.PageRequest;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long uid;
	private String username;
	private String password;
	private String email;
	private String phone;
	private Integer gender;
	private String avatar;
	private Integer status;
	private Date createTime;
	private Date updateTime;
}
