package cn.watchdog.license.events;

public interface Cancellable {
	boolean isCancelled();

	void setCancelled(boolean cancelled);
}
