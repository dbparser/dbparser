package danbikel.switchboard;

import java.io.*;

class DefaultObjectWriterFactory implements ObjectWriterFactory {

  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException {

    boolean noHeader = append && !emptyFile;
    return (noHeader ?
	    (ObjectWriter)new DefaultNoHeaderObjectWriter(os) :
	    (ObjectWriter)new DefaultObjectWriter(os));
  }

  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException {
    boolean noHeader = append && !emptyFile;
    return (noHeader ?
	    (ObjectWriter)new DefaultNoHeaderObjectWriter(os, bufSize) :
	    (ObjectWriter)new DefaultObjectWriter(os, bufSize));
  }

  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    boolean emptyFile = true;
    File file = new File(filename);
    if (file.length() > 0)
      emptyFile = false;

    FileOutputStream os = new FileOutputStream(filename, append);

    boolean noHeader = append && !emptyFile;

    return (noHeader ?
	    (ObjectWriter)new DefaultNoHeaderObjectWriter(os, bufSize) :
	    (ObjectWriter)new DefaultObjectWriter(os, bufSize));
  }
}
