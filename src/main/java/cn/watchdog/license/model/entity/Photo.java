package cn.watchdog.license.model.entity;

import com.alibaba.excel.annotation.ExcelProperty;
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
	@ExcelProperty(value = "PID", index = 0)
	private Long pid;

	@ExcelProperty(value = "MD5", index = 1)
	private String md5;
	@ExcelProperty(value = "后缀", index = 2)
	private String ext;
	@ExcelProperty(value = "大小", index = 3)
	private Long size;

	@ExcelProperty(value = "创建时间", index = 4)
	private Date createTime;

	@ExcelProperty(value = "更新时间", index = 5)
	private Date updateTime;

	@TableLogic
	@ExcelProperty(value = "可用性", index = 6)
	private Boolean available;
}
