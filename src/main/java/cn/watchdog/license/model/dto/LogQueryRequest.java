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
public class LogQueryRequest extends PageRequest implements Serializable {
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
	private String userAgent;
	private String url;
	private String method;
	private String cookies;
	private String params;
	private String result;
	private Integer httpCode;
	private Long cost;

	private Date createTime;
	private Date updateTime;
}
