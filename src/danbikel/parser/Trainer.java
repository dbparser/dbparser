package danbikel.parser;

import java.util.HashMap;
import danbikel.lisp.*;
import danbikel.util.*;
import danbikel.parser.util.Util;
import java.util.*;
import java.io.*;

/**
 * Derives all counts necessary to compute the probabilities for this parser,
 * including the top-level counts and all derived counts.  The two main
 * functionalities provided by this class are the loading and storing
 * of a text file containing top-level event counts and the loading and storing
 * of a Java object file containing all derived event counts.
 * <p>
 * All top-level events or mappings are recorded as S-expressions with the
 * format
 * <pre>(name event count)</pre>
 * for events and
 * <pre>(name key value)</pre>
 * for mappings.
 * <p>
 * All derived counts are stored by the internal data structures of
 * several <code>Model</code> objects, which are in turn all contained within
 * a single <code>ModelCollection</code> object.  This class provides methods
 * to load and store a Java object file containing this
 * <code>ModelCollection</code>, as well as some initial objects containing
 * information about the <code>ModelCollection</code> object (see the
 * <tt>-scan</tt> flag in the usage of {@link #main the main method of this
 * class}).
 * <p>
 * The various model objects capture the generation submodels of the different
 * output elements of the parser.  The smoothing levels of these
 * submodels are represented by <code>ProbabilityStructure</code> objects,
 * passed as parameters to the <code>Model</code> objects, at
 * {@link Model#Model(ProbabilityStructure) construction} time.  This
 * architecture provides a type of "plug-n-play" smoothing scheme for the
 * various submodels of this parser.
 *
 * @see #main(String[])
 * @see Model
 * @see ProbabilityStructure
 */
public class Trainer implements Serializable {

  // constants
  //static boolean secretFlag;

  private final static String className = Trainer.class.getName();
  // constants for default classname prefixes for ProbabilityStructure
  // subclasses
  private final static String packagePrefix =
    className.substring(0, (className.lastIndexOf('.') + 1));
  private final static String lexPriorModelStructureClassnamePrefix =
    packagePrefix + "LexPriorModelStructure";
  private final static String nonterminalPriorModelStructureClassnamePrefix =
    packagePrefix + "NonterminalPriorModelStructure";
  private final static String topNonterminalModelStructureClassnamePrefix =
    packagePrefix + "TopNonterminalModelStructure";
  private final static String topLexModelStructureClassnamePrefix =
    packagePrefix + "TopLexModelStructure";
  private final static String headModelStructureClassnamePrefix =
    packagePrefix + "HeadModelStructure";
  private final static String gapModelStructureClassnamePrefix =
    packagePrefix + "GapModelStructure";
  private final static String leftSubcatModelStructureClassnamePrefix =
    packagePrefix + "LeftSubcatModelStructure";
  private final static String rightSubcatModelStructureClassnamePrefix =
    packagePrefix + "RightSubcatModelStructure";
  private final static String modNonterminalModelStructureClassnamePrefix =
    packagePrefix + "ModNonterminalModelStructure";
  private final static String modWordModelStructureClassnamePrefix =
    packagePrefix + "ModWordModelStructure";
  // fallback defaults (used in constructor)
  private final static int defaultUnknownWordThreshold = 3;
  private final static int defaultReportingInterval = 100;
  private final static int defaultNumPrevMods = 1;

  // constant used in initialization of numeric property values
  private final static Object[][] numericProperties =
  {{Settings.unknownWordThreshold, new Integer(defaultUnknownWordThreshold)},
   {Settings.trainerReportingInterval, new Integer(defaultReportingInterval)},
   {Settings.numPrevMods, new Integer(defaultNumPrevMods)}};

  // types of events gathered by the trainer (provided as public constants
  // so that users can interpret human-readable trainer output file

  /** The label for nonterminal generation events.  This symbol has the
      print-name <tt>&quot;nonterminal&quot;</tt>. */
  public final static Symbol nonterminalEventSym = Symbol.add("nonterminal");
  /** The label for head nonterminal generation events.  This symbol has the
      print-name <tt>&quot;head&quot;</tt>. */
  public final static Symbol headEventSym = Symbol.add("head");
  /** The label for modifier nonterminal generation events.  This symbol has
      the print-name <tt>&quot;mod&quot;</tt>. */
  public final static Symbol modEventSym = Symbol.add("mod");
  /** The label for gap events.  This symbol has the
      print-name <tt>&quot;gap&quot;</tt>. */
  public final static Symbol gapEventSym = Symbol.add("gap");
  /** The label for word to part-of-speech mappings.  This symbol has the
      print-name <tt>&quot;pos&quot;</tt>. */
  public final static Symbol posMapSym = Symbol.add("pos");
  /** The label for vocabulary counts.  This symbol has the
      print-name <tt>&quot;vocab&quot;</tt>. */
  public final static Symbol vocabSym = Symbol.add("vocab");
  /** The label for word feature (unknown vocabulary) counts.  This
      symbol has the print-name <tt>&quot;word-feature&quot;</tt>. */
  public final static Symbol wordFeatureSym = Symbol.add("word-feature");

  // integer types for mapping the above symbols to ints for a switch statement
  private final static int nonterminalEventType = 1;
  private final static int headEventType = 2;
  private final static int modEventType = 3;
  private final static int gapEventType = 4;
  private final static int posMapType = 5;
  private final static int vocabType = 6;
  private final static int wordFeatureType = 7;

  private final static Object[][] eventsToTypesArr = {
    {nonterminalEventSym, new Integer(nonterminalEventType)},
    {headEventSym, new Integer(headEventType)},
    {modEventSym, new Integer(modEventType)},
    {gapEventSym, new Integer(gapEventType)},
    {posMapSym, new Integer(posMapType)},
    {vocabSym, new Integer(vocabType)},
    {wordFeatureSym, new Integer(wordFeatureType)},
  };

  private final static Map eventsToTypes = new HashMap();
  static {
    for (int i = 0; i < eventsToTypesArr.length; i++)
      eventsToTypes.put(eventsToTypesArr[i][0], eventsToTypesArr[i][1]);
  }

  // data members

  // settings
  private int unknownWordThreshold;
  private int reportingInterval;
  private int numPrevMods;
  private boolean keepAllWords;
  private boolean downcaseWords;

  // data storage for training
  private CountsTable nonterminals = new CountsTable();
  private CountsTable priorEvents = new CountsTable();
  private CountsTable headEvents = new CountsTable();
  private CountsTable modifierEvents = new CountsTable();
  private CountsTable gapEvents = new CountsTable();
  private CountsTable vocabCounter = new CountsTable();
  private CountsTable wordFeatureCounter = new CountsTable();
  private Map originalWordCounter = new HashMap();
  private Set vocab = new HashSet();
  private Map posMap = new HashMap();
  private Map leftSubcatMap = new HashMap();
  private Map rightSubcatMap = new HashMap();
  private Map leftModNonterminalMap = new HashMap();
  private Map rightModNonterminalMap = new HashMap();
  // temporary map for canonicalizing subcat SexpList objects for
  // leftSubcatMap and rightSubcatMap data members
  transient private Map canonicalSubcatMap;
  transient private Subcat emptySubcat = Subcats.get();
  private HashSet wordFeatureTypes = new HashSet();
  private HashSet unknownWords = new HashSet();
  private ModelCollection modelCollection = new ModelCollection();

  // handle onto static WordFeatures object from Language object
  transient private WordFeatures wordFeatures = Language.wordFeatures;

