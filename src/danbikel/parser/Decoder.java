package  danbikel.parser;

import java.util.HashMap;
import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;
import java.rmi.*;


/**
 * Provides the methods necessary to perform CKY parsing on input sentences.
 */
public class Decoder implements Serializable {
  // debugging constants
  // debugging code will be optimized away when the following booleans are false
  private final static boolean debug = false;
  private final static boolean debugSentenceSize = true;
  private final static boolean debugSpans = false;
  private final static boolean debugInit = true;
  private final static boolean debugTop = false;
  private final static boolean debugComplete = false;
  private final static boolean debugJoin = false;
  private final static boolean debugStops = false;
  private final static boolean debugUnaries = false;
  private final static boolean debugUnariesAndStopProbs = false;
  private final static boolean debugAnalyzeChart = true;
  private final static String debugGoldFilenameProperty =
    "parser.debug.goldFilename";
  private final static boolean debugAnalyzeBestDerivation = false;
  private final static boolean debugOutputChart = false;
  private final static String debugChartFilenamePrefix = "chart";
  private final static boolean debugCommaConstraint = false;
  private final static Symbol S = Symbol.add("S");
  private final static Symbol VP = Symbol.add("VP");
  private final static Symbol FRAG = Symbol.add("FRAG");
  private final static Symbol willSym = Symbol.add("will");
  private final static Symbol mdSym = Symbol.add("MD");

  // constants
  private final static String className = Decoder.class.getName();

  /**
   * A list containing only {@link Training#startSym()}, which is the
   * type of list that should be used when there are zero real previous
   * modifiers (to start the Markov modifier process).
   */
  private final SexpList startList = Trainer.newStartList();

