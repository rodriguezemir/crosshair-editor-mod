package site.zvolcan;

import java.util.ArrayList;
import java.util.List;

/** Captures startup warnings without starting Minecraft or a logging backend. */
public final class CrosshairEditor {
	public static final String MOD_ID = "crosshaireditor";
	public static final TestLogger LOGGER = new TestLogger();
	public static final class TestLogger {
		public final List<String> warnings = new ArrayList<>();
		public void warn(String format, Object argument) { warnings.add(String.valueOf(argument)); }
	}
}
