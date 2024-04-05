package cn.watchdog.license.constant;

public interface ExcelConstant {
	//一个sheet装100w数据
	Integer PER_SHEET_ROW_COUNT = 1000000;
	//每次查询20w数据，每次写入20w数据
	Integer PER_WRITE_ROW_COUNT = 200000;
}
