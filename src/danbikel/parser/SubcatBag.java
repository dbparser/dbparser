package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * Provides a bag implementation of subcat requirements (a <i>bag</i>
 * is a set that allows multiple occurrences of the same item).  This list
 * of all argument nonterminals is provided by {@link Training#argNonterminals}
 * map.  As a special case, this class also supports gap requirements,
 * that is, requirements equal to {@link Training#gapAugmentation}.
 * This class also provides a separate bin for miscellaneous subcat
 * requirements, such as those that can be matched via {@link
 * danbikel.parser.lang.AbstractTraining#headSym}.  All nonterminal
 * requirements are stripped of any augmentations before being counted in
 * this subcat bag.
 * <p>
 * The comment for the <code>toSexp</code> method describes the way in which
 * this class represents miscellaneous requirements.
 * <b>Bugs</b>:
 * <ol>
 * <li>This class provides special-case bins for counting gap
 * and miscellaneous subcat requirements.  If this parsing package is
 * expanded to include additional elements that are possible
 * generative requirements, and these elements do not appear in {@link
 * Training#argNonterminals}, unless it is modified, this class will
 * simply put these elements in the miscellaneous bin.
 * <li>This class assumes that only single requirements will be passed to its
 * {@link #add(Symbol)} or {@link #remove(Symbol)} methods.  For example, the
 * generation of the modifying nonterminal <tt>NP-A-g</tt> satisfies two types
 * of requirements, being an NP argument and having the gap feature.
 * Nevertheless, this class assumes that these two types of requirements will
 * be added or removed in two separate invocations of either {@link
 * #add(Symbol)} or {@link #remove(Symbol)}, one invocation with <tt>NP-A</tt>
 * and one with <tt>g</tt>.  Currently, the {@link Decoder} class assumes that
 * each nonterminal generated will satisfy only a single requirement (but then,
 * it does not handle the gap feature at all in its current state).
 * <li>As explained in the documentation for {@link #toSexp()}, for
 * input/output purposes, this class treats miscellaneous requirements
 * as the symbol <tt>+STOP+-A</tt>.  This &ldquo;fake&rdquo; argument
 * nonterminal will <i><b>not</b></i> be correctly identified by
 * the {@link Training#isArgumentFast(Symbol)} method after the
 * {@link Training#setUpFastArgMap(CountsTable)} method has been invoked
 * (unless this fake nonterminal happens to be one of the keys of the map
 * passed to {@link Training#setUpFastArgMap(CountsTable)}).  It is therefore
 * important <i><b>not</b></i> to invoke the
 * {@link Training#setUpFastArgMap(CountsTable)} method during training,
 * when requirements are added individually by {@link #add(Symbol)}, which
 * calls {@link #validRequirement(Symbol)} which in turn invokes
 * {@link Training#isArgumentFast(Symbol)}.
 * <li>This class cannot collect more than 127 total occurrences of
 * requirements.  This is well beyond the number of arguments ever postulated
 * in any human language, but <i>not</i> necessarily beyond the number
 * of generative requirements that might be needed by a future parsing
 * model.  A corollary of this limitation is that the number of occurrences
 * of a particular requirement may not exceed 127.
 * </ol>
 *
 * @see Subcats
 * @see #toSexp()
 */
public class SubcatBag implements Subcat, Externalizable {
  // constants
  // index constants: make sure to update remove method if these change
  // (if it's necessary: see comment inside remove method code)
  private final static int sizeIdx = 0;
  private final static int miscIdx = 1;
  private final static int firstRealUid = 2; // must be greater than miscIdx!!!
  private final static int gapIdx = firstRealUid;
  private static int numUids;

  // static data members
  private static Map symbolsToInts = new danbikel.util.HashMap();
  private static Symbol stopSym = Language.training.stopSym();
  private static Symbol[] symbols;
  static {
    Treebank treebank = Language.treebank();
    Symbol gapAugmentation = Language.training.gapAugmentation();
    Symbol argAugmentation = Language.training.defaultArgAugmentation();
    char delimChar = Language.treebank.canonicalAugDelimiter();

    int uid = gapIdx; // depends on gapIdx being equal to firstRealUid
    // kind of a hack: put an entry for gaps (which are "requirements"
    // that can be thrown into subcats);
    // note that uid of gap is firstRealUid (see comment inside remove method)
    symbolsToInts.put(gapAugmentation, new Integer(uid++));

    Iterator args = Language.training.argNonterminals().iterator();
    while (args.hasNext()) {
      Symbol argLabel = (Symbol)args.next();
      symbolsToInts.put(argLabel, new Integer(uid++));
    }
    numUids = uid;

    symbols = new Symbol[numUids];
    Iterator entries = symbolsToInts.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      Symbol symbol = (Symbol)entry.getKey();
      uid = ((Integer)entry.getValue()).intValue();
      /*
      // for arg nonterminals, we re-add arg augmentation for output purposes
      if (uid > gapIdx)
	symbol = Symbol.get(symbol.toString() + delimChar + argAugmentation);
       */
      symbols[uid] = symbol;
    }
    symbols[miscIdx] = Symbol.get(stopSym.toString() +
				  delimChar + argAugmentation);
  }

  private static HashMapInt fastUidMap = new HashMapInt();
  private static boolean canUseFastUidMap = false;

  public static synchronized void setUpFastUidMap(CountsTable nonterminals) {
    if (canUseFastUidMap)
      return;
    fastUidMap.put(Language.training.gapAugmentation(), gapIdx);
    Iterator nts = nonterminals.keySet().iterator();
    while (nts.hasNext()) {
      Symbol nt = (Symbol)nts.next();
      int uid = getUid(nt);
      fastUidMap.put(nt, uid);
    }
    canUseFastUidMap = true;
  }

  // data member
  private byte[] counts;

  /**
   * A method to check if the specified requirement is valid. For this
   * class, a requirement is valid if it is either
   * {@link Training#gapAugmentation} or a symbol for which
   * {@link Training#isArgumentFast(Symbol)} returns <code>true</code>.
   * A subclass may override this method to allow for new or different
   * valid requirements.
   */
  protected boolean validRequirement(Symbol requirement) {
    return
      requirement == Language.training.gapAugmentation() ||
      Language.training.isArgumentFast(requirement);
  }

  /** Constructs an empty subcat. */
  public SubcatBag() {
    counts = new byte[numUids];
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }

  /**
   * Constructs a subcat bag containing the number of occurrences of
   * the symbols of <code>list</code>.
   *
   * @param list a list of <code>Symbol</code> objects to be added to this
   * subcat bag
   */
  public SubcatBag(SexpList list) {
    this();
    addAll(list);
  }

  /**
   * Adds the specified requirement to this subcat bag.  There are
   * separate bins maintained for each of the nonterminals in the
   * list returned by {@link Training#argNonterminals}, as
   * well as a bin for gap augmentations (that is, requirements that are
   * equal to {@link Training#gapAugmentation}) and a miscellaneous bin
   * for all other requirements, such as those that can be matched via
   * {@link danbikel.parser.lang.AbstractTraining#headSym}.
   *
   * @param requirement the requirement to add to this subcat bag
   */
  public Subcat add(Symbol requirement) {
    if (validRequirement(requirement)) {
      counts[sizeIdx]++;
      counts[getUid(requirement)]++;
    }
    return this;
  }

  /**
   * Adds each of the symbols of <code>list</code> to this subcat bag,
   * effectively calling {@link #add(Symbol)} for each element of
   * <code>list</code>.
   *
   * @param list a list of <code>Symbol</code> objects to be added to this
   * subcat bag
   * @exception ClassCastException if one or more elements of <code>list</code>
   * is not an instance of <code>Symbol</code>
   */
  public boolean addAll(SexpList list) {
    int listLen = list.length();
    for (int i = 0; i < listLen; i++)
      add(list.symbolAt(i));
    return listLen > 0;
  }

  /**
   * Removes the specified requirement from this subcat bag, if possible.
   * If the specified requirement is a nonterminal, then it is only
   * removed if it is an argument nonterminal, that is, if
   * <code>Language.training().isArgumentFast(requirement)</code>
   * returns <code>true</code>, and if this subcat contained at least
   * one instance of that nonterminal.
   *
   * @return <code>true</code> if this subcat bag contained at least one
   * instance of the specified requirement and it was removed,
   * <code>false</code> otherwise
   *
   * @see Training#isArgumentFast(Symbol)
   */
  public boolean remove(Symbol requirement) {
    int uid = getUid(requirement);

    // if the uid is of an actual nonterminal (either greater than
    // firstRealUid, which is used for gap requirements, or equal to
    // miscIdx) and if the specified requirement is not marked as an
    // argument, return false
    if ((uid == miscIdx || uid > firstRealUid) &&
	!Language.training.isArgumentFast(requirement))
      return false;

    if (counts[uid] == 0)
      return false;
    else {
      counts[sizeIdx]--;
      counts[uid]--;
      return true;
    }
  }

  private static final int getUid(Symbol requirement) {
    if (canUseFastUidMap) {
      MapToPrimitive.Entry fastUidMapEntry = fastUidMap.getEntry(requirement);
      return fastUidMapEntry == null ? miscIdx : fastUidMapEntry.getIntValue();
    }
    Integer uidInteger =
      (Integer)symbolsToInts.get(Language.training.getCanonicalArg(requirement));
    if (uidInteger == null)
      return miscIdx;
    else
      return uidInteger.intValue();
  }

  /** Returns the number of requirements contained in this subcat bag. */
  public int size() {
    return counts[sizeIdx];
  }

  /**
   * Returns <code>true</code> if and only if there are zero requirements
   * in this subcat bag.
   */
  public boolean empty() {
    return size() == 0;
  }

  public boolean contains(Symbol requirement) {
    int uid = getUid(requirement);
    // if the uid is of an actual nonterminal (either greater than firstRealUid,
    // which is used for gap requirements, or equal to miscIdx) and if it's not
    // marked as an argument, return false
    if ((uid == miscIdx || uid > firstRealUid) &&
	!Language.training.isArgumentFast(requirement))
      return false;

    return counts[uid] > 0;
  }

  /**
   * Returns an itrerator over the elements of this subcat bag, returning
   * the canonical version of symbols for each the categories described in
   * {@link #add(Symbol)}; for each occurrence of a miscellaneous item
   * present in this subcat bag, the return value of {@link Training#stopSym}
   * is returned.
   */
  public Iterator iterator() {
    return new Itr();
  }

  /**
   * Returns a deep copy of this subcat bag.
   */
  public Event copy() {
    SubcatBag subcatCopy = new SubcatBag();
    subcatCopy.counts = (byte[])this.counts.clone();
    return subcatCopy;
  }

  /**
   * Computes the hash code for this subcat.
   */
  public int hashCode() {
    // do the efficient version if we can afford the 2-bit bit-shifts
    if (counts[sizeIdx] == 0)
      return 0;
    if (counts.length <= 16) {
      int code = 0;
      for (int i = counts.length - 1; i >= 0; i--)
	code = (code << 2) ^ counts[i];
      return code;
    }
    // otherwise do the slightly less efficient version
    else {
      int code = 0;
      for (int i = counts.length - 1; i >= 0; i--)
	code = (code * 31) + counts[i];
      return code;
    }
  }

  /**
   * Returns <code>true</code> if and only if the specified object is of
   * type <code>SubcatBag</code> and has the same number of requirement
   * categories and has the same counts for each of those requirement
   * categories.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof SubcatBag))
      return false;
    SubcatBag other = (SubcatBag)obj;
    if (counts.length != other.counts.length)
      return false;
    int len = counts.length;
    for (int i = 0; i < len; i++)
      if (counts[i] != other.counts[i])
	return false;
    return true;
  }

  /**
   * Returns a human-readable string representation of the
   * requirements contained in this bag.  Note that nonterminals that are
   * not in the miscellaneous bag will contain argument augmentations.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(6 * counts.length);
    sb.append("size=").append(counts[sizeIdx]).append(" ");
    for (int i = firstRealUid; i < counts.length; i++)
      sb.append(symbols[i]).append("=").append(counts[i]).append(" ");
    sb.append("misc=").append(counts[miscIdx]);
    return sb.toString();
  }


  private final class Itr implements Iterator {
    int totalCounter = SubcatBag.this.counts[sizeIdx];
    int countIdx = firstRealUid;
    int counter = SubcatBag.this.counts[countIdx];

    public boolean hasNext() {
      return totalCounter > 0;
    }

    public Object next() {
      if (totalCounter == 0)
	throw new NoSuchElementException();

      // if there are more counts at the current count index...
      if (counter > 0) {
	counter--;
	totalCounter--;
	return symbols[countIdx];
      }
      // else go hunting for the next place with non-zero counts
      countIdx++;
      while (countIdx < counts.length && counts[countIdx] == 0)
	countIdx++;
      if (countIdx == counts.length)
	countIdx = miscIdx;

      counter = counts[countIdx] - 1;
      totalCounter--;
      return symbols[countIdx];
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  };

  public Subcat getCanonical(boolean copyInto, Map map) {
    Subcat mapElt = (Subcat)map.get(this);
    if (mapElt == null) {
      Subcat putInMap = copyInto ? (Subcat)this.copy() : this;
      map.put(putInMap, putInMap);
      return putInMap;
    }
    else {
      return mapElt;
    }
  }

  // methods to comply with the MutableEvent interface

  public MutableEvent add(Object obj) {
    return add((Symbol)obj);
  }
  public MutableEvent add(int type, Object obj) {
    return add((Symbol)obj);
  }
  /** This method does nothing and returns. */
  public void ensureCapacity(int size) { return; }
  /** This method does nothing and returns. */
  public void ensureCapacity(int type, int size) { return; }
  /**
   * This method returns the one class that <code>Subcat</code> objects
   * need to support: <code>Symbol.class</code>.
   */
  public Class getClass(int type) { return Symbol.class; }
  /**
   * Returns <tt>0</tt> if the specified class is equal to
   * <code>Symbol.class</code>, <tt>-1</tt> otherwise.
   */
  public int typeIndex(Class cl) {
    if (cl.equals(Symbol.class))
      return 0;
    else
      return -1;
  }
  /**
   * Returns 1 (<code>Subcat</code> objects only support <code>Symbol</code>
   * objects).
   */
  public int numTypes() { return 1; }
  /** An alias for {@link #size()}. */
  public int numComponents() { return size(); }
  /** An alias for {@link #size()}. */
  public int numComponents(int type) { return size(); }
  /**
   * This method does nothing and returns -1, as no internal data to this
   * class can be canonicalized.
   */
  public int canonicalize(Map canonical) { return -1; }
  /** This method sets all counts of this subcat bag to zero. */
  public void clear() {
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }
  /**
   * Gets the <code>index</code><sup>th</sup> components of this subcat bag.
   * <p>
   * <b>Efficiency note</b>: The time complexity of this method is linear in
   * the number of requirement types.
   *
   * @param type an unused type parameter (<code>Subcat</code> events only
   * support the type <code>Symbol</code>, so this argument is effectively
   * superfluous for this class)
   * @param index the index of the requirement to get
   * @return the <code>index</code><sup>th</sup> <code>Symbol</code> of this
   * subcat bag, as would be returned by the <code>index</code><sup>th</sup>
   * invocation of <code>next</code> from the iterator returned by
   * {@link #iterator()}
   */
  public Object get(int type, int index) {
    int totalCounter = size();
    if (index < 0 || index >= totalCounter)
      throw new IndexOutOfBoundsException();
    int countIdx = firstRealUid;
    for (; countIdx < counts.length && index >= counts[countIdx]; countIdx++)
      index -= counts[countIdx];
    if (countIdx == counts.length)
      countIdx = miscIdx;
    return symbols[countIdx];
  }

  /**
   * As per the contract of <code>Subcat</code>, this method returns a
   * <code>Sexp</code> such that an equivalent <code>SubcatBag</code> object
   * would result from the {@link #addAll(SexpList)} method being invoked with
   * this <code>Sexp</code> as its argument.
   * <p>
   * <b>N.B.</b>: For each occurrence of a miscellaneous item present
   * in this subcat bag, the returned list will contain the symbol
   * {@link Training#stopSym} augmented with the argument augmentation:
   * <pre>
   * Symbol.get({@link Training#stopSym()}.toString() +
   *            {@link Treebank#canonicalAugDelimiter()} +
   *            {@link Training#defaultArgAugmentation()});
   * </pre>
   */
  public Sexp toSexp() {
    int size = numComponents();
    SexpList list = new SexpList(size);
    for (int i = 0; i < size; i++)
      list.add((Symbol)get(0, i));
    return list;
  }

  /**
   * Writes this object to the specified output stream.
   *
   * @param stream the stream to which to write this object
   * @throws IOException if there is a problem writing to the specified stream
   */
  public void writeExternal(ObjectOutput stream) throws IOException {
    stream.writeByte(size());
    stream.writeInt(counts.length - 1);
    for (int countIdx = 1; countIdx < counts.length; countIdx++) {
      stream.writeObject(symbols[countIdx]);
      stream.writeByte(counts[countIdx]);
    }
  }

  /**
   * Reads a serialized instance of this class from the specified stream.
   *
   * @param stream the stream from which to read a serialized instance of this
   *               class
   * @throws IOException            if there is a problem reading from the
   *                                specified stream
   * @throws ClassNotFoundException if the concrete type of the object to be
   *                                read cannot be found
   */
  public void readExternal(ObjectInput stream)
    throws IOException, ClassNotFoundException {
    counts[sizeIdx] = stream.readByte();
    int numPairs = stream.readInt();
    for (int i = 0; i < numPairs; i++) {
      Symbol requirement = (Symbol)stream.readObject();
      counts[getUid(requirement)] = stream.readByte();
    }
  }

  public void become(Subcat other) {
    SubcatBag otherBag = (SubcatBag)other;
    System.arraycopy(otherBag.counts, 0, this.counts, 0,
		     otherBag.counts.length);
  }
}
