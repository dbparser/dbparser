package  danbikel.parser;

import java.util.HashMap;
import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.constraints.*;
import java.io.*;
import java.util.*;
import java.rmi.*;

/**
 * Provides the methods necessary to perform CKY parsing on input sentences.
 */
public class Decoder implements Serializable {

  // inner class for decoding timeouts
  protected static class TimeoutException extends Exception {
    TimeoutException() {
      super();
    }
    TimeoutException(String s) {
      super(s);
    }
  }

  // debugging constants
  // debugging code will be optimized away when the following booleans are false
  private final static boolean debug = false;
  private final static boolean debugConvertSubcatMaps = false;
  private final static boolean debugConvertHeadMap = false;
  private final static boolean debugPrunedPretermsPosMap = false;
  private final static boolean debugPrunedPunctuationPosMap = false;
  private final static boolean debugSentenceSize = true;
  private final static boolean debugMaxParseTime = true;
  private final static boolean debugSpans = false;
  private final static boolean debugInit = false;
  private final static boolean debugTop = false;
  private final static boolean debugComplete = false;
  private final static boolean debugJoin = false;
  private final static boolean debugStops = false;
  private final static boolean debugUnaries = false;
  private final static boolean debugUnariesAndStopProbs = false;
  private final static boolean debugConstraints = false;
  private final static boolean debugAnalyzeChart = false;
  private final static String debugGoldFilenameProperty =
    "parser.debug.goldFilename";
  private final static boolean debugAnalyzeBestDerivation = false;
  private final static String debugOutputChartProperty =
    "parser.debug.outputChart";
  private final static boolean debugOutputChart =
    Boolean.valueOf(Settings.get(debugOutputChartProperty)).booleanValue();
  private final static String debugChartFilenamePrefix = "chart";
  private final static boolean debugCommaConstraint = false;
  private final static boolean debugDontPostProcess = false;
  /**
   * This debugging option should be used only when the property
   * <tt>parser.model.precomputeProbabilities</tt> was <tt>false</tt>
   * during training (and should therefore be <tt>false</tt> during
   * decoding as well).  This is the most verbose of the debugging
   * options, so expect an output file on the order of tens of
   * megabytes, if not larger.
   */
  private final static boolean debugOutputAllCounts = false;
  private final static Symbol S = Symbol.add("S");
  private final static Symbol SA = Symbol.add("S-A");
  private final static Symbol SINV = Symbol.add("SINV");
  private final static Symbol PRN = Symbol.add("PRN");
  private final static Symbol RRB = Symbol.add("-RRB-");
  private final static Symbol NP = Symbol.add("NP");
  private final static Symbol NPB = Symbol.add("NPB");
  private final static Symbol NPA = Symbol.add("NP-A");
  private final static Symbol RRC = Symbol.add("RRC");
  private final static Symbol VP = Symbol.add("VP");
  private final static Symbol VBP = Symbol.add("VBP");
  private final static Symbol CC = Symbol.add("CC");
  private final static Symbol comma = Symbol.add(",");
  private final static Symbol FRAG = Symbol.add("FRAG");
  private final static Symbol willSym = Symbol.add("will");
  private final static Symbol mdSym = Symbol.add("MD");
  private final static Symbol PP = Symbol.add("PP");
  private final static Symbol WHADVP = Symbol.add("WHADVP");
  private final static Symbol WHNP = Symbol.add("WHNP");

  // constants
  private final static String className = Decoder.class.getName();
  protected final static boolean LEFT = Constants.LEFT;
  protected final static boolean RIGHT = Constants.RIGHT;
  // cache some constants from Constants class, for more readable code
  protected final static double logOfZero = Constants.logOfZero;
  protected final static double logProbCertain = Constants.logProbCertain;

  protected final static Subcat[] zeroSubcatArr = new Subcat[0];

  /**
   * A list containing only {@link Training#startSym()}, which is the
   * type of list that should be used when there are zero real previous
   * modifiers (to start the Markov modifier process).
   */
  protected final SexpList startList = Trainer.newStartList();
  protected final WordList startWordList = Trainer.newStartWordList();

