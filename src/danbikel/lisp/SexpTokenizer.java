package danbikel.lisp;

import java.io.*;
import java.net.URL;

/**
 * A class for tokenizing simple S-expressions, where there are only strings
 * delimited by whitespace or parentheses (as implemented by {@link
 * WordTokenizer}).  Comments are lines where the first non-whitespace
 * character is a semicolon (the character ';').
 */
public class SexpTokenizer extends WordTokenizer {

  /**
   * Constructs a <code>SexpTokenizer</code> with the specified stream
   * and comment-recognition option.
   *
   * @param inStream the input stream for this tokenizer
   * @param comments indicates whether to recognizes comment lines
   */
  public SexpTokenizer(Reader inStream, boolean comments) {
    super(inStream);
    ordinaryChar('(');
    ordinaryChar(')');
    if (comments)
      commentChar(';');
  }

  /**
   * Constructs a <code>SexpTokenizer</code> with the specified stream
   * and the default comment-recognition option, which is <code>true</code>.
   *
   * @param inStream the input stream for this tokenizer
   */
  public SexpTokenizer(Reader inStream) {
    this(inStream, true);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param filename the name of the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   */
  public SexpTokenizer(String filename, String encoding, int bufSize)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(new FileInputStream(filename),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   *
   * @param filename the name of the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   */
  public SexpTokenizer(String filename, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(new FileInputStream(filename),
						  encoding),
			    bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param file the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   */
  public SexpTokenizer(File file, String encoding, int bufSize)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(new FileInputStream(file),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   *
   * @param file the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   */
  public SexpTokenizer(File file, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(new FileInputStream(file),
						  encoding),
			    bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   * The tokenizer will recognize comment lines. 
   *
   * @param stream the stream of bytes, encoded with <code>encoding</code>,
   * containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   */
  public SexpTokenizer(InputStream stream, String encoding, int bufSize)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(stream, encoding), bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   *
   * @param stream the stream of bytes, encoded with <code>encoding</code>,
   * containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   */
  public SexpTokenizer(InputStream stream, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException, FileNotFoundException {
    this(new BufferedReader(new InputStreamReader(stream, encoding), bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>InputStreamReader</code>
   * around the stream created by calling <code>url.openStream()</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param url the url from which to get the stream containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   */
  public SexpTokenizer(URL url, String encoding, int bufSize)
    throws UnsupportedEncodingException, IOException {
    this(new BufferedReader(new InputStreamReader(url.openStream(),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>InputStreamReader</code>
   * around the stream created by calling <code>url.openStream()</code>.
   * @param url the url from which to get the stream containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * @param comments whether this tokenizer will recognize comments
   */
  public SexpTokenizer(URL url, String encoding, int bufSize, boolean comments)
    throws UnsupportedEncodingException, IOException {
    this(new BufferedReader(new InputStreamReader(url.openStream(),
						  encoding),
			    bufSize),
	 comments);
  }
}
