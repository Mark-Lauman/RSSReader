package ca.marklauman.rssreader.providers.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** An BufferedInputStream that keeps track of how much of
 *  itself has been read.
 * @author Mark Lauman                                  */
public class ProgressStream extends BufferedInputStream {
	private long loaded;
	private long length;
	
	public ProgressStream(InputStream in, long length) {
		super(in);
		this.loaded = 0;
		this.length = length;
	}
	
	@Override
	public synchronized int read() throws IOException {
		loaded++;
		return super.read();
	}
	
	@Override
	public synchronized int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
		int bytes_read = super.read(buffer, byteOffset, byteCount);
		loaded += bytes_read;
		return bytes_read;
	}
	
	@Override
	public synchronized long skip(long byteCount) throws IOException {
		loaded += byteCount;
		return super.skip(byteCount);
	}
	
	/** Get the percentage of the stream that has been loaded.
	 *  @return How much has been loaded as a percent (range
	 *  of values is 0 - 100)                               */
	public int getPercent() {
		if(length < 0) return 0;
		if(length == 0) return 100;
		if(length < loaded) return 99;
		return (int) (100.0 * loaded / length + 0.5);
	}
}