  // data members
  protected int id;
  protected DecoderServerRemote server;
  /** The current sentence index for this decoder (starts at 0). */
  protected int sentenceIdx = -1;
  /** The current sentence. */
  protected SexpList sentence;
  /** The length of the current sentence, cached here for convenience. */
  protected int sentLen;
  /** The parsing chart. */
  protected CKYChart chart;
  /** The map from vocabulary items to their possible parts of speech. */
  protected Map posMap;
  /**
   * An array of all nonterminals observed in training, that is initialized
   * and filled in at construction time.
   */
  protected Symbol[] nonterminals;
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
   * A map from contexts of the last back-off level of the left modifying
   * nonterminal generation model to possible modifying nonterminal labels.
   */
  protected Map leftModNonterminalMap;
  /**
   * A map from contexts of the last back-off level of the right modifying
   * nonterminal generation model to possible modifying nonterminal labels.
   */
  protected Map rightModNonterminalMap;
  /** The left modifying nonterminal generation model structure. */
  protected ProbabilityStructure leftModNonterminalPS;
  /** The last level of back-off in the left modifying nonterminal generation
      model structure. */
  protected int leftModNonterminalPSLastLevel;
  /** The right modifying nonterminal generation model structure. */
  protected ProbabilityStructure rightModNonterminalPS;
  /** The last level of back-off in the right modifying nonterminal generation
      model structure. */
  protected int rightModNonterminalPSLastLevel;
  // these next two data members are used by {@link #preProcess}
  protected Map prunedPretermsPosMap;
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
  /**
   * The value of {@link Training#stopWord()}, cached here for efficiency
   * and convenience.
   */
  protected Word stopWord = Language.training.stopWord();
  /**
   * The value of {@link Training#stopSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol stopSym = Language.training.stopSym();
  /**
   * The value of {@link Training#topSym()}, cached here for efficiency
   * and convenience.
   */
  protected Symbol topSym = Language.training.topSym();
  /** The value of the setting {@link Settings#numPrevMods}. */
  protected int numPrevMods =
    Integer.parseInt(Settings.get(Settings.numPrevMods));
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
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null,
                      emptySubcat, false, false);
  protected ModifierEvent lookupLeftStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null,
                      emptySubcat, false, false);
  protected ModifierEvent lookupRightStopEvent =
    new ModifierEvent(null, null, null, SexpList.emptyList, null, null,
                      emptySubcat, false, false);
  // data members used by getPrevMods
  protected Map canonicalPrevModLists = new danbikel.util.HashMap();
  protected SexpList prevModLookupList = new SexpList(numPrevMods);
  // data members used by joinItems
  protected Subcat lookupSubcat = Subcats.get();
  // values for comma constraint-finding
  protected SLNode headNode = new SLNode(null, null);
  protected boolean useCommaConstraint;

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
    try {
      this.posMap = server.posMap();
      CountsTable nonterminalTable = server.nonterminals();
      if (Subcats.get() instanceof SubcatBag)
        SubcatBag.setUpFastUidMap(nonterminalTable);
      nonterminals = new Symbol[nonterminalTable.size()];
      Iterator it = nonterminalTable.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
        nonterminals[i] = (Symbol)it.next();
      this.leftSubcatMap = server.leftSubcatMap();
      this.rightSubcatMap = server.rightSubcatMap();
      this.leftSubcatPS = server.leftSubcatProbStructure().copy();
      this.rightSubcatPS = server.rightSubcatProbStructure().copy();
      this.leftModNonterminalMap = server.leftModNonterminalMap();
      if (leftModNonterminalMap == null)
        System.err.println("aiiee!  leftModNonterminalMap is null!!!");
      this.rightModNonterminalMap = server.rightModNonterminalMap();
      this.leftModNonterminalPS =
        server.leftModNonterminalProbStructure().copy();
      this.rightModNonterminalPS =
        server.rightModNonterminalProbStructure().copy();
      prunedPretermsPosMap = new danbikel.util.HashMap();
      Set prunedPreterms = server.prunedPreterms();
      it = prunedPreterms.iterator();
      while (it.hasNext()) {
        Word word = Language.treebank.makeWord((Sexp)it.next());
        prunedPretermsPosMap.put(word.word(), word.tag());
      }
      System.err.println("prunedPretermsPosMap: " + prunedPretermsPosMap);
      prunedPunctuationPosMap = new danbikel.util.HashMap();
      Set prunedPunctuation = server.prunedPunctuation();
      it = prunedPunctuation.iterator();
      while (it.hasNext()) {
        Word word = Language.treebank.makeWord((Sexp)it.next());
        prunedPunctuationPosMap.put(word.word(), word.tag());
      }
      //System.err.println("prunedPunctuationPosMap: " + prunedPunctuationPosMap);
    } catch (RemoteException re) {
      System.err.println(re);
    }

    leftSubcatPSLastLevel = leftSubcatPS.numLevels() - 1;
    rightSubcatPSLastLevel = rightSubcatPS.numLevels() - 1;

    leftModNonterminalPSLastLevel = leftModNonterminalPS.numLevels() - 1;
    rightModNonterminalPSLastLevel = rightModNonterminalPS.numLevels() - 1;

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

  protected boolean isPuncRaiseWord(Sexp word) {
    return prunedPunctuationPosMap.containsKey(word);
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
      if (prunedPretermsPosMap.containsKey(word) &&
          !word.toString().equals("'")) {
        sentence.remove(i);
        originalWords.remove(i);
        if (tags != null)
          tags.remove(i);
      }
    }

    sentence = server.convertUnknownWords(sentence);

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
        sentence.remove(i);
        originalWords.remove(i);
        if (tags != null)
          tags.remove(i);
        i--;
      }
      else
        break;
    }
    for (int i = sentence.length() - 1; i > 0; i--) {
      if (sentence.get(i).isList())
        break;
      if (isPuncRaiseWord(sentence.get(i))) {
        sentence.remove(i);
        originalWords.remove(i);
        if (tags != null)
          tags.remove(i);
      }
      else
        break;
    }
  }

  protected void postProcess(Sexp tree) {
    restoreOriginalWords(tree, 0);
    removeOnlyChildBaseNPs(tree);
    canonicalizeNonterminals(tree);
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
   * Handle case where an NP dominates a base NP and has no other children
   * (the base NP is an "only child" of the dominating NP).  This method
   * will effectively remove the base NP node, hooking up all its children
   * as the children of the parent NP.
   */
  protected void removeOnlyChildBaseNPs(Sexp tree) {
    Treebank treebank = Language.treebank;
    if (treebank.isPreterminal(tree))
      return;
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
        Sexp currChild = treeList.get(i);
        boolean hasOnlyChild = treeListLen == 2;
        if (hasOnlyChild &&
            !treebank.isPreterminal(currChild) && currChild.isList()) {
          Symbol parentLabel = treebank.stripAugmentation(treeList.symbolAt(0));
          if (parentLabel == treebank.NPLabel()) {
            Symbol childLabel =
              treebank.stripAugmentation(currChild.list().symbolAt(0));
            if (childLabel == treebank.baseNPLabel()) {
              // we've got an NP dominating an only child base NP!
              // first, remove baseNP from parent
              treeList.remove(i);
              // next, make all baseNP's children become parent's children
              SexpList childList = currChild.list();
              int childListLen = childList.length();
              for (int j = 1; j < childListLen; j++)
                treeList.add(childList.get(j));
              // need to re-calculate cached length
              treeListLen = treeList.length();
            }
          }
        }
        removeOnlyChildBaseNPs(currChild);
      }
    }
  }

  protected void canonicalizeNonterminals(Sexp tree) {
    if (Language.treebank.isPreterminal(tree)) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, Language.treebank.getCanonical(currLabel));
      return;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, Language.treebank.getCanonical(currLabel));
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
        canonicalizeNonterminals(treeList.get(i));
    }
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

    for (int i = 0; i < sentLen; i++) {
      boolean wordIsUnknown = sentence.get(i).isList();
      Symbol word = (wordIsUnknown ?
                     sentence.listAt(i).symbolAt(1) : sentence.symbolAt(i));
      SexpList tagSet = ((wordIsUnknown && tags != null) ?
                         tags.listAt(i) :
                         (SexpList)posMap.get(word));
      if (tagSet == null) {
	word = Language.wordFeatures.defaultFeatureVector();
	tagSet = (SexpList)posMap.get(word);
      }
      if (tagSet == null) {
	tagSet = SexpList.emptyList;
	System.err.println(className + ": warning: no tags for default " +
			   "feature vector " + word);
      }
      int numTags = tagSet.length();
      for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {
        Symbol tag = tagSet.symbolAt(tagIdx);
        Word headWord = new Word(word, tag);
        CKYItem item = chart.getNewItem();
        PriorEvent priorEvent = lookupPriorEvent;
        priorEvent.set(headWord, tag);
        double logPrior = server.logPrior(id, priorEvent);
        double logProb = logPrior; // technically, logPrior + 0.0
        item.set(tag, headWord,
                 emptySubcat, emptySubcat, null, null,
                 null,
                 startList, startList,
                 i, i,
                 false, false, true,
                 0.0, logPrior, logProb);
        chart.add(i, i, item);
        addUnariesAndStopProbs(i, i);
      }
    }
  }

  protected Sexp parse(SexpList sentence) throws RemoteException {
    return parse(sentence, null);
  }

  protected Sexp parse(SexpList sentence, SexpList tags)
    throws RemoteException {
    sentenceIdx++;
    chart.setSizeAndClear(sentence.length());
    initialize(sentence, tags);
    if (debugSentenceSize) {
      System.err.println(className + ": current sentence length: " + sentLen +
                         " word" + (sentLen > 1 ? "s" : ""));
      numSents++;
      avgSentLen = ((numSents - 1)/(float)numSents) * avgSentLen +
                   (float)sentLen / numSents;
      System.err.println(className + ": cummulative average length: " +
                         avgSentLen + " words");
    }
    for (int span = 2; span <= sentLen; span++) {
      if (debugSpans)
        System.err.println(className + ": span: " + span);
      /*
      if (span == sentLen)
	chart.dontDoPruning();
      */
      int split = sentLen - span + 1;
      for (int start = 0; start < split; start++) {
        int end = start + span - 1;
        if (debugSpans)
          System.err.println(className + ": start: " + start + "; end: " + end);
        complete(start, end);
      }
    }

    double prevTopLogProb = chart.getTopLogProb(0, sentLen - 1);
    chart.resetTopLogProb(0, sentLen - 1);
    addTopUnaries(sentLen - 1);

    //chart.doPruning();

    // find top-ranked item whose label is topSym in linear time
    double highestProb = Constants.logOfZero;
    CKYItem topRankedItem = null;
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

    if (debugAnalyzeChart) {
      Sexp goldTree = null;
      try {
	goldTree = Sexp.read(goldTok);
	if (goldTree != null)
	  danbikel.parser.util.DebugChart.findConstituents("chart-debug: ",
                                                           downcaseWords,
							   chart, topRankedItem,
                                                           sentence,
                                                           goldTree);
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
    if (debugTop)
      System.err.println(className + ": highest probability item for " +
                         "sentence-length span (0," + end + "): " +
                         chart.getTopLogProb(0, end));

    topProbItemsToAdd.clear();
    Iterator sentSpanItems = chart.get(0, end);
    while (sentSpanItems.hasNext()) {
      CKYItem item = (CKYItem)sentSpanItems.next();
      if (item.stop()) {

        HeadEvent headEvent = lookupHeadEvent;
        headEvent.set(item.headWord(), topSym, (Symbol)item.label(),
                      emptySubcat, emptySubcat);
        double topLogProb = server.logProbHead(id, headEvent);
        double logProb = item.logTreeProb() + topLogProb;

        if (debugTop)
          System.err.println(className +
                             ": item=" + item + "; topLogProb=" + topLogProb +
                             "; item.logTreeProb()=" + item.logTreeProb() +
                             "; logProb=" + logProb);

        if (topLogProb <= Constants.logOfZero)
          continue;
        CKYItem newItem = chart.getNewItem();
        newItem.set(topSym, item.headWord(),
                    emptySubcat, emptySubcat, item,
                    null, null, startList, startList, 0, end,
                    false, false, true, logProb, 0.0, logProb);
        topProbItemsToAdd.add(newItem);
      }
    }
    Iterator toAdd = topProbItemsToAdd.iterator();
    while (toAdd.hasNext())
      chart.add(0, end, (CKYItem)toAdd.next());
  }

  protected void complete(int start, int end) throws RemoteException {
    for (int split = start; split < end; split++) {

      if (commaConstraintViolation(start, split, end)) {
	if (debugCommaConstraint) {
	  System.err.println(className +
                             ": constraint violation at (start,split,end+1)=(" +
			     start + "," + split + "," + (end + 1) +
			     "); word at end+1 = " + getSentenceWord(end + 1));
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
                if (debugComplete)
                  System.err.println(className +
                                     ".complete: trying to modify\n\t" +
                                     modificandItem + "\n\twith\n\t" +
                                     modifierItem);
                if (!modificandItem.stop() &&
		    derivationOrderOK(modificandItem, modifierSide))
                  joinItems(modificandItem, modifierItem, modifierSide);
              }
            }
          }
        }
      }
    }
    addUnariesAndStopProbs(start, end);
    chart.prune(start, end);
  }

  private boolean derivationOrderOK(CKYItem modificand, boolean modifySide) {
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

    SexpList thisSidePrevMods = getPrevMods(modificand.prevMods(side),
                                            modificand.children(side));
    SexpList oppositeSidePrevMods = modificand.prevMods(!side);

    int thisSideEdgeIndex = modifier.edgeIndex(side);
    int oppositeSideEdgeIndex = modificand.edgeIndex(!side);

    boolean thisSideVerbIntervening =
      modificand.verb(side) || modifier.containsVerb();
    boolean oppositeSideVerbIntervening = modificand.verb(!side);

    ModifierEvent modEvent = lookupModEvent;
    modEvent.set(modifier.headWord(),
                 modificand.headWord(),
                 (Symbol)modifier.label(),
                 thisSidePrevMods,
                 (Symbol)modificand.label(),
                 modificand.headLabel(),
                 modificand.subcat(side),
                 modificand.verb(side),
                 side);

    if (!futurePossible(modEvent, side))
      return;

    if (debugJoin) {
      if (modificand.headWord().tag() == mdSym &&
          modificand.headWord().word() == willSym) {
        Debug.level = 20;
      }
    }

    int lowerIndex = Math.min(thisSideEdgeIndex, oppositeSideEdgeIndex);
    int higherIndex = Math.max(thisSideEdgeIndex, oppositeSideEdgeIndex);

    double logModProb = server.logProbMod(id, modEvent, side);
    if (logModProb <= Constants.logOfZero)
      return;
    double logTreeProb =
      modificand.logTreeProb() + modifier.logTreeProb() + logModProb;

    /*
    double logPrior;
    if (lowerIndex == 0 && higherIndex == sentLen - 1) {
      logPrior = 0.0;
    }
    else {
      PriorEvent priorEvent = lookupPriorEvent;
      priorEvent.set(modificand.headWord(), modificand.label());
      logPrior = server.logPrior(id, priorEvent);
      if (logPrior <= Constants.logOfZero)
	return;
    }
    */
    /*
    PriorEvent priorEvent = lookupPriorEvent;
    priorEvent.set(modificand.headWord(), (Symbol)modificand.label());
    double logPrior = server.logPrior(id, priorEvent);
    if (logPrior <= Constants.logOfZero)
      return;
    */
    double logPrior = modificand.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugJoin) {
      if (modificand.headWord().tag() == mdSym &&
          modificand.headWord().word() == willSym) {
        System.err.println(className +
                           ": trying to join modificand " + modificand +
                           " to modifier " + modifier  + "; logModProb: " +
                           logModProb + "; logPrior: " + logPrior +
                           "; logProb: " + logProb);
      }
      Debug.level = 0;
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

    newItem.setSideInfo(side,
                        thisSideSubcat, thisSideChildren,
                        thisSidePrevMods, thisSideEdgeIndex,
                        thisSideVerbIntervening);
    newItem.setSideInfo(!side,
                        oppositeSideSubcat, oppositeSideChildren,
                        oppositeSidePrevMods, oppositeSideEdgeIndex,
                        oppositeSideVerbIntervening);

    boolean added = chart.add(lowerIndex, higherIndex, newItem);
    if (!added)
      chart.reclaimItem(newItem);
  }

  private boolean futurePossible(ModifierEvent modEvent, boolean side) {
    boolean onLeft = side == Constants.LEFT;
    ProbabilityStructure modPS =
      (onLeft ? leftModNonterminalPS : rightModNonterminalPS);
    int lastLevel =
      (onLeft ? leftModNonterminalPSLastLevel : rightModNonterminalPSLastLevel);
    Map modMap = (onLeft ? leftModNonterminalMap : rightModNonterminalMap);
    Event historyContext = modPS.getHistory(modEvent, lastLevel);
    Set possibleFutures = (Set)modMap.get(historyContext);
    if (possibleFutures != null) {
      Event currentFuture = modPS.getFuture(modEvent, lastLevel);
      if (possibleFutures.contains(currentFuture))
        return true;
    }
    else {
      //no possible futures for history context
    }
    return false;
  }

  protected void addUnariesAndStopProbs(int start, int end)
  throws RemoteException {
    prevItemsAdded.clear();
    currItemsAdded.clear();
    stopProbItemsToAdd.clear();
    boolean buildingOnPreterminalProductions = start == end;
    Iterator it = chart.get(start, end);
    while (it.hasNext()) {
      CKYItem item = (CKYItem)it.next();
      if (item.stop() == false)
        stopProbItemsToAdd.add(item);
      else if (buildingOnPreterminalProductions)
        prevItemsAdded.add(item);
    }

    if (!buildingOnPreterminalProductions) {
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
    unaryItemsToAdd.clear();
    CKYItem newItem = chart.getNewItem();
    newItem.set(null, item.headWord(), null, null, item,
                null, null, startList, startList,
                item.start(), item.end(),
                false, false, false, 0.0, 0.0, 0.0);
    Symbol headSym = (Symbol)item.label();
    HeadEvent headEvent = lookupHeadEvent;
    headEvent.set(item.headWord(), null, headSym, emptySubcat, emptySubcat);
    PriorEvent priorEvent = lookupPriorEvent;
    priorEvent.set(item.headWord(), null);
    // foreach nonterminal
    for (int ntIndex = 0; ntIndex < nonterminals.length; ntIndex++) {
      Symbol parent = nonterminals[ntIndex];
      headEvent.setParent(parent);
      Set leftSubcats = getPossibleSubcats(leftSubcatMap, headEvent,
                                           leftSubcatPS,
                                           leftSubcatPSLastLevel);
      Set rightSubcats = getPossibleSubcats(rightSubcatMap, headEvent,
                                            rightSubcatPS,
                                            rightSubcatPSLastLevel);
      if (debugUnaries) {
        if (newItem.start() == 3 && newItem.end() == 7 &&
            parent == S && headSym == VP) {
          System.err.println(className +
                             ": parent=" + parent + "; head=" + headSym +
                             "; headWord=" + item.headWord());
        }
      }
      if (leftSubcats.size() > 0 && rightSubcats.size() > 0) {
        Iterator leftSubcatIt = leftSubcats.iterator();
        // foreach possible left subcat
        while (leftSubcatIt.hasNext()) {
          Subcat leftSubcat = (Subcat)leftSubcatIt.next();
          Iterator rightSubcatIt = rightSubcats.iterator();
          // foreach possible right subcat
          while (rightSubcatIt.hasNext()) {
            Subcat rightSubcat = (Subcat)rightSubcatIt.next();

            newItem.setLabel(parent);
            newItem.setLeftSubcat(leftSubcat);
            newItem.setRightSubcat(rightSubcat);

            headEvent.setLeftSubcat(leftSubcat);
            headEvent.setRightSubcat(rightSubcat);

            if (debugUnaries) {
            }

            double logProbHead = server.logProbHead(id, headEvent);
            if (logProbHead <= Constants.logOfZero)
              continue;
            double logTreeProb =
              item.logTreeProb() + logProbHead;

	    priorEvent.setLabel(parent);
	    /*
            double logPrior =
	      (item.start() == 0 && item.end() == sentLen - 1 ?
	       0.0 : server.logPrior(id, priorEvent));
	    */
            double logPrior = server.logPrior(id, priorEvent);

            if (logPrior <= Constants.logOfZero)
              continue;

            double logProb = logTreeProb + logPrior;

            if (debugUnaries) {
            }

            if (logProb <= Constants.logOfZero)
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
    Iterator toAdd = unaryItemsToAdd.iterator();
    while (toAdd.hasNext()) {
      CKYItem itemToAdd = (CKYItem)toAdd.next();
      boolean added = chart.add(itemToAdd.start(), itemToAdd.end(), itemToAdd);
      if (added)
        itemsAdded.add(itemToAdd);
      else
        chart.reclaimItem(itemToAdd);
    }

    chart.reclaimItem(newItem);

    return itemsAdded;
  }

  private final Set getPossibleSubcats(Map subcatMap, HeadEvent headEvent,
                                       ProbabilityStructure subcatPS,
                                       int lastLevel) {
    Event lastLevelHist = subcatPS.getHistory(headEvent, lastLevel);
    Set subcats = (Set)subcatMap.get(lastLevelHist);
    return subcats == null ? Collections.EMPTY_SET : subcats;
  }

  protected List addStopProbs(CKYItem item, List itemsAdded)
    throws RemoteException {
    if (!(item.leftSubcat().empty() && item.rightSubcat().empty()))
      return itemsAdded;

    SexpList leftPrevMods =
      getPrevMods(item.leftPrevMods(), item.leftChildren());
    SexpList rightPrevMods =
      getPrevMods(item.rightPrevMods(), item.rightChildren());

    ModifierEvent leftMod = lookupLeftStopEvent;
    leftMod.set(stopWord, item.headWord(), stopSym, leftPrevMods,
                (Symbol)item.label(), item.headLabel(), item.leftSubcat(),
                item.leftVerb(), Constants.LEFT);
    ModifierEvent rightMod = lookupRightStopEvent;
    rightMod.set(stopWord, item.headWord(), stopSym, rightPrevMods,
                 (Symbol)item.label(), item.headLabel(),
                 item.rightSubcat(), item.rightVerb(), Constants.RIGHT);

    if (debugStops) {
      if (item.start() == 0 && item.end() == 75 && item.label() == FRAG) {
        Debug.level = 21;
        System.err.println(className + ": leftMod for stop prob.: " + leftMod);
        System.err.println(className + ": rightMod for stop prob.: " +rightMod);
      }
    }

    double leftLogProb = server.logProbLeft(id, leftMod);
    if (leftLogProb <= Constants.logOfZero)
      return itemsAdded;
    double rightLogProb = server.logProbRight(id, rightMod);
    if (rightLogProb <= Constants.logOfZero)
      return itemsAdded;
    double logTreeProb =
      item.logTreeProb() + leftLogProb + rightLogProb;

    /*
    double logPrior;
    PriorEvent priorEvent = null;
    if (item.start() == 0 && item.end() == sentLen - 1) {
      logPrior = 0.0;
    }
    else {
      priorEvent = lookupPriorEvent;
      priorEvent.set(item.headWord(), (Symbol)item.label());
      logPrior = server.logPrior(id, priorEvent);
      if (logPrior <= Constants.logOfZero)
	return itemsAdded;
    }
    */
    /*
    PriorEvent priorEvent = lookupPriorEvent;
    priorEvent.set(item.headWord(), (Symbol)item.label());
    double logPrior = server.logPrior(id, priorEvent);
    if (logPrior <= Constants.logOfZero)
      return itemsAdded;
    */
    double logPrior = item.logPrior();
    double logProb = logTreeProb + logPrior;

    if (debugStops) {
      if (item.start() == 0 && item.end() == 75 && item.label() == FRAG) {
        //System.err.println(className + ": prior event: " + priorEvent);
        System.err.println(className + ": leftLogProb: " + leftLogProb + "; " +
                           "rightLogProb: " + rightLogProb + "; " +
                           "logTreeProb: " + logTreeProb + "; " +
                           "logPrior: " + logPrior + "; " +
                           "logProb: " + logProb);
        Debug.level = 0;
      }
    }

    if (logProb <= Constants.logOfZero)
      return itemsAdded;

    CKYItem newItem = chart.getNewItem();
    newItem.set((Symbol)item.label(), item.headWord(),
                item.leftSubcat(), item.rightSubcat(),
                item.headChild(),
                item.leftChildren(), item.rightChildren(),
                item.leftPrevMods(), item.rightPrevMods(),
                item.start(), item.end(), item.leftVerb(),
                item.rightVerb(), true, logTreeProb, logPrior, logProb);

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
   * @param itemPrevMods the current previous-modifier list
   * @param modChildren the last node of modifying children on a particular
   * side of the head of a chart item
   * @return the list whose first element is the label of the specified
   * modifying child and whose subsequent elements are those of the
   * specified <code>itemPrevMods</code> list, without its final element
   * (which is "bumped off" the edge, since the previous-modifier list
   * has a constant length)
   */
  private final SexpList getPrevMods(SexpList itemPrevMods,
                                     SLNode modChildren) {
    if (modChildren == null)
      return startList;
    prevModLookupList.clear();
    SexpList prevMods = prevModLookupList;
    Symbol lastMod = (Symbol)((CKYItem)modChildren.data()).label();
    prevMods.add(lastMod);
    int lenMinus1 = numPrevMods - 1;
    for (int i = 0; i < lenMinus1; i++)
      prevMods.add(itemPrevMods.get(i));
    SexpList canonical = (SexpList)canonicalPrevModLists.get(prevMods);
    if (canonical == null) {
      prevMods = (SexpList)prevMods.deepCopy();
      canonicalPrevModLists.put(prevMods, prevMods);
      canonical = prevMods;
    }
    return canonical;
  }

  /**
   * There is a comma contraint violation if the word at the split point
   * is a comma and there exists a word following <code>end</code> and that
   * word is not a comma.
   */
  protected final boolean commaConstraintViolation(int start,
						   int split,
						   int end) {
    return (Language.treebank.isComma(getSentenceWord(split)) &&
	    end < sentLen - 1 &&
	    !Language.treebank.isComma(getSentenceWord(end + 1)));
  }

  private final Symbol getSentenceWord(int index) {
    return (sentence.get(index).isSymbol() ? sentence.symbolAt(index) :
            sentence.listAt(index).symbolAt(1));

  }
}
