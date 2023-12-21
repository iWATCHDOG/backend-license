package cn.watchdog.license.model.entity;

import cn.watchdog.license.model.enums.SecurityType;
import cn.watchdog.license.util.gson.GsonProvider;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.reflect.TypeToken;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
}
