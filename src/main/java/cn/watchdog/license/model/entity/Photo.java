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

@TableName(value = "photo")
@Data
public class Photo implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long pid;

	private String md5;
	private String ext;
	private Long size;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;
}
