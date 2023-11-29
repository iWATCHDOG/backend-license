package cn.watchdog.license.util;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class VersionUtil {

	public static Version getVersion() {
		InputStream file = VersionUtil.class.getClassLoader().getResourceAsStream("version.yml");
		Yaml yaml = new Yaml();
		return yaml.loadAs(file, Version.class);
	}

	@Data
	public static class Version {
		private String version;
		private String time;
	}
}
