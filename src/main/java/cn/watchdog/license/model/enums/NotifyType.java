package cn.watchdog.license.model.enums;

import lombok.Getter;

@Getter
public enum NotifyType {
	SUCCESS(0),
	ERROR(1),
	WARNING(2),
	INFO(3);
	final int value;

	NotifyType(int value) {
		this.value = value;
	}
}
