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

@TableName(value = "one_time_password")
@Data
public class OneTimePassword implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	@ExcelProperty(value = "ID", index = 0)
	private Long id;
	@ExcelProperty(value = "UID", index = 1)
	private Long uid;
	@ExcelProperty(value = "2FA密钥", index = 2)
	private String secret;

	@ExcelProperty(value = "创建时间", index = 4)
	private Date createTime;

	@ExcelProperty(value = "更新时间", index = 5)
	private Date updateTime;

	@TableLogic
	@ExcelProperty(value = "可用性", index = 6)
	private Boolean available;
}
