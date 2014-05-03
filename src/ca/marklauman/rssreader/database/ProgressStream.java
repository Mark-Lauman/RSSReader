package ca.marklauman.rssreader.database;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** A {@link BufferedInputStream} that keeps track
 *  of how much of itself has been read.
 * @author Mark Lauman                                  */
public class ProgressStream extends BufferedInputStream {
	/** What has already been read. */
	private long loaded;
	/** The estimated length of the stream */
	private long length;
	
	/** Constructs a new {@code ProgressStream} with
	 *  an estimated length of {@code length}.
	 *  @param in the InputStream this reads from. 
	 *  @param length The estimated size of the stream
	 *  in bytes. Invalid lengths will result invalid
	 *  results returned by {@link #getProgress()}.  */
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
	public int getProgress() {
		if(length < 0) return 0;
		if(length == 0) return 100;
		if(length < loaded) return 99;
		return (int) (100.0 * loaded / length + 0.5);
	}
}