  // handles onto some data from Training for more efficient and more readable
  // code
  private Symbol startSym = Language.training.startSym();
  private Symbol stopSym = Language.training.stopSym();
  private Symbol topSym = Language.training.topSym();
  private Word stopWord = Language.training.stopWord();
  private Symbol gapAugmentation = Language.training.gapAugmentation();
  private Symbol traceTag = Language.training.traceTag();

  // various filters used by deriveCounts and deriveSubcatMaps
  Filter allPass = new AllPass();
  Filter nonTop = new Filter() {
      public boolean pass(Object obj) {
        return ((TrainerEvent)obj).parent() != topSym;
      }
    };
  Filter topOnly = new Filter() {
      public boolean pass(Object obj) {
        return ((TrainerEvent)obj).parent() == topSym;
      }
    };
  Filter leftOnly = new Filter() {
      public boolean pass(Object obj) {
        return ((TrainerEvent)obj).side() == Constants.LEFT;
      }
    };
  Filter rightOnly = new Filter() {
      public boolean pass(Object obj) {
        return ((TrainerEvent)obj).side() == Constants.RIGHT;
      }
    };
  Filter nonStop = new Filter() {
      public boolean pass(Object obj) {
        return ((ModifierEvent)obj).modifier() != stopSym;
      }
    };



  // constructor

  /**
   * Constructs a new training object, which uses values from {@link Settings}
   * for its settings.  This class is not thread-safe, and there will typically
   * be one instance of a <code>Trainer</code> object per process, constructed
   * via the {@link #main} method of this class.
   *
   * @see Settings#unknownWordThreshold
   * @see Settings#trainerReportingInterval
   * @see Settings#numPrevMods
   */
  public Trainer() {
    int[] numericPropertyValues = new int[numericProperties.length];

    for (int i = 0; i < numericProperties.length; i++) {
      try {
	numericPropertyValues[i] =
	  Integer.parseInt(Settings.get((String)numericProperties[i][0]));
      }
      catch (NumberFormatException nfe) {
	System.err.println(className + ": warning: property " +
			   numericProperties[i][0] + " was not a parsable " +
			   "number\n\t" + nfe + "\n\tsetting value to "+
			   "fallback default: " + numericProperties[i][1]);
	numericPropertyValues[i] =
	  ((Integer)numericProperties[i][1]).intValue();
      }
    }

    // coordinate the setting of these data members with the order of the
    // numericProperties array
    unknownWordThreshold = numericPropertyValues[0];
    reportingInterval = numericPropertyValues[1];
    numPrevMods = numericPropertyValues[2];

    String keepAllWordsStr = Settings.get(Settings.keepAllWords);
    keepAllWords = Boolean.valueOf(keepAllWordsStr).booleanValue();
    String downcaseWordsStr = Settings.get(Settings.downcaseWords);
    downcaseWords = Boolean.valueOf(downcaseWordsStr).booleanValue();
  }

  /**
   * Records observations from the training trees contained in the
   * specified S-expression tokenizer.  The observations are either mappings
   * stored in <code>Map</code> objects or items to be counted, stored in
   * <code>CountsTable</code> objects.  All the trees obtained from
   * <code>tok</code> are first preprocessed using
   * <code>Training.preProcess(Sexp)</code>.
   *
   * @param tok the S-expression tokenizer from which to obtain training
   * parse trees
   * @param auto indicates whether to automatically determine whether to
   * strip off outer parens of training parse trees before preprocessing;
   * if the value of this argument is <code>false</code>, then the value
   * of <code>stripOuterParens</code> is used
   * @param stripOuterParens indicates whether an outer layer of parentheses
   * should be stripped off of trees obtained from <code>tok</code> before
   * preprocessing and training (only used if the <code>auto</code> argument
   * is <code>false</code>)
   *
   * @see CountsTable
   * @see Training#preProcess(Sexp)
   */
  public void train(SexpTokenizer tok, boolean auto, boolean stripOuterParens)
    throws IOException {
    Sexp tree = null;
    int sentNum = 0, intervalCounter = 0;
    for ( ; (tree = Sexp.read(tok)) != null; sentNum++, intervalCounter++) {
      //System.err.println(tree);

      if (intervalCounter == reportingInterval) {
	System.err.println(className + ": processed " + sentNum + " sentence" +
			   (sentNum > 1 ? "s" : ""));
	intervalCounter = 0;
      }

      if (!tree.isList()) {
        System.err.println(className + ": error: invalid format for training " +
                           "parse tree: " + tree + " ...skipping");
        continue;
      }

      // parenthesis-stripping is indicated if the training tree is a list
      // containing one element that is also a list
      if (auto)
	stripOuterParens = (tree.list().length() == 1 &&
			    tree.list().get(0).isList());
      if (stripOuterParens)
	tree = tree.list().get(0);

      //System.err.println(tree);

      Language.training.preProcess(tree);

      //System.err.println(Util.prettyPrint(tree));

      HeadTreeNode headTree = new HeadTreeNode(tree);
      if (downcaseWords)
	downcaseWords(headTree);
      canonicalSubcatMap = new HashMap();
      collectStats(tree, headTree, true);
      canonicalSubcatMap = null; // it has served its purpose

      /*
      if (secretFlag == true) {
	System.out.println(tree);
	secretFlag = false;
      }
      */
    }

    System.err.println(className + ": processed " + sentNum + " sentence" +
		       (sentNum > 1 ? "s " : " ") + "in total");

    if (unknownWordThreshold > 1) {
      Time time = new Time();
      System.err.println("Altering low frequency words (words occurring " +
			 "fewer than " + unknownWordThreshold + " times).");
      alterLowFrequencyWords();

      System.err.println("Finished altering low frequency words in " + time +
			 ".");
    }

    System.err.print("Creating part-of-speech map...");
    System.err.flush();
    createPosMap();
    System.err.println("done.");

    //countUniqueBigrams();
  }

