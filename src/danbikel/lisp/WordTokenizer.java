package danbikel.lisp;

import java.io.*;
import java.util.BitSet;

/**
 * A simple tokenizer for words only (no numbers and no significant eol chars).
 * Many of the methods and some of the data members of {@link
 * java.io.StreamTokenizer} exist in this class, but at present, this class
 * does not extend <code>StreamTokenizer</code>.  This class recognizes
 * comments as beginning with an optionally-specified comment character.
 * A comment is a line where the first non-whitespace character is the
 * comment character.
 */
public class WordTokenizer {
  private static final String className = WordTokenizer.class.getName();

  /**
   * Included as a public data member so that javadoc can resolve
   * external links to members of the <code>StreamTokenizer</code> class.
   */
  public StreamTokenizer javadocHack;

  // constants
  private final static char[] lineSep =
    System.getProperty("line.separator").toCharArray();
  private final static int lineSepLength = lineSep.length;
  private final static int initialBufChunkSize = 1024;
  private final static int maxBufChunkSize = 1048576;

  // data members

  /**
   * The type of the last token read, using the type definitions in {@link
   * java.io.StreamTokenizer}.  Because this tokenizer only reads ordinary
   * characters and words, the value of <code>ttype</code> will only ever be
   * {@link java.io.StreamTokenizer#TT_EOF}, {@link
   * java.io.StreamTokenizer#TT_WORD} or an ordinary character.
   *
   * @see #ordinaryChar
   * @see #ordinaryChars
   */
  public int ttype;

  /**
   * Contains the most recent word tokenized by this tokenizer.
   */
  public String sval;

  private boolean pushedBack = false;
  private boolean firstCharRead = false;
  private char commentChar = 0;
  private BitSet ordinary;
  private Reader inStream;
  private int lastChar;
  private int lineno = 1;
  private int linenoOfLastToken = 0;
  private int lineSepMatchIdx = 0;
  private int currBufChunkSize = initialBufChunkSize;
  // by using increasingly larger StringBuffer objects to do buffering,
  // we hackishly avoid char array copying, since neither the
  // StringBuffer.toString nor String.substring methods do any copying, but
  // merely shares the StringBuffer's internal char array with the
  // newly-created String object; furthermore, as long as the capacity
  // of the StringBuffer is not exceeded, future append operations will
  // not cause any copying to occur this is really a low-level hack, since
  // it is dependent on Sun's implementation of String and StringBuffer;
  // however, it is the only way to avoid an array copy for every single
  // token, since we to create a String object for each token
  private StringBuffer buf = new StringBuffer(currBufChunkSize);

  /**
   * Creates a new tokenizer object.
   *
   * @param inStream the stream to be tokenized.
   */
  public WordTokenizer(Reader inStream) {
    this.inStream = inStream;
    ordinary = new BitSet(Byte.MAX_VALUE);
  }

  /**
   * Specifies a character to be treated as the start of a comment
   * on the current line.  The comment character must have an integer value
   * greater than 0.
   * @param ch the character to be treated as the start of a single-line comment
   */
  public void commentChar(int ch) {
    if (ch <= 0 || ch > Character.MAX_VALUE)
      throw new IllegalArgumentException(className +
					 ": commentChar out of range: " + ch);
    commentChar = (char)ch;
  }

  /**
   * Specifies a character to treated as a token delimiter,
   * to be contained in {@link #ttype} after it is read.  The character
   * must be in the range of <code>0 <= ch <= Byte.MAX_VALUE</code>.
   *
   * @param ch the character to be tested
   */
  public void ordinaryChar(char ch) {
    if (ch < 0 || ch > Byte.MAX_VALUE)
      throw new IndexOutOfBoundsException(className +
					  ": ordinaryChar called with " + ch);
    ordinary.set((int)ch);
  }

