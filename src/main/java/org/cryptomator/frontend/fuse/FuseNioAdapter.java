package org.cryptomator.frontend.fuse;

import ru.serce.jnrfuse.FuseFS;

public interface FuseNioAdapter extends FuseFS, AutoCloseable {

	boolean isMounted();

	/**
	 * Checks if the filesystem is in use (and therefore an unmount attempt should be avoided).
	 * <p>
	 * Important: This function only checks, like the name suggests, if the filesystem is busy and used. A return value of {@code false} should not be considered as an unmount can be safely and successfully executed and thus an unmount may fail.
	 * <p>
	 * API note: It is the task of the fs developer to decide what "in use" means. For example, "in use" can mean there are open resources or pending operations.
	 * Additionally no guarantees about the validity of the result after the call is made, i.e. the result may be immediately outdated.
	 *
	 * @return true if the filesystem is in use
	 */
	boolean isInUse();

	/**
	 * Sets mounted to false.
	 * <p>
	 * Allows custom unmount implementations to prevent subsequent invocations of {@link #umount()} to run into illegal states.
	 */
	void setUnmounted();
}
