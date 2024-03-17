package cn.watchdog.license.model.vo;

import cn.watchdog.license.model.entity.SecurityLog;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class SecurityLogVO implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	private Long uid;
	private SecurityLog.Avatar avatar;
	private String title;
	private String types;
	private String ip;
	private String info;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
