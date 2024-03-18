package cn.watchdog.license.util;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
/*
  提前从 xdb 文件中加载出来 VectorIndex 数据，然后全局缓存
  每次创建 Searcher 对象的时候使用全局的 VectorIndex 缓存可以减少一次固定的 IO 操作
  从而加速查询，减少 IO 压力。
 */
public class IpRegionUtil {
	private static final IpRegionUtil ipRegionUtil = new IpRegionUtil();
	private static final String dbPath = "./data/ip2region.xdb";
	// 1、从 dbPath 中预先加载 VectorIndex 缓存，并且把这个得到的数据作为全局变量，后续反复使用。
	private byte[] vIndex;

	public IpRegionUtil() {
		try {
			vIndex = Searcher.loadVectorIndexFromFile(dbPath);
		} catch (Exception e) {
			System.out.printf("failed to load vector index from `%s`: %s\n", dbPath, e);
			return;
		}
	}

	public static IpRegionUtil getInstance() {
		return ipRegionUtil;
	}

	public Region search(String ip) {
		// 2、使用全局的 vIndex 创建带 VectorIndex 缓存的查询对象。
		Searcher searcher;
		try {
			searcher = Searcher.newWithVectorIndex(dbPath, vIndex);
		} catch (Exception e) {
			log.error("无法创建向量索引缓存搜索器{}:", dbPath, e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "无法创建向量索引缓存搜索器" + dbPath, null);
		}

		Region r = new Region();
		// 3、查询
		try {
			long sTime = System.nanoTime();
			String region = searcher.search(ip);
			long cost = TimeUnit.NANOSECONDS.toMicros((long) (System.nanoTime() - sTime));
			// 国家|区域|省份|城市|ISP
			String[] regionInfo = region.split("\\|");
			r.setCountry(Objects.equals(regionInfo[0], "0") ? null : regionInfo[0]);
			r.setRegion(Objects.equals(regionInfo[1], "0") ? null : regionInfo[1]);
			r.setProvince(Objects.equals(regionInfo[2], "0") ? null : regionInfo[2]);
			r.setCity(Objects.equals(regionInfo[3], "0") ? null : regionInfo[3]);
			r.setIsp(Objects.equals(regionInfo[4], "0") ? null : regionInfo[4]);
			r.setIoCount(searcher.getIOCount());
			r.setCost(cost);
			log.info("region: {}, ioCount: {}, took: {} μs", region, searcher.getIOCount(), cost);
		} catch (Exception e) {
			log.error("搜索失败({}):", ip, e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "搜索失败:" + ip, null);
		}
		// 4、关闭资源
		try {
			searcher.close();
		} catch (Exception e) {
			log.error("关闭搜索器失败:", e);
			throw new BusinessException(ReturnCode.SYSTEM_ERROR, "关闭搜索器失败", null);
		}
		return r;
	}

	@Data
	public static class Region {
		/**
		 * 国家
		 */
		private String country;
		/**
		 * 区域
		 */
		private String region;
		/**
		 * 省份
		 */
		private String province;
		/**
		 * 城市
		 */
		private String city;
		/**
		 * 服务商
		 */
		private String isp;
		private int ioCount;
		private long cost;
	}
}
