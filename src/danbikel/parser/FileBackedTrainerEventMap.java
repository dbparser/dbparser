package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;


/**
 * Presents an immutable map of a type of {@link TrainerEvent} objects to
 * observed counts, backed by a file of the form output by
 * {@link Trainer#writeStats}.  The contract of the {@link java.util.Map}
 * interface may be violated if the underlying file does not contain
 * a collection of unique <code>TrainerEvent</code> objects; however, this
 * contract violation may not be a problem for many kinds of operations,
 * such as those that rely simply on the ability to iterate over all
 * observed events.  One such operation is the additive derivation of
 * counts as implemented by the method
 * {@link Model#deriveCounts(MapToPrimitive,Filter,double,FlexibleMap)}.
 */
public class FileBackedTrainerEventMap extends AbstractMapToPrimitive {
  private File file;
  private Symbol type;

  public FileBackedTrainerEventMap(Symbol type, String filename)
    throws FileNotFoundException {
    this(type, new File(filename));
  }

  public FileBackedTrainerEventMap(Symbol type, File file)
    throws FileNotFoundException {
    if (!file.exists()) {
      String msg = FileBackedTrainerEventMap.class.getName() +
                   ": couldn't find file \"" + file + "\"";
      throw new FileNotFoundException(msg);
    }
    this.file = file;
    this.type = type;
  }


  public Set entrySet() {
    return new java.util.AbstractSet() {
      public int size() {
        Iterator it = entrySet().iterator();
        int size = 0;
        for ( ; it.hasNext(); it.next(), size++)
          ;
        return size;
      }
      public Iterator iterator() {
        SexpTokenizer tok = null;
        try {
          tok = new SexpTokenizer(file, Language.encoding(),
                                  Constants.defaultFileBufsize);
        }
        catch (IOException ioe) {
          throw new RuntimeException(ioe.toString());
        }
        return Trainer.getEventIterator(tok, type);
      }
    };
  }
  public void removeRandom(int bucketIndex) {
    String msg =
      "Method removeRandom not implemented, as this is an unmodifiable map";
    throw new UnsupportedOperationException(msg);
  }
  public MapToPrimitive.Entry getEntry(Object key) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getKey().equals(key))
        return entry;
    }
    return null;
  }
  public MapToPrimitive.Entry getEntryMRU(Object key) {
    throw new java.lang.UnsupportedOperationException("Method getEntryMRU() not yet implemented.");
  }
  public MapToPrimitive.Entry getEntry(Object key, int hashCode) {
    return getEntry(key);
  }
  public MapToPrimitive.Entry getEntryMRU(Object key, int hashCode) {
    /**@todo Implement this danbikel.util.MapToPrimitive abstract method*/
    throw new java.lang.UnsupportedOperationException("Method getEntryMRU() not yet implemented.");
  }
  public static void main(String[] args) {
  }

}