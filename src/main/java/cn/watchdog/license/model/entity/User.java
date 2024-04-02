package cn.watchdog.license.model.entity;

import cn.watchdog.license.model.enums.UserGender;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.model.vo.UserVO;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "user")
@Data
public class User implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	@ExcelProperty(value = "UD", index = 0)
	private Long uid;

	@ExcelProperty(value = "用户名", index = 1)
	private String username;

	@ExcelProperty(value = "密码", index = 2)
	private String password;

	@ExcelProperty(value = "邮箱", index = 3)
	private String email;

	@ExcelProperty(value = "手机号", index = 4)
	private String phone;

	@ExcelProperty(value = "性别", index = 5)
	private Integer gender;

	@ExcelProperty(value = "头像地址", index = 6)
	private String avatar;

	@ExcelProperty(value = "状态", index = 7)
	private Integer status;

	@ExcelProperty(value = "创建时间", index = 8)
	private Date createTime;

	@ExcelProperty(value = "更新时间", index = 9)
	private Date updateTime;

	@TableLogic
	@ExcelProperty(value = "可用性", index = 10)
	private Boolean available;

	public UserGender getUserGender() {
		return UserGender.valueOf(gender);
	}

	public void setUserGender(UserGender userGender) {
		this.gender = userGender.getCode();
	}

	public UserStatus getUserStatus() {
		return UserStatus.valueOf(status);
	}

	public void setUserStatus(UserStatus userStatus) {
		this.status = userStatus.getCode();
	}

	public UserVO toUserVO() {
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(this, userVO);
		return userVO;
	}
}
