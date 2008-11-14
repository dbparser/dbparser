package danbikel.parser;

import danbikel.lisp.Symbol;
import danbikel.util.HashMapInt;
import danbikel.util.MapToPrimitive;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.io.Serializable;

/**
 * A class to represent data common to all {@link SubcatBag} instances created
 * by a {@link danbikel.parser.SubcatBagFactory}.
 */
public class SubcatBagCommon implements Serializable, Settings.Change {
  // constants
  // index constants: make sure to update remove method if these change
  // (if it's necessary: see comment inside remove method code)
  public final static int sizeIdx = 0;
  public final static int miscIdx = 1;
  public final static int firstRealUid = 2; // must be greater than miscIdx!!!
  public final static int gapIdx = firstRealUid;

  // data members
  public int numUids;

  // static data members
  transient private Runtime rt;
  transient private IdentityHashMap<SubcatBag, Object> serializationData =
    new IdentityHashMap<SubcatBag, Object>();
  private Map<Symbol, Integer> symbolsToInts =
    new danbikel.util.HashMap<Symbol, Integer>();
  private Symbol[] symbols;
  private Nonterminal[] nonterminals;
  private Map<Symbol, Nonterminal> symToNt =
    new HashMap<Symbol, Nonterminal>();

  private HashMapInt<Symbol> fastUidMap = new HashMapInt<Symbol>();
  private volatile boolean canUseFastUidMap = false;

  public SubcatBagCommon(Runtime rt) {
    init(rt);
    rt.settings().register(this);
  }

  private void init(Runtime rt) {
    this.rt = rt;
    if (rt == null) {
      return;
    }
    Symbol stopSym = rt.language().training().stopSym();
    Symbol gapAugmentation = rt.language().training().gapAugmentation();
    Symbol argAugmentation = rt.language().training().defaultArgAugmentation();
    char delimChar = rt.language().treebank().canonicalAugDelimiter();

    int uid = gapIdx; // depends on gapIdx being equal to firstRealUid
    // kind of a hack: put an entry for gaps (which are "requirements"
    // that can be thrown into subcats);
    // note that uid of gap is firstRealUid (see comment inside remove method)
    symbolsToInts.put(gapAugmentation, uid++);

    for (Object argObj : rt.language().training().argNonterminals()) {
      Symbol argLabel = (Symbol) argObj;
      symbolsToInts.put(argLabel, uid++);
    }
    numUids = uid;

    symbols = new Symbol[numUids];
    nonterminals = new Nonterminal[numUids];
    for (Map.Entry<Symbol, Integer> entry : symbolsToInts.entrySet()) {
      Symbol symbol = entry.getKey();
      uid = entry.getValue();
      symbols[uid] = symbol;
      nonterminals[uid] = rt.language().treebank().parseNonterminal(symbol);
    }
    symbols[miscIdx] = Symbol.get(stopSym.toString() +
				  delimChar + argAugmentation);
  }

  public void setUpFastUidMap(CountsTable nonterminals) {
    if (canUseFastUidMap)
      return;
    fastUidMap.put(rt.language().training().gapAugmentation(), gapIdx);
    for (Object ntObj : nonterminals.keySet()) {
      Symbol nt = (Symbol) ntObj;
      int uid = getUid(nt);
      fastUidMap.put(nt, uid);
    }
    canUseFastUidMap = true;
  }

  public int getUid(Symbol requirement) {
    if (canUseFastUidMap) {
      MapToPrimitive.Entry fastUidMapEntry = fastUidMap.getEntry(requirement);
      return fastUidMapEntry == null ? miscIdx : fastUidMapEntry.getIntValue();
    }
    // get Nonterminal for the specified requirement (create if necessary)
    Nonterminal requirementNt = symToNt.get(requirement);
    if (requirementNt == null) {
      requirementNt = rt.language().treebank().parseNonterminal(requirement);
      symToNt.put(requirement, requirementNt);
    }
    boolean found = false;
    Nonterminal[] nts = nonterminals;
    int uid = firstRealUid;
    for (; uid < nts.length; uid++) {
      if (nts[uid].subsumes(requirementNt)) {
	found = true;
	break;
      }
    }
    return found ? uid : miscIdx;
  }

  /**
   * A method to check if the specified requirement is valid. For this class, a
   * requirement is valid if it is either {@link danbikel.parser.Training#gapAugmentation}
   * or a symbol for which {@link danbikel.parser.Training#isArgumentFast(danbikel.lisp.Symbol)}
   * returns <code>true</code>. A subclass may override this method to allow for
   * new or different valid requirements.
   *
   * @param requirement the requirement to test
   * @return whether the specified requirement is valid
   */
  public boolean validRequirement(Symbol requirement) {
    return
      requirement == rt.language().training().gapAugmentation() ||
      rt.language().training().isArgumentFast(requirement);
  }

  public final Runtime rt() {
    return rt;
  }

  public final int getNumUids() {
    return numUids;
  }

  public Map<Symbol, Integer> getSymbolsToInts() {
    return symbolsToInts;
  }

  public Symbol[] getSymbols() {
    return symbols;
  }

  public Nonterminal[] getNonterminals() {
    return nonterminals;
  }

  public Map<Symbol, Nonterminal> getSymToNt() {
    return symToNt;
  }

  public void update(Map<String, String> changedSettings, Settings settings) {
    init(rt);
  }
}
