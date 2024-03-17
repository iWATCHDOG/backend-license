package cn.watchdog.license.model.entity;

import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.model.vo.SecurityLogVO;
import cn.watchdog.license.util.gson.GsonProvider;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.reflect.TypeToken;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@TableName(value = "security_log")
@Data
public class SecurityLog implements Serializable {
	@Serial
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
	/**
	 * id
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	private Long uid;
	private String avatar;
	private String title;
	private String types;
	private String ip;
	private String info;

	private Date createTime;
	private Date updateTime;
	@TableLogic
	private Boolean available;

	public List<SecurityType> toTypes() {
		Type listType = new TypeToken<List<Integer>>() {
		}.getType();
		List<Integer> ret = GsonProvider.normal().fromJson(types, listType);
		if (ret == null) {
			return new ArrayList<>();
		}
		return ret.stream().map(SecurityType::valueOf).toList();
	}

	public void setTypesByList(List<SecurityType> securityTypes) {
		List<Integer> list = securityTypes.stream().map(SecurityType::getCode).toList();
		this.types = GsonProvider.normal().toJson(list);
	}

	public void setTitles(Object... titles) {
		List<String> list = Arrays.stream(titles).map(Object::toString).toList();
		this.title = String.join(" ", list);
	}

	public SecurityLogVO toVO() {
		SecurityLogVO vo = new SecurityLogVO();
		BeanUtils.copyProperties(this, vo);
		if (this.avatar != null) {
			vo.setAvatar(GsonProvider.normal().fromJson(this.avatar, Avatar.class));
		}
		return vo;
	}

	public void initAvatar(Avatar avatar) {
		if (avatar == null) {
			return;
		}
		// 保存
		this.avatar = GsonProvider.normal().toJson(avatar);
	}

	public record Avatar(List<AvatarData> avatars) {
	}

	/*
	  头像数字规则
	  <p>
	      1. 使用关联用户头像，uid
	      2. 使用关联图片库图片，md5
	      3. 使用url地址，此刻url地址是使用外部图片地址。需完成url变量。
	*/
	public record AvatarData(Integer code, Object data) {
	}
}
