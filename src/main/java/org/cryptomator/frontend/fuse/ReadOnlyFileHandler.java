package org.cryptomator.frontend.fuse;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.constants.platform.OpenFlags;
import jnr.ffi.Pointer;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

@PerAdapter
public class ReadOnlyFileHandler implements Closeable {

	private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyFileHandler.class);

	protected final OpenFileFactory openFiles;
	private final FileAttributesUtil attrUtil;

	@Inject
	public ReadOnlyFileHandler(OpenFileFactory openFiles, FileAttributesUtil attrUtil) {
		this.openFiles = openFiles;
		this.attrUtil = attrUtil;
	}

	public int open(Path path, FuseFileInfo fi) {
		try {
			return open(path, OpenFlags.valueOf(fi.flags.longValue()));
		} catch (IOException e) {
			LOG.error("Error opening file.", e);
			return -ErrorCodes.EIO();
		}
	}

	protected int open(Path path, OpenFlags openFlags) throws IOException {
		switch (openFlags) {
		case O_RDONLY:
			openFiles.open(path, StandardOpenOption.READ);
			return 0;
		default:
			throw new IOException("Unsupported open flags: " + openFlags.name());
		}
	}

	public int read(Path path, Pointer buf, long size, long offset, FuseFileInfo fi) {
		OpenFile file = openFiles.get(path);
		if (file == null) {
			LOG.warn("File not opened: {}", path);
			return -ErrorCodes.EBADFD();
		}
		try {
			return file.read(buf, size, offset);
		} catch (IOException e) {
			LOG.error("Reading file failed.", e);
			return -ErrorCodes.EIO();
		}
	}

	public int release(Path path, FuseFileInfo fi) {
		try {
			openFiles.close(path);
			return 0;
		} catch (IOException e) {
			LOG.error("Error closing file.", e);
			return -ErrorCodes.EIO();
		}
	}

	public int getattr(Path node, FileStat stat) {
		try {
			stat.st_mode.set(FileStat.S_IFREG | 0444);
			BasicFileAttributes attr = Files.readAttributes(node, BasicFileAttributes.class);
			attrUtil.copyBasicFileAttributesFromNioToFuse(attr, stat);
			return 0;
		} catch (UnsupportedOperationException | IllegalArgumentException e) {
			LOG.error("getattr failed.", e);
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			LOG.error("getattr failed.", e);
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public void close() throws IOException {
		openFiles.close();
	}

}