  // data members
  /** The id of the parsing client that is using this decoder. */
  protected int id;
  /** The server for this decoder. */
  protected DecoderServerRemote server;
  /** The current sentence index for this decoder (starts at 0). */
  protected int sentenceIdx = -1;
  /** The current sentence. */
  protected SexpList sentence;
  /** The length of the current sentence, cached here for convenience. */
  protected int sentLen;
  protected int maxSentLen =
    Integer.parseInt(Settings.get(Settings.maxSentLen));
  /** The timer (used when Settings.maxParseTime is greater than zero) */
  protected final int maxParseTime =
    Integer.parseInt(Settings.get(Settings.maxParseTime));
  protected Time time = new Time();
  /** The parsing chart. */
  protected CKYChart chart;
  /** The map from vocabulary items to their possible parts of speech. */
  protected Map posMap;
  /**
   * A cache derived from {@link #posMap} that is a map of (presumably
   * closed-class) parts of speech to random example words observed with
   * the part of speech from which they are mapped.
   */
  protected Map posToExampleWordMap;
  /** The set of possible parts of speech, derived from {@link #posMap}. */
  protected Set posSet;
  /**
   * An array of all nonterminals observed in training, that is initialized
   * and filled in at construction time.
   */
  protected Symbol[] nonterminals;
  /**
   * A map from futures of the last back-off level of the head generation model
   * to possible history contexts.
   */
  protected Map headToParentMap;
  /**
   * A map from contexts of the last back-off level of the left subcat
   * generation model to possible subcats.
   */
  protected Map leftSubcatMap;
  /**
   * A map from contexts of the last back-off level of the right subcat
   * generation model to possible subcats.
   */
  protected Map rightSubcatMap;
  /** The left subcat generation model structure. */
  protected ProbabilityStructure leftSubcatPS;
  /** The last level of back-off in the left subcat generation model
      structure. */
  protected int leftSubcatPSLastLevel;
  /** The right subcat generation model structure. */
  protected ProbabilityStructure rightSubcatPS;
  /** The last level of back-off in the right subcat generation model
      structure. */
  protected int rightSubcatPSLastLevel;
  /**
   * A map from contexts of the last back-off level of the modifying
   * nonterminal generation model to possible modifying nonterminal labels.
   */
  protected Map modNonterminalMap;
  /** The modifying nonterminal generation model structure. */
  protected ProbabilityStructure modNonterminalPS;
  /** The last level of back-off in the modifying nonterminal generation
      model structure. */
  protected int modNonterminalPSLastLevel;
  // these next three data members are used by {@link #preProcess}
  protected Map prunedPretermsPosMap;
  protected Set prunedPretermsPosSet;
  protected Map prunedPunctuationPosMap;
  // these next two data members are also kept in CKYChart, but we keep
  // them here as well, for debugging purposes
  /** The cell limit for the parsing chart (stored here for debugging). */
  protected int cellLimit = -1;
  /** The prune factor for the parsing chart (stored here for debugging). */
  protected double pruneFact = 0.0;
  /** The original sentence, before preprocessing. */
  protected SexpList originalSentence = new SexpList();
  /**
   * The original sentence, but with word removed to match pre-processing.
   * This will be used to restore the original words after parsing.
   */
  protected SexpList originalWords = new SexpList();
  /** An instance of an empty subcat, for use when constructing lookup events.*/
  protected Subcat emptySubcat = Subcats.get();
  /** The boolean value of the {@link Settings#downcaseWords} setting. */
  protected boolean downcaseWords =
    Boolean.valueOf(Settings.get(Settings.downcaseWords)).booleanValue();
  /** The boolean value of the {@link Settings#useLowFreqTags} setting. */
  protected boolean useLowFreqTags =
    Boolean.valueOf(Settings.get(Settings.useLowFreqTags)).booleanValue();
  /**
   * The boolean value of the
   * {@link Settings#decoderSubstituteWordsForClosedClassTags} setting.
   */
  protected boolean substituteWordsForClosedClassTags;
  {
    String substituteWordsForClosedClassTagsStr =
      Settings.get(Settings.decoderSubstituteWordsForClosedClassTags);
    substituteWordsForClosedClassTags =
      Boolean.valueOf(substituteWordsForClosedClassTagsStr).booleanValue();
  }
  /**
   * The boolean value of the {@link Settings#decoderUseOnlySuppliedTags}
   * setting.
   */
  protected boolean useOnlySuppliedTags;
  {
    String useOnlySuppliedTagsStr =
      Settings.get(Settings.decoderUseOnlySuppliedTags);
    useOnlySuppliedTags =
      Boolean.valueOf(useOnlySuppliedTagsStr).booleanValue();
  }
  /**
   * The boolean value of the {@link Settings#decoderUseHeadToParentMap}
   * setting.
   */
  protected boolean useHeadToParentMap;
  {
    String useHeadToParentMapStr =
      Settings.get(Settings.decoderUseHeadToParentMap);
    useHeadToParentMap = Boolean.valueOf(useHeadToParentMapStr).booleanValue();
  }
  /**
   * The value of {@link Training#startSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol startSym = Language.training.startSym();
  /**
   * The value of {@link Training#startWord()}, cached here for efficiency
   * and convenience.
   */
  protected Word startWord = Language.training.startWord();
  /**
   * The value of {@link Training#stopSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol stopSym = Language.training.stopSym();
  /**
   * The value of {@link Training#stopWord()}, cached here for efficiency
   * and convenience.
   */
  protected Word stopWord = Language.training.stopWord();
  /**
   * The value of {@link Training#topSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol topSym = Language.training.topSym();
  /**
   * The value of {@link Treebank#baseNPLabel()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol baseNP = Language.treebank.baseNPLabel();
  /** The value of the setting {@link Settings#numPrevMods}. */
  protected int numPrevMods =
    Integer.parseInt(Settings.get(Settings.numPrevMods));
  /** The value of the setting {@link Settings#numPrevWords}. */
  protected int numPrevWords =
    Integer.parseInt(Settings.get(Settings.numPrevWords));
  // data members used by addUnariesAndStopProbs
  /** One of a pair of lists used by {@link #addUnariesAndStopProbs}. */
  protected List prevItemsAdded = new ArrayList();
  /** One of a pair of lists used by {@link #addUnariesAndStopProbs}. */
  protected List currItemsAdded = new ArrayList();
  // data members used by various methods that iterate over chart HashMaps
  /**
   * A temporary storage area used by {@link #addTopUnaries} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List topProbItemsToAdd = new ArrayList();
  /**
   * A temporary storage area used by {@link #addUnaries} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List unaryItemsToAdd = new ArrayList();
  /**
   * A temporary storage area used by {@link #addStopProbs} for storing
   * items to be added to the chart when iterating over a cell in the chart.
   */
  protected List stopProbItemsToAdd = new ArrayList();
  // lookup TrainerEvent objects (created once here, and constantly mutated
  // throughout decoding)
  protected PriorEvent lookupPriorEvent = new PriorEvent(null, null);
  protected HeadEvent lookupHeadEvent =
    new HeadEvent(null, null, null, emptySubcat, emptySubcat);
  protected ModifierEvent lookupModEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  protected ModifierEvent lookupLeftStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  protected ModifierEvent lookupRightStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null, null,
		      emptySubcat, false, false);
  /**
   * A lookup Word object, for obtaining a canonical version.
   */
  protected Word lookupWord = Words.get(null, null, null);
  /**
   * A reflexive map of Word objects, for getting a canonical version.
   */
  protected Map canonicalWords = new danbikel.util.HashMap();
  /**
   * A reusable set for storing <code>Word</code> objects, used when seeding
   * the chart in {@link #initialize}.
   */
  protected Set wordSet = new HashSet();
  // data member used by both getPrevMods and getPrevModWords
  protected SLNode tmpChildrenList = new SLNode(null, null);
  // data members used by getPrevMods
  protected Map canonicalPrevModLists = new danbikel.util.HashMap();
  protected SexpList prevModLookupList = new SexpList(numPrevMods);
  // data members used by getPrevModWords
  protected WordList prevModWordLeftLookupList =
    WordListFactory.newList(numPrevMods);
  protected WordList prevModWordRightLookupList =
    WordListFactory.newList(numPrevMods);
  // data members used by joinItems
  protected Subcat lookupSubcat = Subcats.get();
  // values for comma constraint-finding
  protected boolean useCommaConstraint;
  protected boolean[] commaForPruning;
  protected boolean[] conjForPruning;

  /** Cached value of {@link Settings#keepAllWords}, for efficiency and
      convenience. */
  protected boolean keepAllWords =
    Boolean.valueOf(Settings.get(Settings.keepAllWords)).booleanValue();

  /**
   * Caches the ConstraintSet, if any, for the current sentence.
   */
  protected ConstraintSet constraints = null;
  /**
   * Caches the value of {@link ConstraintSet#findAtLeastOneSatisfying()},
   * if there are constraints for the current sentence; otherwise, this
   * data member will be set to <tt>false</tt>.
   *
   * @see #constraints
   */
  protected boolean findAtLeastOneSatisfyingConstraint = false;
  /**
   * Caches whether or not the ConstraintSet for the current sentence
   * requires a tree that is isomorphic to the tree of constraints.
   * Specifically, this data member will be set to <tt>true</tt> if the
   * {@link ConstraintSet#findAtLeastOneSatisfying} and
   * {@link ConstraintSet#hasTreeStructure} methods of the current
   * sentence's constraint set both return <tt>true</tt>.
   * If there is no constraint set for the current sentence, this data
   * member is set to <tt>false</tt>.
   *
   * @see #constraints
   */
  protected boolean isomorphicTreeConstraints = false;

  // data members used when debugSentenceSize is true
  private float avgSentLen = 0.0f;
  private int numSents = 0;

  // data member to use when debugAnalyzeChart is true
  // (and when the property "parser.debug.goldFilename" has been set)
  private SexpTokenizer goldTok;

  /**
   * Constructs a new decoder that will use the specified
   * <code>DecoderServer</code> to get all information and probabilities
   * required for decoding (parsing).
   *
   * @param id the id of this parsing client
   * @param  server the <code>DecoderServerRemote</code> implementor
   * (either local or remote) that provides this decoder object with
   * information and probabilities required for decoding (parsing)
   */
  public Decoder(int id, DecoderServerRemote server) {
    this.id = id;
    this.server = server;
    String localCacheStr =
      Settings.get(Settings.decoderUseLocalProbabilityCache);
    boolean localCache = Boolean.valueOf(localCacheStr).booleanValue();
    if (localCache) {
      wrapCachingServer();
    }
    try {
      this.posMap = server.posMap();
      posSet = new HashSet();
      Iterator posVals = posMap.values().iterator();
      while (posVals.hasNext()) {
	SexpList posList = (SexpList)posVals.next();
	for (int i = 0; i < posList.length(); i++)
	  posSet.add(posList.get(i));
      }
      CountsTable nonterminalTable = server.nonterminals();
      // first, cache all nonterminals (and these are strictly nonterminals
      // and not parts of speech) into nonterminals array
      nonterminals = new Symbol[nonterminalTable.size()];
      Iterator it = nonterminalTable.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
	nonterminals[i] = (Symbol)it.next();
      // before setting up fastUidMap and fastArgMap, add pos tags to
      // nonterminal table, just in case pos tags can be args
      it = posSet.iterator();
      while (it.hasNext())
	nonterminalTable.add(it.next());
      Subcat sampleSubcat = Subcats.get();
      if (sampleSubcat instanceof SubcatBag)
	SubcatBag.setUpFastUidMap(nonterminalTable);
      if (sampleSubcat instanceof BrokenSubcatBag)
	BrokenSubcatBag.setUpFastUidMap(nonterminalTable);
      Language.training().setUpFastArgMap(nonterminalTable);
      if (useHeadToParentMap) {
	this.headToParentMap = server.headToParentMap();
	convertHeadToParentMap();
      }
      this.leftSubcatMap = server.leftSubcatMap();
      this.rightSubcatMap = server.rightSubcatMap();
      convertSubcatMaps();
      this.leftSubcatPS = server.leftSubcatProbStructure().copy();
      this.rightSubcatPS = server.rightSubcatProbStructure().copy();
      this.modNonterminalMap = server.modNonterminalMap();
      this.modNonterminalPS = server.modNonterminalProbStructure().copy();
      prunedPretermsPosMap = new danbikel.util.HashMap();
      prunedPretermsPosSet = new HashSet();
      Set prunedPreterms = server.prunedPreterms();
      it = prunedPreterms.iterator();
      while (it.hasNext()) {
	Word word = Language.treebank.makeWord((Sexp)it.next());
	prunedPretermsPosMap.put(word.word(), word.tag());
	prunedPretermsPosSet.add(word.tag());
      }
      if (debugPrunedPretermsPosMap)
	System.err.println("prunedPretermsPosMap: " + prunedPretermsPosMap);
      prunedPunctuationPosMap = new danbikel.util.HashMap();
      Set prunedPunctuation = server.prunedPunctuation();
      it = prunedPunctuation.iterator();
      while (it.hasNext()) {
	Word word = Language.treebank.makeWord((Sexp)it.next());
	prunedPunctuationPosMap.put(word.word(), word.tag());
      }
      if (debugPrunedPunctuationPosMap)
	System.err.println("prunedPunctuationPosMap: " +
			   prunedPunctuationPosMap);
    } catch (RemoteException re) {
      System.err.println(re);
    }

    leftSubcatPSLastLevel = leftSubcatPS.numLevels() - 1;
    rightSubcatPSLastLevel = rightSubcatPS.numLevels() - 1;

    modNonterminalPSLastLevel = modNonterminalPS.numLevels() - 1;

    String useCellLimitStr = Settings.get(Settings.decoderUseCellLimit);
    boolean useCellLimit = Boolean.valueOf(useCellLimitStr).booleanValue();
    if (useCellLimit)
      cellLimit = Integer.parseInt(Settings.get(Settings.decoderCellLimit));
    String usePruneFactStr = Settings.get(Settings.decoderUsePruneFactor);
    boolean usePruneFact = Boolean.valueOf(usePruneFactStr).booleanValue();
    if (usePruneFact) {
      pruneFact = Math.log(10) *
		  Double.parseDouble(Settings.get(Settings.decoderPruneFactor));
    }
    String useCommaConstraintStr =
      Settings.get(Settings.decoderUseCommaConstraint);
    useCommaConstraint = Boolean.valueOf(useCommaConstraintStr).booleanValue();

    chart = new CKYChart(cellLimit, pruneFact);

    if (!usePruneFact)
      chart.dontDoPruning();

    if (debugAnalyzeChart) {
      String goldFilename = Settings.get(debugGoldFilenameProperty);
      if (goldFilename != null) {
	try {
	  goldTok = new SexpTokenizer(goldFilename, Language.encoding(),
				      Constants.defaultFileBufsize);
	}
	catch (Exception e) {
	  throw new RuntimeException(e.toString());
	}
      }
    }
  }

  protected void wrapCachingServer() {
    server = new CachingDecoderServer(server);
  }

  protected void convertHeadToParentMap() {
    if (debugConvertHeadMap)
      System.err.print(className + ": converting head map...");
    Iterator entries = headToParentMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      Set parents = (Set)entry.getValue();
      Symbol[] newValue = new Symbol[parents.size()];
      parents.toArray(newValue);
      entry.setValue(newValue);
    }
    if (debugConvertHeadMap)
      System.err.println("done.");
  }

  /**
   * This helper method used by constructor converts the values of the subcat
   * maps from <code>Set</code> objects (containing <code>Subcat</code>
   * objects) to <code>Subcat</code> arrays, that is, objects of type
   * <code>Subcat[]</code>.  This allows possible subcats for given contexts
   * to be iterated over without the need to create <code>Iterator</code>
   * objects during decoding.
   */
  protected void convertSubcatMaps() {
    if (debugConvertSubcatMaps)
      System.err.print(className + ": converting subcat maps...");
    convertSubcatMap(leftSubcatMap);
    convertSubcatMap(rightSubcatMap);
    if (debugConvertSubcatMaps)
      System.err.println("done.");
  }

  /**
   * Helper method used by {@link #convertSubcatMaps()}.
   *
   * @param subcatMap the subcat map whose values are to be converted
   */
  protected void convertSubcatMap(Map subcatMap) {
    Iterator entries = subcatMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      Set subcats = (Set)entry.getValue();
      Subcat[] newValue = new Subcat[subcats.size()];
      subcats.toArray(newValue);
      entry.setValue(newValue);
    }
  }

  protected boolean isPuncRaiseWord(Sexp word) {
    return prunedPunctuationPosMap.containsKey(word);
  }

  /**
   * A helper method used by {@link #preProcess} that removes words from
   * the specified sentence and {@link #originalWords} lists, and also
   * from the specified tags list, if it is not <code>null</code>.
   *
   * @param sentence the sentence from which to remove a word
   * @param tags the list of tag lists that is coordinated with the specified
   * sentence from which an item is to be removed
   * @param i the index of the word to be removed
   */
  protected void removeWord(SexpList sentence, SexpList tags, int i) {
    sentence.remove(i);
    originalWords.remove(i);
    if (tags != null)
      tags.remove(i);
  }

  protected void preProcess(SexpList sentence, SexpList tags)
  throws RemoteException {
    // preserve original sentence
    originalSentence.clear();
    originalSentence.addAll(sentence);

    originalWords.clear();
    originalWords.addAll(sentence);

    // eliminate pruned words
    for (int i = sentence.length() - 1; i >= 0; i--) {
      Symbol word = (downcaseWords ?
		     Symbol.get(sentence.get(i).toString().toLowerCase()) :
		     sentence.symbolAt(i));
      Symbol tag = tags == null ? null : tags.listAt(i).first().symbol();
      if (tag != null ? prunedPretermsPosSet.contains(tag) :
			(prunedPretermsPosMap.containsKey(word) &&
			 !word.toString().equals("'"))) {
	removeWord(sentence, tags, i);
      }
    }

    SexpList convertedSentence = server.convertUnknownWords(sentence);
    // we cannot just say
    //   sentence = server.convertUnknownWords(sentence);
    // because the server might return a copy of the sentence list (this is
    // guaranteed to happen if the server is remote), and we need
    // to modify the object that was passed as an arg to this function, because
    // that's what the caller of preProcess expects; the alternative would be
    // for this method to work with the (potentially) different list object
    // returned by the server and then return that list object, but then this
    // method should probably also return the tags list as well, making things
    // difficult, since we only get to return a single object
    if (convertedSentence != sentence) {
      sentence.clear();
      sentence.addAll(convertedSentence);
    }


    // downcase words
    int sentLen = sentence.length();
    if (downcaseWords) {
      for (int i = 0; i < sentLen; i++) {
	if (sentence.get(i).isList()) // skip unknown words
	  continue;
	Symbol downcasedWord =
	  Symbol.add(sentence.symbolAt(i).toString().toLowerCase());
	sentence.set(i, downcasedWord);
      }
    }


    // remove intitial and final punctuation "words"
    for (int i = 0; i < sentence.length() - 1; i++) {
      if (sentence.get(i).isList())
	break;
      if (isPuncRaiseWord(sentence.get(i))) {
	removeWord(sentence, tags, i);
	i--;
      }
      else
	break;
    }
    for (int i = sentence.length() - 1; i > 0; i--) {
      if (sentence.get(i).isList())
	break;
      if (isPuncRaiseWord(sentence.get(i))) {
	removeWord(sentence, tags, i);
      }
      else
	break;
    }

    // finally, perform any language-specific pre-processing of the words
    // and tags
    SexpList langSpecific =
      Language.training.preProcessTest(sentence, originalWords, tags);
    sentence = langSpecific.listAt(0);
    if (tags != null)
      tags = langSpecific.listAt(1);
  }

  protected void postProcess(Sexp tree) {
    restoreOriginalWords(tree, 0);
    if (debugDontPostProcess)
      return;
    else
      Language.training.postProcess(tree);
  }

  /**
   * Restores the original words in the current sentence.
   *
   * @param tree the sentence for which to restore the original words,
   * cached during execution of {@link #preProcess}
   * @param wordIdx a threaded word index
   * @return the current value of the monotonically-increasing word index,
   * after replacing all words in the current subtree
   */
  protected int restoreOriginalWords(Sexp tree, int wordIdx) {
    Treebank treebank = Language.treebank;
    if (treebank.isPreterminal(tree))
      ;
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Word word = treebank.makeWord(currChild);
	  word.setWord(originalWords.symbolAt(wordIdx++));
	  treeList.set(i, treebank.constructPreterminal(word));
	}
	else
	  wordIdx = restoreOriginalWords(currChild, wordIdx);
      }
    }
    return wordIdx;
  }

  /**
   * Caches the locations of commas to be used for the comma constraint in the
   * boolean array {@link #commaForPruning}.  Also, sets up an array
   * (initialized to be entirely false) of booleans to cache the locations of
   * conjunctions, determined within {@link #initialize(SexpList,SexpList)}
   * (hence, the initialization of the {@link #conjForPruning} array is not
   * complete until after {@link #initialize(SexpList,SexpList)} has finished
   * executing).
   */
  protected void setCommaConstraintData() {
    if (commaForPruning == null || sentLen > commaForPruning.length)
      commaForPruning = new boolean[sentLen];
    boolean withinParens = false;
    for (int i = 0; i < sentLen; i++) {
      Symbol word = getSentenceWord(i);
      if (Language.treebank.isLeftParen(word))
	withinParens = true;
      else if (Language.treebank.isRightParen(word))
	withinParens = false;
      commaForPruning[i] = !withinParens && Language.treebank.isComma(word);
    }

    if (conjForPruning == null || sentLen > conjForPruning.length)
      conjForPruning = new boolean[sentLen];
    for (int i = 0; i < sentLen; i++)
      conjForPruning[i] = false;
  }

  /**
   * Returns a known word that was observed with the specified part of speech
   * tag.
   *
   * @param tag a part of speech tag for which an example word is to be found
   * @return a word that was observed with the specified part of speech tag.
   */
  protected Symbol getExampleWordForTag(Symbol tag) {
    // first, check cache.
    Symbol word = (Symbol)posToExampleWordMap.get(tag);
    if (word != null)
      return word;

    // run through posMap and find first known word that was observed with the
    // specified tag
    Iterator it = posMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      word = (Symbol)entry.getKey();
      SexpList tags = (SexpList)entry.getValue();
      if (tags.contains(tag)) {
	if (downcaseWords)
	  word = Symbol.get(word.toString().toLowerCase());
	posToExampleWordMap.put(tag, word);
	return word;
      }
    }
    return null;
  }

  protected SexpList getTagSet(SexpList tags, int wordIdx, Symbol word,
			       boolean wordIsUnknown, Symbol origWord,
			       HashSet tmpSet) {
    SexpList tagSet = null;

    int i = wordIdx;

    if (useOnlySuppliedTags) {
      tagSet = tags.listAt(i);
      // if word is known and has never been observed with any supplied tags,
      // issue a warning
      if (!wordIsUnknown) {
	boolean allSuppliedTagsUnobserved = true;
	SexpList observedTagSet = (SexpList)posMap.get(word);
	for (int tagIdx = 0; tagIdx < tagSet.length(); tagIdx++) {
	  if (observedTagSet.contains(tagSet.symbolAt(tagIdx))) {
	    allSuppliedTagsUnobserved = false;
	    break;
	  }
	}
	if (allSuppliedTagsUnobserved) {
	  System.err.println(className +
			     ": warning: useOnlySuppliedTags=true but known " +
			     "word \"" + word + "\" in sentence " +
			     (sentenceIdx + 1) + " has never been " +
			     "observed with any of supplied tags " + tagSet);
	  //tagSet = observedTagSet;
	}
      }
    }
    else if (wordIsUnknown) {
      if (useLowFreqTags && posMap.containsKey(origWord)) {
	tagSet = (SexpList)posMap.get(origWord);
	if (tags != null)
	  tagSet = setUnion(tagSet, tags.listAt(i), tmpSet);
      }
      else if (tags != null)
	tagSet = tags.listAt(i);
      else
	tagSet = (SexpList)posMap.get(word);
    }
    else {
      tagSet = (SexpList)posMap.get(word);
    }

    if (tagSet == null) {
      Symbol defaultFeatures = Language.wordFeatures.defaultFeatureVector();
      tagSet = (SexpList)posMap.get(defaultFeatures);
    }
    if (tagSet == null) {
      tagSet = SexpList.emptyList;
      System.err.println(className + ": warning: no tags for default " +
			 "feature vector " + word);
    }
    return tagSet;
  }

  /**
   * Adds a chart item for every possible part of speech for the specified
   * word at the specified index in the current sentence.
   *
   * @param word the current word
   * @param wordIdx the index of the current word in the current sentence
   * @param features the word-feature vector for the current word
   * @param neverObserved indicates whether the current word was never observed
   * during training (a truly unknown word)
   * @param tagSet a list containing all possible part of speech tags for
   * the current word
   * @param constraints the constraint set for this sentence
   * @throws RemoteException if any calls to the underlying
   * {@link DecoderServerRemote} object throw a <code>RemoteException</code>
   *
   * @see Chart#add(int,int,Item)
   */
  protected void seedChart(Symbol word, int wordIdx, Symbol features,
			   boolean neverObserved, SexpList tagSet,
			   boolean wordIsUnknown, Symbol origWord,
			   ConstraintSet constraints) throws RemoteException {
    int i = wordIdx;
    int numTags = tagSet.length();
    for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {
      Symbol tag = tagSet.symbolAt(tagIdx);
      if (!posSet.contains(tag))
	System.err.println(className + ": warning: part of speech tag " +
			   tag + " not seen during training");
      if (useCommaConstraint)
	if (Language.treebank.isConjunction(tag))
	  conjForPruning[i] = true;
      Word headWord = neverObserved ?
		      Words.get(word, tag, features) :
		      getCanonicalWord(lookupWord.set(word, tag, features));
      CKYItem item = chart.getNewItem();
      PriorEvent priorEvent = lookupPriorEvent;
      priorEvent.set(headWord, tag);
      double logPrior = server.logPrior(id, priorEvent);

      if (substituteWordsForClosedClassTags &&
	  numTags == 1 && wordIsUnknown && logPrior == logOfZero) {
	Symbol exampleWord = getExampleWordForTag(tag);
	headWord = getCanonicalWord(lookupWord.set(exampleWord, tag, features));
	priorEvent.set(headWord, tag);
	logPrior = server.logPrior(id, priorEvent);
      }

      double logProb = logPrior; // technically, logPrior + logProbCertain
      item.set(tag, headWord,
	       emptySubcat, emptySubcat, null, null,
	       null,
	       startList, startList,
	       i, i,
	       false, false, true,
	       Constants.logProbCertain, logPrior, logProb);

      if (findAtLeastOneSatisfyingConstraint) {
	Constraint constraint = constraints.constraintSatisfying(item);
	if (constraint != null) {
	  if (debugConstraints)
	    System.err.println("assigning satisfied constraint " +
			       constraint + " to item " + item);
	  item.setConstraint(constraint);
	}
	else {
	  if (debugConstraints)
	    System.err.println("no satisfying constraint for item " + item);
	  continue;
	}
      }
      chart.add(i, i, item);
    } // end for each tag
  }

  /**
   * Initializes the chart for parsing the specified sentence.  Specifically,
   * this method will add a chart item for each possible part of speech for
   * each word.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   */
  protected void initialize(SexpList sentence) throws RemoteException {
    initialize(sentence, null);
  }

  /**
   * Initializes the chart for parsing the specified sentence, using the
   * specified coordinated list of part-of-speech tags when assigning parts
   * of speech to unknown words.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   * @param tags a list that is the same length as <code>sentence</code> that
   * will be used when seeding the chart with the parts of speech for unknown
   * words; each element <i>i</i> of <code>tags</code> should itself be a
   * <code>SexpList</code> containing all possible parts of speech for the
   * <i>i</i><sup>th</sup> word in <code>sentence</code>; if the value of this
   * argument is <code>null</code>, then for each unknown word (or feature
   * vector), all possible parts of speech observed in the training data for
   * that unknown word will be used
   */
  protected void initialize(SexpList sentence, SexpList tags)
  throws RemoteException {

    preProcess(sentence, tags);

    if (debugInit) {
      System.err.println(className + ": sentence to parse: " + sentence);
    }

    this.sentence = sentence;
    sentLen = sentence.length();

    if (useCommaConstraint)
      setCommaConstraintData();

    HashSet tmpSet = new HashSet();

    for (int i = 0; i < sentLen; i++) {
      boolean wordIsUnknown = sentence.get(i).isList();
      boolean neverObserved = false;
      Symbol word = null, features = null;
      if (wordIsUnknown) {
	SexpList wordInfo = sentence.listAt(i);
	neverObserved = wordInfo.symbolAt(2) == Constants.trueSym;
	if (keepAllWords) {
	  features = wordInfo.symbolAt(1);
	  word = neverObserved ? features : wordInfo.symbolAt(0);
	}
	else {
	  // we *don't* set features, since when keepAllWords is false,
	  // we simply replace unknown words with their word feature vectors
	  word = wordInfo.symbolAt(1);
	}
      }
      else {
	// word is a known word, so just grab it
	word = sentence.symbolAt(i);
      }

      Symbol origWord = (wordIsUnknown ?
			 sentence.listAt(i).symbolAt(0) : sentence.symbolAt(i));

      SexpList tagSet =
	  getTagSet(tags, i, word, wordIsUnknown, origWord, tmpSet);

      seedChart(word, i, features, neverObserved, tagSet,
		wordIsUnknown, origWord, constraints);

      addUnariesAndStopProbs(i, i);
    } // end for each word index
  }

  protected Word getCanonicalWord(Word lookup) {
    Word canonical = (Word)canonicalWords.get(lookup);
    if (canonical == null) {
      canonical = lookup.copy();
      canonicalWords.put(canonical, canonical);
    }
    return canonical;
  }

  protected SexpList setUnion(SexpList l1, SexpList l2, Set tmpSet) {
    tmpSet.clear();
    for (int i = 0; i < l1.length(); i++)
      tmpSet.add(l1.get(i));
    for (int i = 0; i < l2.length(); i++)
      tmpSet.add(l2.get(i));
    SexpList union = new SexpList(tmpSet.size());
    Iterator it = tmpSet.iterator();
    while (it.hasNext())
      union.add((Sexp)it.next());
    return union;
  }

  protected Sexp parse(SexpList sentence) throws RemoteException {
    return parse(sentence, null);
  }

  protected Sexp parse(SexpList sentence, SexpList tags)
    throws RemoteException {
    return parse(sentence, tags, null);
  }

  protected Sexp parse(SexpList sentence, SexpList tags,
		       ConstraintSet constraints)
    throws RemoteException {

    if (debugOutputAllCounts)
      Debug.level = 21;

    sentenceIdx++;
    time.reset();
    if (maxSentLen > 0 && sentence.length() > maxSentLen) {
      if (debugSentenceSize)
	System.err.println(className + ": current sentence length " +
			   sentence.length() + " is greater than max. (" +
			   maxSentLen + ")");
      return null;
    }

    if (constraints == null) {
      findAtLeastOneSatisfyingConstraint = isomorphicTreeConstraints = false;
    }
    else {
      this.constraints = constraints;
      findAtLeastOneSatisfyingConstraint =
	constraints.findAtLeastOneSatisfying();
      isomorphicTreeConstraints =
	findAtLeastOneSatisfyingConstraint && constraints.hasTreeStructure();
      if (debugConstraints)
	System.err.println(className + ": constraints: " + constraints);
    }

    chart.setSizeAndClear(sentence.length());
    initialize(sentence, tags);

    if (debugSentenceSize) {
      System.err.println(className + ": current sentence length: " + sentLen +
			 " word" + (sentLen == 1 ? "" : "s"));
      numSents++;
      avgSentLen = ((numSents - 1)/(float)numSents) * avgSentLen +
		   (float)sentLen / numSents;
      System.err.println(className + ": cummulative average length: " +
			 avgSentLen + " words");
    }

    if (sentLen == 0) {         // preprocessing could have removed all words!
      chart.postParseCleanup(); // get rid of seed items from initialize()
      sentence.clear();
      sentence.addAll(originalSentence); // restore original sentence
      return null;
    }

    try {
      for (int span = 2; span <= sentLen; span++) {
	if (debugSpans)
	  System.err.println(className + ": span: " + span);
	int split = sentLen - span + 1;
	for (int start = 0; start < split; start++) {
	  int end = start + span - 1;
	  if (debugSpans)
	    System.err.println(className + ": start: " + start +
			       "; end: " + end);
	  complete(start, end);
	}
      }
    }
    catch (TimeoutException te) {
      if (debugMaxParseTime) {
	System.err.println(te.getMessage());
      }
    }
    double prevTopLogProb = chart.getTopLogProb(0, sentLen - 1);
    if (debugTop)
      System.err.println(className + ": highest probability item for " +
			 "sentence-length span (0," + (sentLen - 1) + "): " +
			 prevTopLogProb);
    chart.resetTopLogProb(0, sentLen - 1);
    addTopUnaries(sentLen - 1);

    // the chart mixes two types of items that cover the entire span
    // of the sentnece: those that have had their +TOP+ probability multiplied
    // in (with topSym as their label) and those that have not; if the
    // top-ranked item also has topSym as its label, we're done; otherwise,
    // we look through all items that cover the entire sentence and get
    // the highest-ranked item whose label is topSym (NO WE DO NOT, since
    // we reset the top-ranked item just before adding top unaries)
    CKYItem topRankedItem = null;
    CKYItem potentialTopItem = (CKYItem)chart.getTopItem(0, sentLen - 1);
    if (potentialTopItem != null && potentialTopItem.label() == topSym)
      topRankedItem = potentialTopItem;

    if (debugTop)
      System.err.println(className + ": top-ranked +TOP+ item: " +
			 topRankedItem);


    if (debugConstraints) {
      Iterator it = constraints.iterator();
      while (it.hasNext()) {
	Constraint c = (Constraint)it.next();
	System.err.println(className + ": constraint " + c + " has" +
			   (c.hasBeenSatisfied() ? " " : " NOT ") +
			   "been satisfied");
      }
    }

    /*
    if (topRankedItem == null) {
     double highestProb = logOfZero;
     Iterator it = chart.get(0, sentLen - 1);
     while (it.hasNext()) {
       CKYItem item = (CKYItem)it.next();
       if (item.label() != topSym)
	 continue;
       if (item.logProb() > highestProb) {
	 topRankedItem = item;
	 highestProb = item.logProb();
       }
     }
    }
    */

    if (debugAnalyzeChart) {
      Sexp goldTree = null;
      try {
	goldTree = Sexp.read(goldTok);
	if (goldTree != null) {
	  String prefix = "chart-debug (" + sentenceIdx + "): ";
	  danbikel.parser.util.DebugChart.findConstituents(prefix,
							   downcaseWords,
							   chart, topRankedItem,
							   sentence,
							   goldTree);
	}
	else
	  System.err.println(className + ": couldn't read gold parse tree " +
			     "for chart analysis of sentence " + sentenceIdx);
      }
      catch (IOException ioe) {
	System.err.println(className + ": couldn't read gold parse tree " +
			   "for chart analysis of sentence " + sentenceIdx);
      }
    }

    if (debugAnalyzeBestDerivation) {
      String prefix = "derivation-debug for sent. " + sentenceIdx + " (len=" +
	sentLen + "): ";
      danbikel.parser.util.DebugChart.printBestDerivationStats(prefix,
							       chart,
							       sentLen,
							       topSym,
							       prevTopLogProb,
							       topRankedItem);
    }

    if (debugOutputChart) {
      try {
	String chartFilename =
	  debugChartFilenamePrefix + "-" + id + "-" + sentenceIdx + ".obj";
	System.err.println(className +
			   ": outputting chart to Java object file " +
			   "\"" + chartFilename + "\"");
	BufferedOutputStream bos =
	  new BufferedOutputStream(new FileOutputStream(chartFilename),
				   Constants.defaultFileBufsize);
	ObjectOutputStream os = new ObjectOutputStream(bos);
	os.writeObject(chart);
	os.writeObject(topRankedItem);
	os.writeObject(sentence);
	os.writeObject(originalWords);
	os.close();
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }

    chart.postParseCleanup();

    if (topRankedItem == null) {
      sentence.clear();
      sentence.addAll(originalSentence); // restore original sentence
      return null;
    }
    else {
      Sexp tree = topRankedItem.headChild().toSexp();
      postProcess(tree);
      return tree;
    }
  }

  protected void addTopUnaries(int end) throws RemoteException {
    topProbItemsToAdd.clear();
    Iterator sentSpanItems = chart.get(0, end);
    while (sentSpanItems.hasNext()) {
      CKYItem item = (CKYItem)sentSpanItems.next();
      if (item.stop()) {

	HeadEvent headEvent = lookupHeadEvent;
	headEvent.set(item.headWord(), topSym, (Symbol)item.label(),
		      emptySubcat, emptySubcat);
	double topLogProb = server.logProbTop(id, headEvent);
	double logProb = item.logTreeProb() + topLogProb;

	if (debugTop)
	  System.err.println(className +
			     ": item=" + item + "; topLogProb=" + topLogProb +
			     "; item.logTreeProb()=" + item.logTreeProb() +
			     "; logProb=" + logProb);

	if (topLogProb <= logOfZero)
	  continue;
	CKYItem newItem = chart.getNewItem();
	newItem.set(topSym, item.headWord(),
		    emptySubcat, emptySubcat, item,
		    null, null, startList, startList, 0, end,
		    false, false, true, logProb, Constants.logProbCertain,
		    logProb);
	topProbItemsToAdd.add(newItem);
      }
    }
    Iterator toAdd = topProbItemsToAdd.iterator();
    while (toAdd.hasNext())
      chart.add(0, end, (CKYItem)toAdd.next());
  }

  protected void complete(int start, int end)
    throws RemoteException, TimeoutException {
    for (int split = start; split < end; split++) {

      if (maxParseTime > 0 && time.elapsedMillis() > maxParseTime) {
	throw new TimeoutException(className + ": ran out of time (>" +
				   maxParseTime + "ms) on sentence " +
				   sentenceIdx);
      }

      if (useCommaConstraint && commaConstraintViolation(start, split, end)) {
	if (debugCommaConstraint) {
	  System.err.println(className +
			     ": constraint violation at (start,split,end+1)=(" +
			     start + "," + split + "," + (end + 1) +
			     "); word at end+1 = " + getSentenceWord(end + 1));
	}
	// EVEN IF there is a constraint violation, we still try to find
	// modificands that have not yet received their stop probabilities
	// whose labels are baseNP, to see if we can add a premodifier
	// (so that we can build baseNPs to the left even if they contain
	// commas)
	// TECHNICALLY, we should really try to build constituents on both
	// the LEFT *and* RIGHT, but since base NPs are typically right-headed,
	// this hack works well (at least, in English and Chinese), but it is
	// a hack nonetheless
	boolean modifierSide = Constants.LEFT;
	int modificandStartIdx =  split + 1;
	int modificandEndIdx =    end;
	int modifierStartIdx  =   start;
	int modifierEndIdx =      split;
	if (debugComplete && debugSpans)
	  System.err.println(className + ": modifying [" +
			     modificandStartIdx + "," + modificandEndIdx +
			     "]" + " with [" + modifierStartIdx + "," +
			     modifierEndIdx + "]");

	// for each possible modifier that HAS received its stop probabilities,
	// try to find a modificand that has NOT received its stop probabilities
	if (chart.numItems(modifierStartIdx, modifierEndIdx) > 0 &&
	    chart.numItems(modificandStartIdx, modificandEndIdx) > 0) {
	  Iterator modifierItems = chart.get(modifierStartIdx, modifierEndIdx);
	  while (modifierItems.hasNext()) {
	    CKYItem modifierItem = (CKYItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		CKYItem modificandItem = (CKYItem)modificandItems.next();
		if (!modificandItem.stop() && modificandItem.label()==baseNP) {
		  if (debugComplete)
		    System.err.println(className +
				       ".complete: trying to modify\n\t" +
				       modificandItem + "\n\twith\n\t" +
				       modifierItem);
		  joinItems(modificandItem, modifierItem, modifierSide);
		}
	      }
	    }
	  }
	}
	continue;
      }

      boolean modifierSide;
      for (int sideIdx = 0; sideIdx < 2; sideIdx++) {
	modifierSide = sideIdx == 0 ? Constants.RIGHT : Constants.LEFT;
	boolean modifyLeft = modifierSide == Constants.LEFT;

	int modificandStartIdx = modifyLeft ?  split + 1  :  start;
	int modificandEndIdx =   modifyLeft ?  end        :  split;

	int modifierStartIdx =   modifyLeft ?  start      :  split + 1;
	int modifierEndIdx =     modifyLeft ?  split      :  end;

	if (debugComplete && debugSpans)
	  System.err.println(className + ": modifying [" +
			     modificandStartIdx + "," + modificandEndIdx +
			     "]" + " with [" + modifierStartIdx + "," +
			     modifierEndIdx + "]");

	// for each possible modifier that HAS received its stop probabilities,
	// try to find a modificand that has NOT received its stop probabilities
	if (chart.numItems(modifierStartIdx, modifierEndIdx) > 0 &&
	    chart.numItems(modificandStartIdx, modificandEndIdx) > 0) {
	  Iterator modifierItems = chart.get(modifierStartIdx, modifierEndIdx);
	  while (modifierItems.hasNext()) {
	    CKYItem modifierItem = (CKYItem)modifierItems.next();
	    if (modifierItem.stop()) {
	      Iterator modificandItems =
		chart.get(modificandStartIdx, modificandEndIdx);
	      while (modificandItems.hasNext()) {
		CKYItem modificandItem = (CKYItem)modificandItems.next();
		if (!modificandItem.stop() &&
		    derivationOrderOK(modificandItem, modifierSide)) {
		/*
		if (!modificandItem.stop()) {
		*/
		  if (debugComplete)
		    System.err.println(className +
				       ".complete: trying to modify\n\t" +
				       modificandItem + "\n\twith\n\t" +
				       modifierItem);
		  joinItems(modificandItem, modifierItem, modifierSide);
		}
	      }
	    }
	  }
	}
      }
    }
    addUnariesAndStopProbs(start, end);
    chart.prune(start, end);
  }

  /**
   * Enforces that modificand receives all its right modifiers before receiving
   * any left modifiers, by ensuring that right-modification only happens
   * when a modificand has no left-children (this is both necessary and
   * sufficient to enforce derivation order).  Also, in the case of
   * left-modification, this method checks to make sure that the right subcat
   * is empty (necessary but <i>not</i> sufficient to enforce derivation order).
   * This method is called by {@link #complete(int,int)}.
   */
  protected boolean derivationOrderOK(CKYItem modificand, boolean modifySide) {
    return (modifySide == Constants.LEFT ?
	    modificand.rightSubcat().empty() :
	    modificand.leftChildren() == null);
  }

  /**
   * Joins two chart items, one representing the modificand that has not
   * yet received its stop probabilities, the other representing the modifier
   * that has received its stop probabilities.
   *
   * @param modificand the chart item representing a partially-completed
   * subtree, to be modified on <code>side</code> by <code>modifier</code>
   * @param modifier the chart item representing a completed subtree that
   * will be added as a modifier on <code>side</code> of
   * <code>modificand</code>'s subtree
   * @param side the side on which to attempt to add the specified modifier
   * to the specified modificand
   */
  protected void joinItems(CKYItem modificand, CKYItem modifier,
			   boolean side)
  throws RemoteException {
    Symbol modLabel = (Symbol)modifier.label();

    Subcat thisSideSubcat = (Subcat)modificand.subcat(side);
    Subcat oppositeSideSubcat = modificand.subcat(!side);
    boolean thisSideSubcatContainsMod = thisSideSubcat.contains(modLabel);
    if (!thisSideSubcatContainsMod &&
	Language.training.isArgumentFast(modLabel))
      return;

    if (isomorphicTreeConstraints) {
      if (modificand.getConstraint().isViolatedByChild(modifier)) {
	if (debugConstraints)
	  System.err.println("constraint " + modificand.getConstraint() +
			     " violated by child item(" +
			    modifier.start() + "," + modifier.end() + "): " +
			    modifier);
	return;
      }
    }

    /*
    SexpList thisSidePrevMods = getPrevMods(modificand,
					    modificand.prevMods(side),
					    modificand.children(side));
    */
    /*
    SexpList thisSidePrevMods = modificand.prevMods(side);
    */
    tmpChildrenList.set(modifier, modificand.children(side));

    SexpList thisSidePrevMods = getPrevMods(modificand, tmpChildrenList);
    SexpList oppositeSidePrevMods = modificand.prevMods(!side);

    WordList previousWords = getPrevModWords(modificand, tmpChildrenList, side);

    int thisSideEdgeIndex = modifier.edgeIndex(side);
    int oppositeSideEdgeIndex = modificand.edgeIndex(!side);

    boolean thisSideContainsVerb =
      modificand.verb(side) || modifier.containsVerb();
    boolean oppositeSideContainsVerb = modificand.verb(!side);

    ModifierEvent modEvent = lookupModEvent;
    modEvent.set(modifier.headWord(),
		 modificand.headWord(),
		 modLabel,
		 thisSidePrevMods,
		 previousWords,
		 (Symbol)modificand.label(),
		 modificand.headLabel(),
		 modificand.subcat(side),
		 modificand.verb(side),
		 side);

    boolean debugFlag = false;
    if (debugJoin) {
      Symbol modificandLabel = (Symbol)modificand.label();
      boolean modificandLabelP = modificandLabel == S;
      boolean modLabelP = modLabel == CC;
      debugFlag = (modificandLabelP && side == Constants.LEFT &&
		   modificand.start() <= 35 && modificand.end() == 38);
      /*
      if (debugFlag)
	Debug.level = 21;
      */
    }

    if (!futurePossible(modEvent, side, debugFlag))
      return;

    if (debugJoin) {
    }

    int lowerIndex = Math.min(thisSideEdgeIndex, oppositeSideEdgeIndex);
    int higherIndex = Math.max(thisSideEdgeIndex, oppositeSideEdgeIndex);

    double logModProb = server.logProbMod(id, modEvent);

    if (logModProb <= logOfZero) {
      if (debugFlag) {
	System.err.println(className +
			   ".join: couldn't join because logProbMod=logOfZero");
      }
      Debug.level = 0;
      return;
    }
    double logTreeProb =
      modificand.logTreeProb() + modifier.logTreeProb() + logModProb;

    double logPrior = modificand.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugJoin) {
      if (debugFlag) {
	System.err.println(className + ".join: trying to extend modificand\n" +
			   modificand + "\nwith modifier\n" + modifier);
	System.err.println("where logModProb=" + logModProb);
      }
    }

    // if this side's subcat contains the the current modifier's label as one
    // of its requirements, make a copy of it and remove the requirement
    if (thisSideSubcatContainsMod) {
      thisSideSubcat = (Subcat)thisSideSubcat.copy();
      thisSideSubcat.remove(modLabel);
    }

    SLNode thisSideChildren = new SLNode(modifier, modificand.children(side));
    SLNode oppositeSideChildren = modificand.children(!side);

    CKYItem newItem = chart.getNewItem();
    newItem.set((Symbol)modificand.label(), modificand.headWord(),
		null, null, modificand.headChild(), null, null, null, null,
		lowerIndex, higherIndex, false, false, false,
		logTreeProb, logPrior, logProb);

    tmpChildrenList.set(null, thisSideChildren);
    SexpList thisSideNewPrevMods = getPrevMods(modificand, tmpChildrenList);

    newItem.setSideInfo(side,
			thisSideSubcat, thisSideChildren,
			thisSideNewPrevMods, thisSideEdgeIndex,
			thisSideContainsVerb);
    newItem.setSideInfo(!side,
			oppositeSideSubcat, oppositeSideChildren,
			oppositeSidePrevMods, oppositeSideEdgeIndex,
			oppositeSideContainsVerb);

    if (isomorphicTreeConstraints) {
      if (debugConstraints)
	System.err.println("assigning partially-satisfied constraint " +
			   modificand.getConstraint() + " to " + newItem);
      newItem.setConstraint(modificand.getConstraint());
    }

    boolean added = chart.add(lowerIndex, higherIndex, newItem);
    if (!added) {
      chart.reclaimItem(newItem);
      if (debugFlag)
	System.err.println(className + ".join: couldn't add item");
    }

    if (debugJoin) {
      Debug.level = 0;
    }
  }

  private boolean futurePossible(ModifierEvent modEvent, boolean side,
				 boolean debug) {
    ProbabilityStructure modPS = modNonterminalPS;
    int lastLevel = modNonterminalPSLastLevel;
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modNonterminalMap.get(historyContext);
    if (possibleFutures != null) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures.contains(currentFuture)) {
	/*
	if (debug)
	  System.err.println(className + ".futurePossible: future " +
			     currentFuture + " FOUND for history context " +
			     historyContext);
	*/
	return true;
      }
    }
    else {
      //no possible futures for history context
    }

    if (debug) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures == null)
	System.err.println(className + ".futurePossible: history context " +
			   historyContext + " not seen in training");
      else if (!possibleFutures.contains(currentFuture))
	System.err.println(className + ".futurePossible: future " +
			   currentFuture + " not found for history context " +
			   historyContext);
    }

    return false;
  }

  private Set possibleFutures(ModifierEvent modEvent, boolean side) {
    ProbabilityStructure modPS = modNonterminalPS;
    int lastLevel = modNonterminalPSLastLevel;
    boolean onLeft = side == Constants.LEFT;
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modNonterminalMap.get(historyContext);
    return possibleFutures;
  }

  protected void addUnariesAndStopProbs(int start, int end)
  throws RemoteException {
    prevItemsAdded.clear();
    currItemsAdded.clear();
    stopProbItemsToAdd.clear();

    Iterator it = chart.get(start, end);
    while (it.hasNext()) {
      CKYItem item = (CKYItem)it.next();
      if (item.stop() == false)
	stopProbItemsToAdd.add(item);
      else if (item.isPreterminal())
	prevItemsAdded.add(item);
    }

    if (stopProbItemsToAdd.size() > 0) {
      it = stopProbItemsToAdd.iterator();
      while (it.hasNext())
	addStopProbs((CKYItem)it.next(), prevItemsAdded);
    }

    int i = -1;
    //for (i = 0; i < 5 && prevItemsAdded.size() > 0; i++) {
    for (i = 0; prevItemsAdded.size() > 0; i++) {
      Iterator prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	CKYItem item = (CKYItem)prevItems.next();
	if (!item.garbage())
	  addUnaries(item, currItemsAdded);
      }

      exchangePrevAndCurrItems();
      currItemsAdded.clear();

      prevItems = prevItemsAdded.iterator();
      while (prevItems.hasNext()) {
	CKYItem item = (CKYItem)prevItems.next();
	if (!item.garbage())
	  addStopProbs(item, currItemsAdded);
      }
      exchangePrevAndCurrItems();
      currItemsAdded.clear();
    }
    if (debugUnariesAndStopProbs) {
      System.err.println(className +
			 ": added unaries and stop probs " + i + " times");
    }
  }

  private final void exchangePrevAndCurrItems() {
    List exchange;
    exchange = prevItemsAdded;
    prevItemsAdded = currItemsAdded;
    currItemsAdded = exchange;
  }

  protected List addUnaries(CKYItem item, List itemsAdded)
  throws RemoteException {
    // get possible parent nonterminals
    Symbol[] nts;
    if (useHeadToParentMap) {
      nts = (Symbol[])headToParentMap.get(item.label());
      // this item's root label was ONLY seen as a modifier
      if (nts == null)
	return itemsAdded;
    }
    else
      nts = nonterminals;

    unaryItemsToAdd.clear();
    CKYItem newItem = chart.getNewItem();
    // set some values now, most to be filled in by code below
    newItem.set(null, item.headWord(), null, null, item,
		null, null, startList, startList,
		item.start(), item.end(),
		false, false, false, 0.0, 0.0, 0.0);
    Symbol headSym = (Symbol)item.label();
    HeadEvent headEvent = lookupHeadEvent;
    headEvent.set(item.headWord(), null, headSym, emptySubcat, emptySubcat);
    PriorEvent priorEvent = lookupPriorEvent;
    priorEvent.set(item.headWord(), null);
    int numNTs = nts.length;
    // foreach possible parent nonterminal
    for (int ntIndex = 0; ntIndex < numNTs; ntIndex++) {
      Symbol parent = nts[ntIndex];
      headEvent.setParent(parent);
      Subcat[] leftSubcats = getPossibleSubcats(leftSubcatMap, headEvent,
						leftSubcatPS,
						leftSubcatPSLastLevel);
      Subcat[] rightSubcats = getPossibleSubcats(rightSubcatMap, headEvent,
						 rightSubcatPS,
						 rightSubcatPSLastLevel);
      if (debugUnaries) {
	if (item.start() == 13 && item.end() == 20 &&
	    headSym == VP && parent == SA) {
	  System.err.println(className + ".addUnaries: trying to build on " +
			     headSym + " with " + parent);
	}
      }

      int numLeftSubcats = leftSubcats.length;
      int numRightSubcats = rightSubcats.length;
      if (numLeftSubcats > 0 && numRightSubcats > 0) {
	// foreach possible right subcat
	for (int rightIdx = 0; rightIdx < numRightSubcats; rightIdx++) {
	  Subcat rightSubcat = (Subcat)rightSubcats[rightIdx];
	  // foreach possible left subcat
	  for (int leftIdx = 0; leftIdx < numLeftSubcats; leftIdx++) {
	    Subcat leftSubcat = (Subcat)leftSubcats[leftIdx];

	    newItem.setLabel(parent);
	    newItem.setLeftSubcat(leftSubcat);
	    newItem.setRightSubcat(rightSubcat);

	    headEvent.setLeftSubcat(leftSubcat);
	    headEvent.setRightSubcat(rightSubcat);

	    if (debugUnaries) {
	      if (item.start() == 13 && item.end() == 20 &&
		  headSym == VP && parent == SA) {
		System.err.println(className + ".addUnaries: trying to " +
				   "build on " + headSym + " with " + parent +
				   " and left subcat " + leftSubcat +
				   " and right subcat " + rightSubcat);
	      }
	    }

	    if (isomorphicTreeConstraints) {
	      // get head child's constraint's parent and check that it is
	      // locally satisfied by newItem
	      if (item.getConstraint() == null) {
		System.err.println("uh-oh: no constraint for item " + item);
	      }
	      Constraint headChildParent = item.getConstraint().getParent();
	      if (headChildParent != null &&
		  headChildParent.isLocallySatisfiedBy(newItem)) {
		if (debugConstraints)
		  System.err.println("assigning locally-satisfied constraint " +
				     headChildParent + " to " + newItem);
		newItem.setConstraint(headChildParent);
	      }
	      else {
		if (debugConstraints)
		  System.err.println("constraint " + headChildParent +
				     " is not locally satisfied by item " +
				     newItem);
		continue;
	      }
	    }
	    else if (findAtLeastOneSatisfyingConstraint) {
	      Constraint constraint = constraints.constraintSatisfying(newItem);
	      if (constraint == null)
		continue;
	      else
		newItem.setConstraint(constraint);
	    }

	    double logProbLeftSubcat =
	      (numLeftSubcats == 1 ? logProbCertain :
	       server.logProbLeftSubcat(id, headEvent));
	    double logProbRightSubcat =
	      (numRightSubcats == 1 ? logProbCertain :
	       server.logProbRightSubcat(id, headEvent));
	    double logProbHead = server.logProbHead(id, headEvent);
	    if (logProbHead <= logOfZero)
	      continue;
	    double logTreeProb =
	      item.logTreeProb() +
	      logProbHead + logProbLeftSubcat + logProbRightSubcat;

	    priorEvent.setLabel(parent);
	    double logPrior = server.logPrior(id, priorEvent);

	    if (logPrior <= logOfZero)
	      continue;

	    double logProb = logTreeProb + logPrior;

	    if (debugUnaries) {
	      if (item.start() == 13 && item.end() == 20 &&
		  headSym == VP && parent == SA &&
		  leftSubcat.size() == 0 && rightSubcat.size() == 0) {
		String msg =
		  className + ".addUnaries: logprobs: lc=" +
		  logProbLeftSubcat + "; rc=" +
		  logProbRightSubcat + "; head=" + logProbHead +
		  "; tree=" + logTreeProb + "; prior=" + logPrior;
		System.err.println(msg);
	      }
	    }

	    if (logProb <= logOfZero)
	      continue;

	    newItem.setLogTreeProb(logTreeProb);
	    newItem.setLogPrior(logPrior);
	    newItem.setLogProb(logProb);

	    CKYItem newItemCopy = chart.getNewItem();
	    newItemCopy.setDataFrom(newItem);
	    unaryItemsToAdd.add(newItemCopy);
	  }
	} // end foreach possible left subcat
      }
    }
    if (unaryItemsToAdd.size() > 0) {
      Iterator toAdd = unaryItemsToAdd.iterator();
      while (toAdd.hasNext()) {
	CKYItem itemToAdd = (CKYItem)toAdd.next();
	boolean added = chart.add(itemToAdd.start(), itemToAdd.end(), itemToAdd);
	if (added)
	  itemsAdded.add(itemToAdd);
	else
	  chart.reclaimItem(itemToAdd);
      }
    }
    chart.reclaimItem(newItem);

    return itemsAdded;
  }

  protected final Subcat[] getPossibleSubcats(Map subcatMap, HeadEvent headEvent,
					    ProbabilityStructure subcatPS,
					    int lastLevel) {
    Event lastLevelHist = subcatPS.getHistory(headEvent, lastLevel);
    Subcat[] subcats = (Subcat[])subcatMap.get(lastLevelHist);
    return subcats == null ? zeroSubcatArr : subcats;
  }

  protected List addStopProbs(CKYItem item, List itemsAdded)
    throws RemoteException {
    if (!(item.leftSubcat().empty() && item.rightSubcat().empty()))
      return itemsAdded;

    /*
    SexpList leftPrevMods =
      getPrevMods(item, item.leftPrevMods(), item.leftChildren());
    SexpList rightPrevMods =
      getPrevMods(item, item.rightPrevMods(), item.rightChildren());
    */

    // technically, we should getPrevMods for both lists here, but there
    // shouldn't be skipping of previous mods because of generation of stopSym
    SexpList leftPrevMods = item.leftPrevMods();
    SexpList rightPrevMods = item.rightPrevMods();

    tmpChildrenList.set(null, item.leftChildren());
    WordList leftPrevWords = getPrevModWords(item, tmpChildrenList,
					     Constants.LEFT);
    tmpChildrenList.set(null, item.rightChildren());
    WordList rightPrevWords = getPrevModWords(item, tmpChildrenList,
					      Constants.RIGHT);

    ModifierEvent leftMod = lookupLeftStopEvent;
    leftMod.set(stopWord, item.headWord(), stopSym, leftPrevMods,
		leftPrevWords,
		(Symbol)item.label(), item.headLabel(), item.leftSubcat(),
		item.leftVerb(), Constants.LEFT);
    ModifierEvent rightMod = lookupRightStopEvent;
    rightMod.set(stopWord, item.headWord(), stopSym, rightPrevMods,
		 rightPrevWords,
		 (Symbol)item.label(), item.headLabel(),
		 item.rightSubcat(), item.rightVerb(), Constants.RIGHT);

    if (debugStops) {
      if (item.start() == 13 && item.end() == 20 && item.label() == SA) {
	System.err.println(className + ".addStopProbs: trying to add stop " +
			   "probs to item " + item);
      }
    }

    if (isomorphicTreeConstraints) {
      if (!item.getConstraint().isSatisfiedBy(item)) {
	if (debugConstraints)
	  System.err.println("constraint " + item.getConstraint() +
			     " is not satisfied by item " + item);
	return itemsAdded;
      }
    }

    double leftLogProb = server.logProbMod(id, leftMod);
    if (leftLogProb <= logOfZero)
      return itemsAdded;
    double rightLogProb = server.logProbMod(id, rightMod);
    if (rightLogProb <= logOfZero)
      return itemsAdded;
    double logTreeProb =
      item.logTreeProb() + leftLogProb + rightLogProb;

    double logPrior = item.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugStops) {
      if (item.start() == 13 && item.end() == 20 && item.label() == SA) {
	System.err.println(className + ".addStopProbs: adding stops to item " +
			   item);
      }
    }

    if (logProb <= logOfZero)
      return itemsAdded;

    CKYItem newItem = chart.getNewItem();
    newItem.set((Symbol)item.label(), item.headWord(),
		item.leftSubcat(), item.rightSubcat(),
		item.headChild(),
		item.leftChildren(), item.rightChildren(),
		item.leftPrevMods(), item.rightPrevMods(),
		item.start(), item.end(), item.leftVerb(),
		item.rightVerb(), true, logTreeProb, logPrior, logProb);

    if (isomorphicTreeConstraints) {
      if (debugConstraints)
	System.err.println("assigning satisfied constraint " +
			   item.getConstraint() + " to " + newItem);
      newItem.setConstraint(item.getConstraint());
    }

    boolean added = chart.add(item.start(), item.end(), newItem);
    if (added)
      itemsAdded.add(newItem);
    else
      chart.reclaimItem(newItem);

    return itemsAdded;
  }

  /**
   * Creates a new previous-modifier list given the specified current list
   * and the last modifier on a particular side.
   *
   * @param modChildren the last node of modifying children on a particular
   * side of the head of a chart item
   * @return the list whose first element is the label of the specified
   * modifying child and whose subsequent elements are those of the
   * specified <code>itemPrevMods</code> list, without its final element
   * (which is "bumped off" the edge, since the previous-modifier list
   * has a constant length)
   */
  private final SexpList getPrevMods(CKYItem item, SLNode modChildren) {
    if (modChildren == null)
      return startList;
    prevModLookupList.clear();
    SexpList prevMods = prevModLookupList;
    int i = 0; // index in prev mod list we are constructing
    // as long as there are children and we haven't reached the numPrevMods
    // limit, set elements of prevModList, starting at index 0
    for (SLNode curr = modChildren; curr != null && i < numPrevMods; ) {
      Symbol currMod = (curr.data() == null ? stopSym :
			(Symbol)((CKYItem)curr.data()).label());
      Symbol prevMod = (curr.next() == null ? startSym :
			(Symbol)((CKYItem)curr.next().data()).label());
      if (!Shifter.skip(item, prevMod, currMod)) {
	prevMods.add(prevMod);
	i++;
      }
      curr = curr.next();
    }

    // if, due to skipping, we haven't finished setting prevModList, finish here
    if (i == 0)
      return startList;
    for (; i < numPrevMods; i++)
      prevMods.add(startSym);

    SexpList canonical = (SexpList)canonicalPrevModLists.get(prevMods);
    if (canonical == null) {
      canonical = (SexpList)prevMods.deepCopy();
      canonicalPrevModLists.put(canonical, canonical);
    }
    return canonical;
  }

  private final WordList getPrevModWords(CKYItem item, SLNode modChildren,
					 boolean side) {
    if (modChildren == null)
      return startWordList;
    WordList wordList =
      side == LEFT ? prevModWordLeftLookupList : prevModWordRightLookupList;
    int i = 0; // the index of the previous mod head wordlist
    // as long as there are children and we haven't reached the numPrevWords
    // limit, set elements of wordList, starting at index 0 (i = 0, initially)
    for (SLNode curr = modChildren; curr!=null && i < numPrevWords;) {
      Word currWord = (curr.data() == null ? stopWord :
		       ((CKYItem)curr.data()).headWord());
      Word prevWord = (curr.next() == null ? startWord :
		       (Word)((CKYItem)curr.next().data()).headWord());
      if (!Shifter.skip(item, prevWord, currWord))
	wordList.set(i++, prevWord);
      curr = curr.next();
    }
    // if we ran out of children, but haven't finished setting all numPrevWords
    // elements of word list, set remainder of word list with startWord
    if (i == 0)
      return startWordList;
    for ( ; i < numPrevWords; i++)
      wordList.set(i, startWord);

    return wordList;
  }

  /**
   * There is a comma contraint violation if the word at the split point
   * is a comma and there exists a word following <code>end</code> and that
   * word is not a comma and when it is not the case that the word at
   * <code>end</code> is <i>not</i> a conunction.  The check for a conjunction
   * is to allow chart items representing partial derivations of the form
   * <tt>P --&gt; <i>alpha</i> , CC</tt>, where <i>alpha</i> is a sequence
   * of nonterminals.  This addition to Mike Collins' definition of the comma
   * constraint was necessary because, unlike in Collins' parser, commas
   * and conjunctions are generated in two separate steps.
   */
  protected final boolean commaConstraintViolation(int start,
						   int split,
						   int end) {
    /*
    return (Language.treebank.isComma(getSentenceWord(split)) &&
	    end < sentLen - 1 &&
	    !Language.treebank.isComma(getSentenceWord(end + 1)));
    */
    return (commaForPruning[split] &&
	    end < sentLen - 1 &&
	    !commaForPruning[end + 1] &&
	    !conjForPruning[end]);
  }

  private final Symbol getSentenceWord(int index) {
    return (index >= sentLen ? null :
	    (sentence.get(index).isSymbol() ? sentence.symbolAt(index) :
	     sentence.listAt(index).symbolAt(1)));

  }
}
