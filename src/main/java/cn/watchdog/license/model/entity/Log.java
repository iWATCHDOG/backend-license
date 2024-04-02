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
	@ExcelProperty(value = "ID", index = 0)
	private Long id;

	@ExcelProperty(value = "UID", index = 1)
	private Long uid;

	@ExcelProperty(value = "请求ID", index = 2)
	private String requestId;

	@ExcelProperty(value = "IP", index = 3)
	private String ip;

	@ExcelProperty(value = "请求头", index = 4)
	private String headers;

	@ExcelProperty(value = "URL", index = 5)
	private String url;

	@ExcelProperty(value = "请求方法", index = 6)
	private String method;

	@ExcelProperty(value = "请求参数", index = 7)
	private String params;

	@ExcelProperty(value = "结果", index = 8)
	private String result;

	@ExcelProperty(value = "请求代码", index = 9)
	private Integer httpCode;

	@ExcelProperty(value = "花费时间", index = 10)
	private Long cost;

	@ExcelProperty(value = "创建时间", index = 11)
	private Date createTime;

	@ExcelProperty(value = "更新时间", index = 12)
	private Date updateTime;

	@TableLogic
	@ExcelProperty(value = "可用性", index = 13)
	private Boolean available;
}
