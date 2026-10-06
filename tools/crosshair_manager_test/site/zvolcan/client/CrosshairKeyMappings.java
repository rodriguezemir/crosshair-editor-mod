package site.zvolcan.client;

/** Manager-only harness boundary. Actual registration is tested by the wiring harness. */
public final class CrosshairKeyMappings {
	public static int registrations;
	public static void register() { registrations++; }
}
