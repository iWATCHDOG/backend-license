package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum Group {
	/**
	 * 默认
	 */
	DEFAULT(0, 0, 0);

	/**
	 * 数字越大越优先
	 */
	final int priority;
	/**
	 * 多少钱/月(单位:分CNY)
	 */
	final int price;

	/**
	 * 优惠百分比
	 */
	final float discount;

	Group(int priority, int price, float discount) {
		this.priority = priority;
		this.price = price;
		this.discount = discount;
	}

	/**
	 * 根据月计算价格(单位:分)
	 */
	public int getPriceByMonth(int month) {
		int price = this.price;
		if (discount > 0) {
			// 计算折扣
			float d = 1 - discount / 100;
			price = (int) (price * d);
		}
		/*
		  3个月95折
		  6个月9折
		  12个月85折
		 */
		if (month >= 3 && month < 6) {
			price = (int) (price * 0.95);
		} else if (month >= 6 && month < 12) {
			price = (int) (price * 0.9);
		} else if (month >= 12) {
			price = (int) (price * 0.85);
		}
		return price;
	}
}
