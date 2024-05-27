package cn.watchdog.license.util;

import cn.watchdog.license.constant.ExcelConstant;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ExcelUtil<T> {
	public static <T> void write(IService<T> service, Class<?> clas, ExcelWriter excelWriter, String sheetName) {
		//记录总数:实际中需要根据查询条件进行统计即可:一共多少条
		long totalCount = service.lambdaQuery().count();
		//每一个Sheet存放100w条数据
		int sheetDataRows = ExcelConstant.PER_SHEET_ROW_COUNT;
		//每次写入的数据量20w,每页查询20W
		int writeDataRows = ExcelConstant.PER_WRITE_ROW_COUNT;
		//计算需要的Sheet数量
		int sheetNum = (int) (totalCount % sheetDataRows == 0 ? (totalCount / sheetDataRows) : (totalCount / sheetDataRows + 1));
		//计算一般情况下每一个Sheet需要写入的次数(一般情况不包含最后一个sheet,因为最后一个sheet不确定会写入多少条数据)
		long oneSheetWriteCount = sheetDataRows / writeDataRows;
		//计算最后一个sheet需要写入的次数
		long lastSheetWriteCount = totalCount % sheetDataRows == 0 ? oneSheetWriteCount : (totalCount % sheetDataRows % writeDataRows == 0 ? (totalCount / sheetDataRows / writeDataRows) : (totalCount / sheetDataRows / writeDataRows + 1));
		for (int i = 0; i < sheetNum; i++) {
			//创建Sheet
			WriteSheet sheet = new WriteSheet();
			sheet.setSheetName(sheetName + i);
			sheet.setSheetNo(i);
			//循环写入次数: j的自增条件是当不是最后一个Sheet的时候写入次数为正常的每个Sheet写入的次数,如果是最后一个就需要使用计算的次数lastSheetWriteCount
			for (int j = 0; j < (i != sheetNum - 1 ? oneSheetWriteCount : lastSheetWriteCount); j++) {
				//分页查询一次20w
				Page<T> page = service.page(new Page<>(j + 1 + oneSheetWriteCount * i, writeDataRows), null);
				List<T> empList = page.getRecords();
				WriteSheet writeSheet = EasyExcel.writerSheet(i, sheetName + (i + 1)).head(clas)
						.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).build();
				//写数据
				excelWriter.write(empList, writeSheet);
			}
		}
	}
}
