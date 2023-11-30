package cn.watchdog.license.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

@Slf4j
public class VersionUtil {
	public static Version version = null;

	public static Version getVersion() {
		if (version != null) {
			return version;
		}
		InputStream file = VersionUtil.class.getClassLoader().getResourceAsStream("version.yml");
		Yaml yaml = new Yaml();
		version = yaml.loadAs(file, Version.class);
		return version;
	}

	@Data
	public static class Version {
		private String version;
		private String time;
	}
}
