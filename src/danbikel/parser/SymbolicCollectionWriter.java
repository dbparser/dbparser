package danbikel.parser;

import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides static methods to write out the contents of a <code>Map</code>
 * or a <code>Set</code> in an S-expression format.
 */
public class SymbolicCollectionWriter implements Serializable {
  private SymbolicCollectionWriter() {}

  public static void writeSet(Set set, Symbol name, Writer writer)
    throws IOException {
    writer.write("(");
    if (name != null) {
      writer.write(name.toString());
      writer.write(" ");
    }
    int initWhitespaceSize = (name == null ? 1 : name.toString().length() + 2);
    char[] initWhitespaceArr = new char[initWhitespaceSize];
    Arrays.fill(initWhitespaceArr, ' ');
    String initWhitespaceStr = new String(initWhitespaceArr);
    Iterator it = set.iterator();
    for (boolean first = true; it.hasNext(); first = false) {
      if (!first)
	writer.write(initWhitespaceStr);
      writer.write(valueOf(it.next()));
      if (it.hasNext())
	writer.write("\n");
    }
    writer.write(")\n");
  }

  public static void writeSet(Set set, Writer writer) throws IOException {
    writeSet(set, null, writer);
  }

  /**
   * Writes out the contents of <code>map</code> in an S-expression format.
   * Each <code>key-value</code> pair is written on its own line as
   * <pre> (name key value) </pre>
   * where <code>key</code> is the result of calling
   * <code>valueOf(key)</code> and <code>value</code> is the result of
   * calling <code>valueOf(value)</code>.
   * If <code>name</code> is <code>null</code>, then the format will be
   * <pre> (key value) </pre>
   * If the <code>value</code> in a <code>key-value</code> pair is an
   * instance of <code>Set</code> then each of that set's members is
   * output in a space-separated list within parentheses; otherwise,
   * the normal string representation of <code>value</code> is
   * written.
   *
   * @param map the map to write out
   * @param name the name to prepend to each key-value pair, or
   * <code>null</code> if the key-value pairs are to be unnamed
   * @param writer the output stream to write to
   *
   * @see #valueOf(Object)
   */
  public static void writeMap(Map map, Symbol name, Writer writer)
    throws IOException {
    Iterator it = map.keySet().iterator();
    while (it.hasNext()) {
      writer.write("(");
      if (name != null) {
	writer.write(name.toString());
	writer.write(" ");
      }
      Object key = it.next();
      Object value = map.get(key);
      writer.write(valueOf(key));
      writer.write(" ");

      if (value instanceof Set) {
	writer.write("(");
	Iterator setIterator = ((Set)value).iterator();
	while (setIterator.hasNext()) {
	  writer.write(valueOf(setIterator.next()));
	  if (setIterator.hasNext())
	    writer.write(" ");
	}
	writer.write(")");
      }
      else
	writer.write(valueOf(value));

      writer.write(")\n");
    }
  }

  /**
   * An alias for <code>writeMap(map, null, writer)</code>.
   *
   * @param map the map to write out
   * @param writer the output stream to write to
   */
  public static void writeMap(Map map, Writer writer) throws IOException {
    writeMap(map, null, writer);
  }

  /**
   * If the specified object is not an instance of a <code>Sexp</code> object
   * but <i>is</i> an instance of a <code>SexpConvertible</code> object,
   * then the value returned is the string representation of
   * <code>((SexpConvertible)obj).toSexp()</code>; otherwise, the value
   * returned is that returned by <code>String.valueOf(obj)</code>.
   */
  public final static String valueOf(Object obj) {
    if (!(obj instanceof Sexp) && obj instanceof SexpConvertible)
      return String.valueOf(((SexpConvertible)obj).toSexp());
    else
      return String.valueOf(obj);
  }
}
