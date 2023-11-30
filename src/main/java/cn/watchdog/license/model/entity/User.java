package cn.watchdog.license.model.entity;

import cn.watchdog.license.model.enums.UserGender;
import cn.watchdog.license.model.enums.UserStatus;
import cn.watchdog.license.model.vo.UserVO;
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
	@TableLogic
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