  private void downcaseWords(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	headWord.setOriginalWord(headWord.word());
	headWord.setWord(Symbol.add(headWord.word().toString().toLowerCase()));
      }
    }
    else {
      downcaseWords(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
    }
  }

  /**
   * Collects the statistics from the specified tree.  Some
   * &quot;statistics&quot; are actually mappings, such as
   * part-of-speech-to-word mappings.
   *
   * @param orig the original (preprocessed) tree, used for debugging purposes
   * @param tree the tree from which to collect statistics and mappings
   * @param isRoot indicates whether <code>tree</code> is the observed root
   * of a tree (the observed root is the child of the hidden root,
   * represented by the symbol {@link Training#topSym})
   *
   */
  private void collectStats(Sexp orig, HeadTreeNode tree, boolean isRoot) {
    if (isRoot) {
      // add special head transition from +TOP+ to actual root of tree
      headEvents.add(new HeadEvent(tree.headWord(), topSym, tree.label(),
				   emptySubcat, emptySubcat));
      //nonterminals.add(topSym); // +TOP+ is a nonterminal, too!
    }
    if (tree.isPreterminal()) {
      Word word = tree.headWord();
      if (word.tag() != traceTag) {
	//nonterminals.add(word.tag());// parts of speech are nonterminals, too!
	vocabCounter.add(word.word());
	addToValueCounts(originalWordCounter,
			 word.word(), new SymbolPair(word.originalWord(),
						     Symbol.add(word.index())));
      }
    }
    else {
      nonterminals.add(tree.label());
      // head generation event
      Subcat leftSubcat = collectArguments(tree.preMods());
      Subcat rightSubcat = collectArguments(tree.postMods());

      Symbol parent = tree.label();
      Symbol head = tree.headChild().label();

      boolean parentHasGap = Language.training.hasGap(parent);
      boolean headHasGap = Language.training.hasGap(head);

      int leftGapIdx = -1;
      int rightGapIdx = -1;

      // take care of gap passed from parent to head child
      // this isn't *strictly* necessary, as the headEvent contains all
      // this information (via the gap augmentations on parent and head child)
      if (parentHasGap) {
	if (headHasGap) {
	  GapEvent gapEvent = new GapEvent(GapEvent.toHead, tree.headWord(),
					   parent, head);
	  gapEvents.add(gapEvent);
	}
	else {
	  // find gap index, which is either on left or right side of head
	  leftGapIdx = hasGapOrTrace(tree.preMods());
	  rightGapIdx = hasGapOrTrace(tree.postMods());
	}
      }

      leftSubcat = leftSubcat.getCanonical(false, canonicalSubcatMap);
      rightSubcat = rightSubcat.getCanonical(false, canonicalSubcatMap);

      HeadEvent headEvent = new HeadEvent(tree.headWord(),
					  parent, head,
					  leftSubcat, rightSubcat);
      headEvents.add(headEvent);

      collectModifierStats(tree, leftSubcat, leftGapIdx, Constants.LEFT);
      collectModifierStats(tree, rightSubcat, rightGapIdx, Constants.RIGHT);

      // make recursive call to head child
      collectStats(orig, tree.headChild(), false);
      // make recursive calls on pre- and postmodifiers
      Iterator mods = tree.preMods().iterator();
      while (mods.hasNext())
	collectStats(orig, (HeadTreeNode)mods.next(), false);
      mods = tree.postMods().iterator();
      while (mods.hasNext())
	collectStats(orig, (HeadTreeNode)mods.next(), false);
    }
  }

  /**
   * Creates and returns a new start list.  A start list is a list of length
   * equal to the value of <tt>Settings.get(Settings.numPrevMods)</tt>, where
   * every element is the symbol <code>Language.training.startSym()</code>.
   * This is the appropriate initial list of previously "generated" modidifers
   * when beginning the Markov process of generating modifiers.
   *
   * @return a new list of start symbols
   *
   * @see Training#startSym()
   */
  public static SexpList newStartList() {
    int numPrevMods = Integer.parseInt(Settings.get(Settings.numPrevMods));
    return newStartList(numPrevMods);
  }

  private static final SexpList newStartList(int numPrevMods) {
    SexpList startList = new SexpList(numPrevMods);
    for (int i = 0; i < numPrevMods; i++)
      startList.add(Language.training.startSym());
    return startList;
  }

  /**
   * Note the O(n) operation performed on the prevModList.
   */
  private void collectModifierStats(HeadTreeNode tree,
				    Subcat subcat,
				    int gapIdx,
				    boolean side) {
    Symbol parent = tree.label();
    Symbol head = tree.headChild().label();
    // it's crucial to use a copy of the head word, so that
    // alterLowFrequencyWords doesn't change equals status of ModifierEvent
    // objects when it alters words from HeadEvent objects (i.e., we don't
    // want ModifierEvent and HeadEvent objects to share the same head word
    // Word objects)
    Word headWord = tree.headWord().copy();
    Iterator mods = (side == Constants.LEFT ?
		     tree.preMods().iterator() : tree.postMods().iterator());

    SexpList prevModList = newStartList(numPrevMods);
    Subcat dynamicSubcat = (Subcat)subcat.copy();
    // if there's a gap to generate, add as requirement to end of subcat list
    if (gapIdx != -1)
      dynamicSubcat.add(gapAugmentation);
    boolean verbIntervening = false;

    // collect modifier generation events
    boolean prevModHadVerb = false;
    // the next two booleans only necessary for (currently) unused method of
    // determining modifier adjacency, which is that no *words* appear between
    // head and currently-generated left- or right-edge frontier word of
    // currently-generated modifier subtree
    boolean wordsIntervening = false;
    boolean headAlreadyHasMods =
      (side == Constants.LEFT ?
       tree.headChild().leftIdx() < headWord.index() :
       tree.headChild().rightIdx() > headWord.index() + 1);
    for (int modIdx = 0; mods.hasNext(); modIdx++) {
      HeadTreeNode currMod = (HeadTreeNode)mods.next();
      Symbol modifier = currMod.label();
      verbIntervening |= prevModHadVerb;
      wordsIntervening = (modIdx > 0 ? true : headAlreadyHasMods);
      Subcat canonicalDynamicSubcat =
	dynamicSubcat.getCanonical(false, canonicalSubcatMap);
      // crucial to copy modifier's head word object (see comment above)
      ModifierEvent modEvent = new ModifierEvent(currMod.headWord().copy(),
						 headWord,
						 modifier,
						 new SexpList(prevModList),
						 parent,
						 head,
						 canonicalDynamicSubcat,
						 verbIntervening,
						 side);
      modifierEvents.add(modEvent);

      if (modIdx == gapIdx) {
	Symbol direction = (side == Constants.LEFT ?
			    GapEvent.toLeft : GapEvent.toRight);
	GapEvent gapEvent = new GapEvent(direction, headWord, parent, head);
	gapEvents.add(gapEvent);
	if (dynamicSubcat.size() == 0)
	  System.err.println(className + ": error: gap detected in " +
			     tree + " but subcat list is empty!");
	if (dynamicSubcat.contains(gapAugmentation)) {
	  dynamicSubcat = (Subcat)dynamicSubcat.copy();
	  dynamicSubcat.remove(gapAugmentation);
	}
	else {
	  System.err.println(className + ": warning: gap detected in " +
			     "modifier " + tree.label() + " but not present " +
			     "in subcat list");
	}
      }

      if (dynamicSubcat.contains(modifier)) {
	dynamicSubcat = (Subcat)dynamicSubcat.copy();
	dynamicSubcat.remove(modifier);
      }

      prevModHadVerb = currMod.containsVerb();
      prevModList.remove(prevModList.length() - 1);
      prevModList.add(0, currMod.label()); // an O(n) op (list is usu. len. 1)
    }

    if (!dynamicSubcat.empty())
      System.err.println(className + ": warning: dynamic subcat not empty: " +
			 dynamicSubcat);

    Subcat emptySubcat = dynamicSubcat.getCanonical(false, canonicalSubcatMap);

    // transition to stop symbol
    verbIntervening |= prevModHadVerb;
    ModifierEvent modEvent = new ModifierEvent(stopWord,
					       headWord,
					       stopSym,
					       prevModList,
					       parent,
					       head,
					       emptySubcat,
					       verbIntervening,
					       side);
    modifierEvents.add(modEvent);
  }

  private void alterLowFrequencyWords() {
    System.err.println("\tInitial vocab size is " + vocabCounter.size());

    int headEventsModified = alterLowFrequencyWords(headEvents);
    System.err.println("\tModified " + headEventsModified + " head events");

    int modEventsModified = alterLowFrequencyWords(modifierEvents);
    System.err.println("\tModified " + modEventsModified + " mod events");

    int gapEventsModified = alterLowFrequencyWords(gapEvents);
    System.err.println("\tModified " + gapEventsModified + " gap events");

    System.err.println("\tNumber of distinct low-frequency words: " +
		       unknownWords.size());

    System.err.println("\tNumber of distinct word-feature vectors: " +
		       wordFeatureTypes.size());

    System.err.println("\tUpdating vocabulary counts to include altered " +
		       "low-frequency words.");
    updateVocab(unknownWords);

    System.err.println("\tNew vocab size is " + vocabCounter.size());

    unknownWords = null; // it has served its purpose
  }

  private void updateVocab(Set unknownWords) {
    // first, remove all low-frequency words from vocabCounter, if
    // keepAllWords is false
    if (keepAllWords == false) {
      Iterator unknowns = unknownWords.iterator();
      while (unknowns.hasNext())
	vocabCounter.remove(unknowns.next());
    }

    // next, for each low-frequency word, look it up in originalWordCounter
    // to get all the counts of its original, mixed-case ocurrences and index
    // values, so as to add the appropriate feature vectors (and counts)
    // to the word feature (unknown vocab) counter
    Iterator unknowns = unknownWords.iterator();
    while (unknowns.hasNext()) {
      Symbol word = (Symbol)unknowns.next();
      CountsTable valueCounts = (CountsTable)originalWordCounter.get(word);
      Iterator values = valueCounts.keySet().iterator();
      while (values.hasNext()) {
	SymbolPair pair = (SymbolPair)values.next();
	Symbol origWord = pair.first();
	int index = pair.second().getInteger().intValue();
	int count = valueCounts.count(pair);
	wordFeatureCounter.add(wordFeatures.features(origWord, index == 0),
				count);
      }
    }

    originalWordCounter = null; // it has served its purpose
  }

  private int alterLowFrequencyWords(CountsTable events) {
    CountsTable tempEvents = new CountsTable();

    int numModifiedEvents = 0;

    Iterator it = events.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      int count = entry.getIntValue(0);
      boolean eventModified = alterLowFrequencyWords(tempEvents, event, count);
      if (eventModified) {
	numModifiedEvents++;
	if (keepAllWords == false)
	  it.remove();
      }
    }
    events.putAll(tempEvents);

    return numModifiedEvents;
  }

  /**
   * This method puts a copy of the specified event in the
   * <code>tempEvents</code> counts table if either its head word or
   * modifier head word (if <code>event</code> has a valid
   * <code>modHeadWord</code>) occur with a frequency less than unknown word
   * threshold.  Crucially, the copied event will have the appropriate
   * head words modified to be word-feature vectors, as returned by the
   * method {@link WordFeatures#features(String,boolean)}.
   * <br>
   * <b>Bugs</b>: The second argument to {@link
   * WordFeatures#features(String,boolean)} indicates whether the word
   * that is the first argument was the first word in its sentence.  A
   * proper implementation would check to see if the word is the first
   * <i>alphanumeric</i> word in the sentence, instead of simply checking for
   * the word's index being equal to zero, as is done by the current
   * implementation of this method.
   *
   * @return true if this method modifies the specified event
   */
  private final boolean alterLowFrequencyWords(CountsTable tempEvents,
					       TrainerEvent event,
					       int count) {

    boolean headLowFreq = isLowFrequencyWord(event.headWord());
    boolean modHeadLowFreq = isLowFrequencyWord(event.modHeadWord());

    if (headLowFreq || modHeadLowFreq) {

      event = event.copy();  // make deep copy of the event

      if (headLowFreq) {
	Word headWord = event.headWord();
	unknownWords.add(headWord.word());
	// see "Bugs" note above
	headWord.setWord(wordFeatures.features(headWord.originalWord(),
					       headWord.index() == 0));
	headWord.setOriginalWord(null);
	wordFeatureTypes.add(headWord.word());
      }
      if (modHeadLowFreq) {
	Word modHeadWord = event.modHeadWord();
	unknownWords.add(modHeadWord.word());
	// see "Bugs" note above
	modHeadWord.setWord(wordFeatures.features(modHeadWord.originalWord(),
						  modHeadWord.index() == 0));
	modHeadWord.setOriginalWord(null);
	wordFeatureTypes.add(modHeadWord.word());
      }
      tempEvents.add(event, count);
      return true;
    }
    return false;
  }

  private final boolean isLowFrequencyWord(Word word) {
    // we don't modify traces
    return (isRealWord(word) &&
	    vocabCounter.count(word.word()) < unknownWordThreshold);
  }


  public void createPosMap() {
    createPosMap(headEvents);
    createPosMap(modifierEvents);
    createPosMap(gapEvents);
  }
  public void createPosMap(CountsTable events) {
    Iterator it = events.keySet().iterator();
    while (it.hasNext()) {
      TrainerEvent event = (TrainerEvent)it.next();
      addToPosMap(posMap, event.headWord());
      addToPosMap(posMap, event.modHeadWord());
    }
  }

  // helper methods

  /**
   * Called by {@link #collectStats} and
   * {@link #alterLowFrequencyWords(CountsTable,Map)}.
   */
  private final void addToPosMap(Map posMap, Word word) {
    if (isRealWord(word)) {
      SexpList mapSet = (SexpList)posMap.get(word.word());
      if (mapSet == null)
	posMap.put(word.word(), (mapSet = new SexpList(2)).add(word.tag()));
      else if (!mapSet.contains(word.tag()))
	mapSet.add(word.tag());
    }
  }

  /** Called by {@link #collectStats}. */
  private final static Subcat collectArguments(List modifiers) {
    Subcat args = Subcats.get();
    Iterator mods = modifiers.iterator();
    while (mods.hasNext()) {
      HeadTreeNode mod = (HeadTreeNode)mods.next();
      Symbol modLabel = mod.label();
      args.add(modLabel);
      /*
      if (Language.training.isArgument(modLabel)) {
	if (modLabel.toString().indexOf("RRB") != -1 ||
	    modLabel.toString().indexOf("LRB") != -1)
	  secretFlag = true;
      }
      */
    }
    return args;
  }

  /** Called by {@link #collectStats}. */
  private final int hasGapOrTrace(List modifiers) {
    Iterator mods = modifiers.iterator();
    for (int i = 0; mods.hasNext(); i++)
      if (hasGapOrTrace((HeadTreeNode)mods.next()))
	return i;
    return -1;
  }

  /** Called by {@link #hasGapOrTrace(List)}. */
  private final boolean hasGapOrTrace(HeadTreeNode modifier) {
    if (Language.training.hasGap(modifier.label()))
      return true;
    if (Language.treebank.isWHNP(modifier.label()) &&
	modifier.headChild().isPreterminal() &&
	modifier.headChild().headWord().tag() == traceTag)
      return true;
    return false;
  }

  public void deriveCounts() {
    try {
      String globalModelStructureNumber =
	Settings.get(Settings.globalModelStructureNumber);
      if (globalModelStructureNumber == null) {
	globalModelStructureNumber = "1";
      }
      System.err.println(className + ": using global model structure number " +
			 globalModelStructureNumber);

      Model lexPriorModel =
	new Model(getProbStructure(lexPriorModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.lexPriorModelStructureNumber,
				   Settings.lexPriorModelStructureClass));

      Model nonterminalPriorModel =
	new Model(getProbStructure(nonterminalPriorModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.nonterminalPriorModelStructureNumber,
				   Settings.nonterminalPriorModelStructureClass));

      Model topNonterminalModel =
	new Model(getProbStructure(topNonterminalModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.topNonterminalModelStructureNumber,
				   Settings.topNonterminalModelStructureClass));

      Model topLexModel =
	new Model(getProbStructure(topLexModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.topLexModelStructureNumber,
				   Settings.topLexModelStructureClass));

      Model headModel =
	new Model(getProbStructure(headModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.headModelStructureNumber,
				   Settings.headModelStructureClass));
      Model gapModel =
	new Model(getProbStructure(gapModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.gapModelStructureNumber,
				   Settings.gapModelStructureClass));
      Model leftSubcatModel =
	new Model(getProbStructure(leftSubcatModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.leftSubcatModelStructureNumber,
				   Settings.leftSubcatModelStructureClass));
      Model rightSubcatModel =
	new Model(getProbStructure(rightSubcatModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.rightSubcatModelStructureNumber,
				   Settings.rightSubcatModelStructureClass));
      Model leftModNonterminalModel =
	new Model(getProbStructure(modNonterminalModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.modNonterminalModelStructureNumber,
				   Settings.modNonterminalModelStructureClass));
      Model rightModNonterminalModel =
	new Model(getProbStructure(modNonterminalModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.modNonterminalModelStructureNumber,
				   Settings.modNonterminalModelStructureClass));
      Model leftModWordModel =
	new Model(getProbStructure(modWordModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.modWordModelStructureNumber,
				   Settings.modWordModelStructureClass));
      Model rightModWordModel =
	new Model(getProbStructure(modWordModelStructureClassnamePrefix,
				   globalModelStructureNumber,
				   Settings.modWordModelStructureNumber,
				   Settings.modWordModelStructureClass));

      danbikel.util.HashMap canonical = new danbikel.util.HashMap();

      System.err.print("Deriving events for prior probability computations...");
      derivePriors();
      System.err.println("done.");

      lexPriorModel.deriveCounts(priorEvents, allPass, canonical);
      nonterminalPriorModel.deriveCounts(priorEvents, allPass, canonical);
      topNonterminalModel.deriveCounts(headEvents, topOnly, canonical);
      topLexModel.deriveCounts(headEvents, allPass, canonical);
      headModel.deriveCounts(headEvents, nonTop, canonical);
      gapModel.deriveCounts(gapEvents, allPass, canonical);
      leftSubcatModel.deriveCounts(headEvents, nonTop, canonical);
      rightSubcatModel.deriveCounts(headEvents, nonTop, canonical);
      leftModNonterminalModel.deriveCounts(modifierEvents, leftOnly, canonical);
      rightModNonterminalModel.deriveCounts(modifierEvents, rightOnly, canonical);
      leftModWordModel.deriveCounts(modifierEvents, leftOnly, canonical);
      rightModWordModel.deriveCounts(modifierEvents, rightOnly, canonical);

      deriveSubcatMaps(leftSubcatModel.getProbStructure(),
		       rightSubcatModel.getProbStructure(),
                       canonical);

      deriveModNonterminalMaps(leftModNonterminalModel.getProbStructure(),
                               rightModNonterminalModel.getProbStructure(),
                               canonical);

      System.err.println("Canonical events HashMap stats: " +
                         canonical.getStats());

      /*
      Iterator it = canonicalEventLists.keySet().iterator();
      while (it.hasNext()) {
        Object canonObj = it.next();
        System.err.print("canonical event: " + canonObj);
        if (canonObj instanceof Sexp) {
          it.remove();
          System.err.println("...removed");
        }
        else
          System.err.println();
      }
      */

      //canonicalEventLists = null;
      /*
      System.err.print("gc ... ");
      System.err.flush();
      System.gc();
      System.err.println("done");
      */

      if (leftModNonterminalMap == null)
        System.err.println("aiiee!  it is null!");
      modelCollection.set(lexPriorModel,
			  nonterminalPriorModel,
			  topNonterminalModel,
			  topLexModel,
			  headModel,
			  gapModel,
			  leftSubcatModel,
			  rightSubcatModel,
			  leftModNonterminalModel,
			  rightModNonterminalModel,
			  leftModWordModel,
			  rightModWordModel,
			  vocabCounter,
			  wordFeatureCounter,
			  nonterminals,
			  posMap,
			  leftSubcatMap,
			  rightSubcatMap,
                          leftModNonterminalMap,
                          rightModNonterminalMap,
                          Language.training.getPrunedPreterms(),
                          Language.training.getPrunedPunctuation(),
                          canonical);
    }
    catch (ExceptionInInitializerError e) {
      System.err.println(className + ": problem initializing an instance of " +
			 "a model class: " + e);
    }
    catch (LinkageError e) {
      System.err.println(className + ": problem linking a model class: " + e);
    }
    catch (ClassNotFoundException e) {
      System.err.println(className + ": couldn't find a model class: " + e);
    }
    catch (InstantiationException e) {
      System.err.println(className + ": couldn't instantiate a model class: " +
			 e);
    }
    catch (IllegalAccessException e) {
      System.err.println(className + ": not allowed to instantiate a model " +
			 "class: " + e);
    }
  }

  /**
   * Called by {@link #deriveCounts}.
   *
   * @param leftMS the left subcat model structure
   * @param rightMS the right subcat model structure
   */
  private void deriveSubcatMaps(ProbabilityStructure leftMS,
				ProbabilityStructure rightMS,
                                FlexibleMap canonicalMap) {
    System.err.println("Deriving subcat maps.");

    //canonicalSubcatMap = new HashMap();

    int leftMSLastLevel = leftMS.numLevels() - 1;
    int rightMSLastLevel = rightMS.numLevels() - 1;

    Filter filter = nonTop;
    Iterator events = headEvents.keySet().iterator();
    while (events.hasNext()) {
      HeadEvent headEvent = (HeadEvent)events.next();
      if (!filter.pass(headEvent))
        continue;
      Event leftContext =
	leftMS.getHistory(headEvent, leftMSLastLevel).copy();
      leftContext.canonicalize(canonicalMap);
      Subcat canonicalLeftSubcat =
	headEvent.leftSubcat().getCanonical(false, canonicalMap);
      addToValueSet(leftSubcatMap, leftContext, canonicalLeftSubcat);

      Event rightContext =
	rightMS.getHistory(headEvent, rightMSLastLevel).copy();
      rightContext.canonicalize(canonicalMap);
      Subcat canonicalRightSubcat =
	headEvent.rightSubcat().getCanonical(false, canonicalMap);
      addToValueSet(rightSubcatMap, rightContext, canonicalRightSubcat);
    }

    outputSubcatMaps();

    //canonicalSubcatMap = null; // it has served its purpose
  }

  /**
   * Called by {@link #deriveCounts}.
   *
   * @param leftMS the left modifying nonterminal model structure
   * @param rightMS the right modifying nonterminal model structure
   */
  private void deriveModNonterminalMaps(ProbabilityStructure leftMS,
				        ProbabilityStructure rightMS,
                                        FlexibleMap canonicalMap) {
    System.err.println("Deriving modifying nonterminal maps.");

    int leftMSLastLevel = leftMS.numLevels() - 1;
    int rightMSLastLevel = rightMS.numLevels() - 1;

    Filter filter = nonStop;
    Iterator events = modifierEvents.keySet().iterator();
    while (events.hasNext()) {
      ModifierEvent modEvent = (ModifierEvent)events.next();
      if (!filter.pass(modEvent))
        continue;
      if (modEvent.side() == Constants.LEFT) {
        Event leftContext =
  	  leftMS.getHistory(modEvent, leftMSLastLevel).copy();
        leftContext.canonicalize(canonicalMap);
        Event leftFuture =
          leftMS.getFuture(modEvent, leftMSLastLevel).copy();
        leftFuture.canonicalize(canonicalMap);
        addToValueSet(leftModNonterminalMap, leftContext, leftFuture);
      }
      else {
        Event rightContext =
  	  rightMS.getHistory(modEvent, rightMSLastLevel).copy();
        rightContext.canonicalize(canonicalMap);
        Event rightFuture =
          rightMS.getFuture(modEvent, rightMSLastLevel).copy();
        rightFuture.canonicalize(canonicalMap);
        addToValueSet(rightModNonterminalMap, rightContext, rightFuture);
      }
    }

    outputModNonterminalMaps();
  }

  // four utility methods to output subcat and mod nonterminal maps

  /**
   * Outputs the subcat maps internal to this <code>Trainer</code> object
   * to <code>System.err</code>.
   */
  public void outputSubcatMaps() {
    outputMaps(leftSubcatMap, "left-subcat", rightSubcatMap, "right-subcat");
  }

  /**
   * Outputs the modifier maps internal to this <code>Trainer</code> object
   * to <code>System.err</code>.
   */
  public void outputModNonterminalMaps() {
    outputMaps(leftModNonterminalMap, "left-mod",
               rightModNonterminalMap, "right-mod");
  }

  /** Outputs both the specified maps to <code>System.err</code>. */
  public static void outputMaps(Map leftMap, String leftMapName,
                                Map rightMap, String rightMapName) {
    try {
      BufferedWriter systemErr =
	new BufferedWriter(new OutputStreamWriter(System.err,
						  Language.encoding()),
			   Constants.defaultFileBufsize);
      try {
	outputMaps(leftMap, leftMapName, rightMap, rightMapName, systemErr);
	systemErr.flush();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
  }

  /** Outputs both the specified maps to the specified writer. */
  public static void outputMaps(Map leftMap, String leftMapName,
                                Map rightMap, String rightMapName,
                                Writer writer)
  throws IOException {
    SymbolicCollectionWriter.writeMap(leftMap,
				      Symbol.add(leftMapName), writer);
    SymbolicCollectionWriter.writeMap(rightMap,
				      Symbol.add(rightMapName), writer);
  }



  /**
   * Runs through all headEvents and modifierEvents, collecting lexicalized
   * nonterminal occurrences.  Called by {@link #deriveCounts}.
   */
  private void derivePriors() {
    // note that since ProbabilityStructure and Model objects are all set
    // up to compute posterior probabilities, we "fake" a prior probability
    // by making all histories be identical; in this case, p_prior(w,t)
    // is really computed as p(w,t | event.parent()), where event.parent()
    // is guaranteed (by the code below) to be stopSym

    Iterator it = headEvents.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      HeadEvent event = (HeadEvent)entry.getKey();
      int count = entry.getIntValue();
      if (isRealWord(event.headWord())) {
	priorEvents.add(new HeadEvent(event.headWord(), stopSym, event.head(),
				      emptySubcat, emptySubcat),
			count);
      }
    }
    it = modifierEvents.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      ModifierEvent event = (ModifierEvent)entry.getKey();
      int count = entry.getIntValue();
      if (isRealWord(event.modHeadWord())) {
	priorEvents.add(new HeadEvent(event.modHeadWord(), stopSym,
				      event.modifier(),
				      emptySubcat, emptySubcat),
			count);
      }
    }
  }

  /** Called by {@link #deriveCounts}. */
  private ProbabilityStructure getProbStructure(String classPrefix,
						String globalStructureNumber,
						String structureNumberProperty,
						String structureClassProperty)
    throws LinkageError, ExceptionInInitializerError, ClassNotFoundException,
    InstantiationException, IllegalAccessException {
    String structureNumber = Settings.get(structureNumberProperty);
    String structureClass = Settings.get(structureClassProperty);
    String className = null;
    if (structureClass != null)
      className = structureClass;
    else if (structureNumber != null)
      className = classPrefix + structureNumber;
    else
      className = classPrefix + globalStructureNumber;
    return (ProbabilityStructure)Class.forName(className).newInstance();
  }

  // widely-used utility method
  /** Returns <code>true</code> if <code>word</code> is not <code>null</code>
      and does not have {@link Training#traceTag()} as its part of speech
      nor have {@link Training#stopSym()} as its actual word. */
  private final boolean isRealWord(Word word) {
    return (word != null &&
	    word.tag() != traceTag && word.word() != stopWord.word());
  }


  // some utility methods for adding to maps

  /**
   * Adds <code>value</code> to the set that is the vale of <code>key</code>
   * in <code>map</code>; creates this set if a mapping doesn't already
   * exist for <code>key</code>.
   *
   * @param map the map to be updated
   * @param key the key in <code>map</code> whose value set is to be updated
   * @param value the value to be added to <code>key</code>'s value set
   */
  public final static void addToValueSet(Map map,
					 Object key,
					 Object value) {
    Set valueSet = (Set)map.get(key);
    if (valueSet == null) {
      valueSet = new HashSet();
      map.put(key, valueSet);
    }
    valueSet.add(value);
  }


  /**
   * Adds <code>value</code> to the set of values to which
   * <code>key</code> is mapped (if <code>value</code> is not already in
   * that set) and increments the count of that value by 1.
   *
   * @param map the map of keys to sets of values, where each value has its
   * own count (<code>map</code> is actually a map of keys to maps of values
   * to counts)
   * @param key the key in <code>map</code> to associate with a set of values
   * with counts
   * @param value the value to add to the set of <code>key</code>'s values,
   * whose count is to be incremented by 1
   */
  public final static void addToValueCounts(Map map,
					    Object key,
					    Object value) {
    addToValueCounts(map, key, value, 1);
  }

  /**
   * Adds <code>value</code> to the set of values to which
   * <code>key</code> is mapped (if <code>value</code> is not already
   * in that set) and increments the count of that value by
   * <code>count</code>.
   *
   * @param map the map of keys to sets of values, where each value has its
   * own count (<code>map</code> is actually a map of keys to maps of values
   * to counts)
   * @param key the key in <code>map</code> to associate with a set of values
   * with counts
   * @param value the value to add to the set of <code>key</code>'s values,
   * whose count is to be incremented by <code>count</code>
   * @param count the amount by which to increment <code>value</code>'s count
   */
  public final static void addToValueCounts(Map map,
					    Object key,
					    Object value,
					    int count) {
    CountsTable valueCounts = (CountsTable)map.get(key);
    if (valueCounts == null) {
      valueCounts = new CountsTable(2);
      map.put(key, valueCounts);
    }
    valueCounts.add(value, count);
  }

  // I/O methods

  /**
   * Writes the statistics and mappings collected by
   * {@link #train(SexpTokenizer,boolean,boolean)} to a human-readable text
   * file, by constructing a <code>Writer</code> around a stream around the
   * specified file and calling {@link #writeStats(Writer)}.
   *
   * @see #train(SexpTokenizer,boolean,boolean)
   * @see #writeStats(Writer)
   */
  public void writeStats(File file) throws IOException {
    Writer writer =
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
						Language.encoding()),
			 Constants.defaultFileBufsize);
    writeStats(writer);
  }

  /**
   * Writes the statistics and mappings collected by
   * {@link #train(SexpTokenizer,boolean,boolean)} to a human-readable text
   * file.
   *
   * @see #train(SexpTokenizer,boolean,boolean)
   * @see SymbolicCollectionWriter#writeMap(Map,Symbol,Writer)
   * @see CountsTable#output(String,Writer)
   */
  public void writeStats(Writer writer) throws IOException {
    nonterminals.output(nonterminalEventSym.toString(), writer);
    headEvents.output(headEventSym.toString(), writer);
    modifierEvents.output(modEventSym.toString(), writer);
    gapEvents.output(gapEventSym.toString(), writer);
    vocabCounter.output(vocabSym.toString(), writer);
    wordFeatureCounter.output(wordFeatureSym.toString(), writer);
    SymbolicCollectionWriter.writeMap(posMap, posMapSym, writer);
  }

  /**
   * Reads the statistics and observations from an output file in the format
   * created by {@link #writeStats(Writer)}.  Observations are one of several
   * types, all recorded as S-expressions where the first element is one of the
   * following symbols:
   * <ul>
   * <li>{@link #nonterminalEventSym}
   * <li>{@link #headEventSym}
   * <li>{@link #modEventSym}
   * <li>{@link #gapEventSym}
   * <li>{@link #posMapSym}
   * <li>{@link #vocabSym}
   * <li>{@link #wordFeatureSym}
   * </ul>
   */
  public void readStats(File file)
    throws FileNotFoundException, UnsupportedEncodingException, IOException {
    readStats(new SexpTokenizer(file, Language.encoding(),
				Constants.defaultFileBufsize));
  }

  public void readStats(SexpTokenizer tok) throws IOException {

    canonicalSubcatMap = new HashMap();

    Sexp curr = null;
    for (int i = 1; (curr = Sexp.read(tok)) != null; i++) {
      if (curr.isSymbol() ||
	  (curr.isList() && curr.list().length() != 3)) {
	System.err.println(className + ": error: S-expression No. " + i +
			   " is not in the correct format:\n\t" + curr);
	continue;
      }
      SexpList event = curr.list();
      Symbol name = event.symbolAt(0);
      int count = -1;
      switch(((Integer)eventsToTypes.get(name)).intValue()) {
      case nonterminalEventType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	nonterminals.add(event.get(1), count);
	break;
      case headEventType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	headEvents.add(new HeadEvent(event.get(1)), count);
	break;
      case modEventType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	modifierEvents.add(new ModifierEvent(event.get(1)), count);
	break;
      case gapEventType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	gapEvents.add(new GapEvent(event.get(1)), count);
	break;
      case posMapType:
	posMap.put(event.get(1), event.get(2));
	break;
      case vocabType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	vocabCounter.add(event.get(1), count);
	break;
      case wordFeatureType:
	count = Integer.parseInt(event.symbolAt(2).toString());
	wordFeatureCounter.add(event.get(1), count);
	break;
      }
    }
    canonicalSubcatMap = null;
  }

  public void writeModelCollection(String objectOutputFilename,
				   String trainingInputFilename,
				   String trainingOutputFilename)
    throws FileNotFoundException, IOException {
    FileOutputStream fos = new FileOutputStream(objectOutputFilename);
    int bufSize = Constants.defaultFileBufsize;
    BufferedOutputStream bos = new BufferedOutputStream(fos, bufSize);
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    writeModelCollection(oos,
			 trainingInputFilename, trainingOutputFilename);
  }

  public void writeModelCollection(ObjectOutputStream oos,
				   String trainingInputFilename,
				   String trainingOutputFilename)
    throws IOException {
    Settings.store(oos);
    oos.writeObject(trainingInputFilename);
    oos.writeObject(trainingOutputFilename);
    oos.writeObject(modelCollection);
    oos.close();
  }

  public void setModelCollection(String objectInputFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = loadModelCollection(objectInputFilename);
  }

  public static ModelCollection loadModelCollection(String objectInputFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    FileInputStream fi = new FileInputStream(objectInputFilename);
    int bufSize = Constants.defaultFileBufsize * 10;
    BufferedInputStream bfi = new BufferedInputStream(fi, bufSize);
    ObjectInputStream ois = new ObjectInputStream(bfi);
    System.err.println("\nLoading derived counts from object file \"" +
		       objectInputFilename + "\":");
    return loadModelCollection(ois);
  }

  public void setModelCollection(ObjectInputStream ois)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = loadModelCollection(ois);
  }

  public static ModelCollection loadModelCollection(ObjectInputStream ois)
    throws ClassNotFoundException, IOException, OptionalDataException {
    scanModelCollectionObjectFile(ois, System.err);
    return (ModelCollection)ois.readObject();
  }


  public static void scanModelCollectionObjectFile(String scanObjectFilename,
					           OutputStream os)
    throws ClassNotFoundException, IOException, OptionalDataException {
    FileInputStream fis = new FileInputStream(scanObjectFilename);
    int bufSize = Constants.defaultFileBufsize;
    BufferedInputStream bis = new BufferedInputStream(fis, bufSize);
    ObjectInputStream ois = new ObjectInputStream(bis);
    System.err.println("\nInformation from object file \"" +
		       scanObjectFilename + "\":");
    scanModelCollectionObjectFile(ois, os);
  }

  public static void scanModelCollectionObjectFile(ObjectInputStream ois,
					           OutputStream os)
    throws ClassNotFoundException, IOException, OptionalDataException {
    PrintStream ps = new PrintStream(os);
    ps.println("Settings\n------------------------------");
    Properties props = (Properties)ois.readObject();
    Settings.storeSorted(props, os, " " + Settings.progName + " v" +
			 Settings.version);
    ps.println("------------------------------");
    String trainingInputFilename = (String)ois.readObject();
    if (trainingInputFilename != null)
      ps.println("training input file: \"" + trainingInputFilename + "\".");
    String trainingOutputFilename = (String)ois.readObject();
    if (trainingOutputFilename != null)
      ps.println("training output file: \"" + trainingOutputFilename + "\".");
  }


  // main method stuff

  private final static String[] usageMsg = {
    "usage: [-help] [-s <settings file>]",
    "\t[-l <input file>]",
    "\t[-scan <derived data scan file>]",
    "\t[-i <training file>] [-o <output file>]",
    "\t[-od <derived data output file>] [-ld <derived data input file>]",
    "\t[ -strip-outer-parens | -dont-strip-outer-parens | -auto ]",
    "where",
    "\t-help prints this usage message",
    "\t<settings file> is an optionally-specified settings file",
    "\t<training file> is a Treebank file containing training parse trees",
    "\t<output file> is the events output file (use \"-\" for stdout)",
    "\t<input file> is an <output file> from a previous run to load",
    "\t<derived data {scan,input,output} file> are Java object files",
    "\t\tcontaining information about and all derived counts from a",
    "\t\ttraining run",
    "\t-scan indicates to scan the first few informational objects of",
    "\t\t<derived data scan file> and print them out to stderr",
    "\t-od indicates to derive counts from observations from <training file>",
    "\t\tand output them to <derived data output file>",
    "\t-ld indicates to load derived counts from <derived data input file>",
    "\t-strip-outer-parens indicates to strip the outer parens off training",
    "\t\ttrees (appropriate for Treebank II-style annotated parse trees)",
    "\t-dont-strip-outer-parens indicates not to strip the outer parens off",
    "\t\ttraining parse trees",
    "\t-auto indicates to determine automatically whether to strip outer",
    "\t\tparens off training parse trees (default)"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(1);
  }

  /**
   * Takes arguments according to the following usage:
   * <pre>
   * usage: [-help] [-s &lt;settings file&gt;]
   * [-l &lt;input file&gt;]
   * [-scan &lt;derived data scan file&gt;]
   * [-i &lt;training file&gt;] [-o &lt;output file&gt;]
   * [-od &lt;derived data output file&gt;] [-ld &lt;derived data input file&gt;]
   * [ -strip-outer-parens | -dont-strip-outer-parens | -auto ]
   * where
   * -help prints this usage message
   * &lt;settings file&gt; is an optionally-specified settings file
   * &lt;training file&gt; is a Treebank file containing training parse trees
   * &lt;output file&gt; is the events output file (use "-" for stdout)
   * &lt;input file&gt; is an &lt;output file&gt; from a previous run to load
   * &lt;derived data {scan,input,output} file&gt; are Java object files
   *         containing information about and all derived counts from a
   *         training run
   * -scan indicates to scan the first few informational objects of
   *         &lt;derived data scan file&gt; and print them out to stderr
   * -od indicates to derive counts from observations from &lt;training file&gt;
   *         and output them to &lt;derived data output file&gt;
   * -ld indicates to load derived counts from &lt;derived data input file&gt;
   * -strip-outer-parens indicates to strip the outer parens off training
   *         trees (appropriate for Treebank II-style annotated parse trees)
   * -dont-strip-outer-parens indicates not to strip the outer parens off
   *         training parse trees
   * -auto indicates to determine automatically whether to strip outer
   *         parens off training parse trees (default)
   * </pre>
   */
  public static void main(String[] args) {
    boolean stripOuterParens = false, auto = true;
    String trainingFilename = null, outputFilename = null, inputFilename = null;
    String settingsFilename = null, objectOutputFilename = null;
    String objectInputFilename = null, scanObjectFilename = null;
    // process arguments
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-i")) {
	  if (i + 1 == args.length)
	    usage();
	  trainingFilename = args[++i];
	}
	else if (args[i].equals("-o")) {
	  if (i + 1 == args.length)
	    usage();
	  outputFilename = args[++i];
	}
	else if (args[i].equals("-od")) {
	  if (i + 1 == args.length)
	    usage();
	  objectOutputFilename = args[++i];
	}
	else if (args[i].equals("-ld")) {
	  if (i + 1 == args.length)
	    usage();
	  objectInputFilename = args[++i];
	}
	else if (args[i].equals("-scan")) {
	  if (i + 1 == args.length)
	    usage();
	  scanObjectFilename = args[++i];
	}
	else if (args[i].equals("-s")) {
	  if (i + 1 == args.length)
	    usage();
	  settingsFilename = args[++i];
	}
	else if (args[i].equals("-l")) {
	  if (i + 1 == args.length)
	    usage();
	  inputFilename = args[++i];
	}
	else if (args[i].equals("-strip-outer-parens")) {
	  stripOuterParens = true;
	  auto = false;
	}
	else if (args[i].equals("-dont-strip-outer-parens")) {
	  stripOuterParens = false;
	  auto = false;
	}
	else if (args[i].equals("-auto"))
	  auto = true;
	else if (args[i].equals("-help"))
	  usage();
	else {
	  System.err.println("unrecognized flag: " + args[i]);
	  usage();
	}
      }
    }
    if (scanObjectFilename == null && objectInputFilename == null &&
	trainingFilename == null && outputFilename == null &&
	inputFilename == null)
      usage();

    try {
      if (settingsFilename != null)
	Settings.load(settingsFilename);
    }
    catch (IOException ioe) {
      System.err.println("warning: problem loading settings file \"" +
			 settingsFilename + "\"");
    }

    String encoding = Language.encoding();

    Trainer trainer = new Trainer();

    try {
      /*
	Process proc = Runtime.getRuntime().exec("date");
	InputStream is = proc.getInputStream();
	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	String line = null;
	while ((line = reader.readLine()) != null)
	System.err.println(line);
      */
      if (scanObjectFilename != null) {
	try {
	  trainer.scanModelCollectionObjectFile(scanObjectFilename,
						System.err);
	}
	catch (ClassNotFoundException cnfe) {
	  System.err.println(cnfe);
	}
	catch (OptionalDataException ode) {
	  System.err.println(ode);
	}
      }

      Time overallTime = new Time();
      Time trainingTime = new Time();

      if (inputFilename != null) {
	Time time = new Time();
	System.err.println("Loading observations from \"" + inputFilename +
			   "\".");
	trainer.readStats(new File(inputFilename));
	System.err.println("Finished reading observations in " + time + ".");
      }

      if (trainingFilename != null) {
	System.err.println("Training from trees in \"" +
			   trainingFilename + "\".");
	Time time = new Time();
	trainer.train(new SexpTokenizer(trainingFilename, encoding,
					Constants.defaultFileBufsize),
		      auto, stripOuterParens);
	System.err.println("Observation collection completed in " + time + ".");
      }


      if (outputFilename != null) {
	OutputStream os =
	  (outputFilename.equals("-") ?
	   (OutputStream)System.out : new FileOutputStream(outputFilename));
	Writer writer = new BufferedWriter(new OutputStreamWriter(os, encoding),
					   Constants.defaultFileBufsize);
	System.err.println("Writing observations to output file \"" +
			   outputFilename + "\".");
	Time time = new Time();
	trainer.writeStats(writer);
	writer.close();
	System.err.println("Finished writing observations in " + time + ".");
      }

      if (trainingFilename != null && outputFilename != null &&
	  inputFilename != null) {
	System.err.println("Training completed in " + trainingTime + ".");
	System.err.print("Cleaning symbol table...");
	System.err.flush();
	Symbol.clean();
	System.err.println("done.");
      }

      if (objectOutputFilename != null) {
	System.err.println("Deriving counts.");
	Time time1 = new Time();
	trainer.deriveCounts();
	System.err.println("Finished deriving counts in " + time1 + ".\n" +
			   "Writing out all derived counts to object file \"" +
			   objectOutputFilename + "\".");
	Time time2 = new Time();
	trainer.writeModelCollection(objectOutputFilename,
				     trainingFilename, outputFilename);
	System.err.println("Finished outputting derived counts in " +
			   time2 + ".");
      }

      if (objectInputFilename != null) {
	try {
	  System.err.println("Loading derived counts from \"" +
			     objectInputFilename + "\".");
	  Time time = new Time();
	  trainer.setModelCollection(objectInputFilename);
	  System.err.println("Finished loading derived counts in "+ time +".");
	  System.err.print("gc ... ");
	  System.err.flush();
	  System.gc();
	  System.err.println("done");
	  Runtime runtime = Runtime.getRuntime();
	  System.err.println("Memory usage: " +
			     (runtime.totalMemory() - runtime.freeMemory())+
			     " bytes.");
	}
	catch (ClassNotFoundException cnfe) {
	  System.err.println(cnfe);
	}
	catch (OptionalDataException ode) {
	  System.err.println(ode);
	}
      }

      System.err.println("\nTotal elapsed time: " + overallTime + ".");
      System.err.println("\nHave a nice day!");
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  // unused utility method

  /**
   * Converts a map whose key-value pairs are of type Object-Set, where the
   * sets contain Sexp objects, to be key-value pairs of type Object-SexpList,
   * where the newly-created SexpList objects contain all the Sexp's that were
   * in each of the sets.
   */
  private final static void convertValueSetsToSexpLists(Map map) {
    Iterator mapIterator = map.keySet().iterator();
    while (mapIterator.hasNext()) {
      Object key = mapIterator.next();
      Set set = (Set)map.get(key);
      SexpList list = new SexpList(set.size());
      Iterator setIterator = set.iterator();
      while (setIterator.hasNext()) {
	list.add((Sexp)setIterator.next());
      }
      map.put(key, list);
    }
  }

  private void countUniqueBigrams() {
    HashSet bigramSet = new HashSet(modifierEvents.size());
    Iterator modEvents = modifierEvents.keySet().iterator();
    while (modEvents.hasNext()) {
      TrainerEvent event = (TrainerEvent)modEvents.next();
      bigramSet.add(new SymbolPair(event.headWord().word(),
				   event.modHeadWord().word()));
    }
    System.err.println("num unique bigrams " +
		       "(including traces, start and stop words): " +
		       bigramSet.size());
  }
}