  /**
   * Specifies a range of characters to treated as a token delimiter, to be
   * contained in {@link #ttype} after it is read.  The characters
   * must be in the range of <code>0 <= ch <= Byte.MAX_VALUE</code>.
   *
   * @param low the lowest-valued character in a range to be treated as
   * token delimiters
   * @param hi the highest-valued character in a range to be treated as
   * token delimiters
   */
  public void ordinaryChars(int low, int hi) {
    if (low > hi || low > Byte.MAX_VALUE)
      throw new IndexOutOfBoundsException(className +
					  ": ordinaryChars called with " +
					  low + " and " + hi);
    int bitSetSize = ordinary.size();
    for (int i = low; i <= hi && i < bitSetSize; i++)
      ordinary.set(i);
  }


  /**
   * Reads the next token from the underlying character stream and returns
   * its type, which is also stored in {@link #ttype}.
   *
   * @return the type of the token that was just read
   * (also stored in {@link #ttype})
   * @throws IOException if there was a problem reading the next token
   * from the underlying stream
   */
  public int nextToken() throws IOException {
    if (pushedBack) {
      pushedBack = false;
      return ttype;
    }
    if (firstCharRead == false) {
      lastChar = readChar();  // read the first character
      firstCharRead = true;
    }
    while (Character.isWhitespace((char)lastChar))
      lastChar = readChar();
    if (commentChar > 0 &&
	lastChar == commentChar &&
	lineno > linenoOfLastToken) {
      while (lastChar == commentChar) {
	int currLineNo = lineno;
	while (lineno == currLineNo)
	  lastChar = readChar();
	while (Character.isWhitespace((char)lastChar))
	  lastChar = readChar();
      }
    }
    if (lastChar == -1) {
      ttype = StreamTokenizer.TT_EOF;
    }
    else if (ordinary.get(lastChar)) {
      ttype = lastChar;
      linenoOfLastToken = lineno;
      lastChar = readChar();
    }
    else {
      ttype = StreamTokenizer.TT_WORD;
      linenoOfLastToken = lineno;
      int tokenStartIdx = buf.length();
      // we synchronize on buf, so we don't have to re-acquire lock for every
      // invocation of append
      synchronized (buf) {
	while (lastChar != -1 &&
	       !Character.isWhitespace((char)lastChar) &&
	       !ordinary.get(lastChar)) {
	  tokenStartIdx = ensureBufferCapacity(tokenStartIdx);
	  buf.append((char)lastChar);
	  lastChar = readChar();
	}
	sval = buf.substring(tokenStartIdx, buf.length());
      }
    }
    return ttype;
  }

  private final int ensureBufferCapacity(int tokenStartIdx) {
    if (buf.capacity() == buf.length()) {
      StringBuffer old = buf;
      int newSize = currBufChunkSize * 2;
      if (newSize <= maxBufChunkSize)
	currBufChunkSize = newSize;
      buf = new StringBuffer(currBufChunkSize);
      buf.append(old.substring(tokenStartIdx, old.length()));
      return 0;
    }
    else
      return tokenStartIdx;
  }

  /**
   * Causes the most recent token read (either a word or ordinary character)
   * to be pushed back, so that it is the next token returned by
   * {@link #nextToken}.
   */
  public void pushBack() {
    pushedBack = true;
  }

  /**
   * Returns the line number of the underlying character stream.
   *
   * @return the line number of the underlying character stream.
   */
  public int lineno() { return lineno; }

  /**
   * Closes the underlying stream.
   *
   * @throws IOException if the underlying stream throws an
   * <code>IOException</code> while being closed
   */
  public void close() throws IOException {
    inStream.close();
  }

  // helper methods
  private final int readChar() throws IOException {
    int ch = inStream.read();

    // as we read chars, we continuously try to match the line separator
    // string; as soon as a character differs, we reset our match index,
    // lineSepMatchIdx, back to zero.  if we reach the length of the line
    // separator string, then we've just read a newline, and so we increase
    // lineno and reset lineSepMatchIdx;
    // alternatively, a newline character also delimits lines (the "default"
    // line separator, as it were)
    if (ch == '\n') {
      lineno++;
      lineSepMatchIdx = 0;
    }
    else if (ch == lineSep[lineSepMatchIdx]) {
      lineSepMatchIdx++;
      if (lineSepMatchIdx == lineSepLength) {
	lineno++;
	lineSepMatchIdx = 0;
      }
    }
    else
      lineSepMatchIdx = 0;

    return ch;
  }
}
