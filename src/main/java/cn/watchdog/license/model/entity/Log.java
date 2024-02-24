package cn.watchdog.license.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "log")
@Data
public class Log implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	private Long uid;
	private String requestId;
	private String ip;
	private String headers;
	private String url;
	private String method;
	private String params;
	private String result;
	private Integer httpCode;
	private Long cost;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
