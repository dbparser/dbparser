package danbikel.parser;

import danbikel.util.Filter;
import danbikel.util.FlexibleMap;
import danbikel.util.Time;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides access to all <code>Model</code> objects and maps
 * necessary for parsing.  By bundling all of this information
 * together, all of the objects necessary for parsing can be stored
 * and retrieved simply by serializing and de-serializing this object
 * to a Java object file.
 *
 * @see Settings#precomputeProbs
 * @see Settings#writeCanonicalEvents
 */
public class ModelCollection implements Serializable {

  // constants
  protected final static boolean verbose = true;
  protected final static boolean callGCAfterReadingObject = false;

  // data members

  /**
   * An array containing all <code>Model</code> objects contained by this
   * model collection, to be set up by {@link #createModelArray()}.
   */
  protected transient Model[] modelArr;
  /** The model for lexical priors. */
  protected transient Model lexPriorModel;
  /** The model for nonoterminal priors. */
  protected transient Model nonterminalPriorModel;
  /**
   * The model for generating observed root nonterminals given the hidden
   * <tt>+TOP+</tt> nonterminal.
   */
  protected transient Model topNonterminalModel;

  /**
   * The model for generating the head word and part of speech of observed
   * root nonterminals given the hidden <tt>+TOP+</tt> nonterminal.
   */
  protected transient Model topLexModel;
  /**
   * The model for generating a head nonterminal given its (lexicalized)
   * parent.
   */
  protected transient Model headModel;
  /** The model for generating gaps. */
  protected transient Model gapModel;
  /** The model for generating subcats on the left side of the head child. */
  protected transient Model leftSubcatModel;
  /** The model for generating subcats on the right side of the head child. */
  protected transient Model rightSubcatModel;
  /**
   * The model for generating partially-lexicalized nonterminals that modify
   * the head child.
   */
  protected transient Model modNonterminalModel;
  /**
   * The model for generating head words of lexicalized nonterminals that
   * modify the head child.
   */
  protected transient Model modWordModel;
  /**
   * A table that maps observed words to their counts in the training
   * corpus.
   */
  protected transient CountsTable vocabCounter;
  /**
   * A table that maps observed word-feature vectors to their counts
   * in the training corpus.
   */
  protected transient CountsTable wordFeatureCounter;
  /**
   * A table that maps unlexicalized nonterminals to their counts in the
   * training corpus.
   */
  protected transient CountsTable nonterminals;
  /**
   * A mapping from lexical items to all of their possible parts
   * of speech.
   */
  protected transient Map posMap;

  /**
   * A mapping from head labels to possible parent labels.
   */
  protected transient Map headToParentMap;

  /**
   * A mapping from left subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible subcat frames.
   */
  protected transient Map leftSubcatMap;
  /**
   * A mapping from right subcat-prediction conditioning contexts (typically
   * parent and head nonterminal labels) to all possible subcat frames.
   */
  protected transient Map rightSubcatMap;
  /**
   * A mapping from the last level of back-off of modifying nonterminal
   * conditioning contexts to all possible modifying nonterminals.
   */
  protected transient Map modNonterminalMap;
  /** The set of preterminals pruned during training. */
  protected transient Set prunedPreterms;
  /** The set of punctuation preterminals pruned during training. */
  protected transient Set prunedPunctuation;
  /**
   * The reflexive map used to canonicalize objects created when deriving
   * counts for all models in this model collection.
   */
  protected transient FlexibleMap canonicalEvents;

  // derived transient data
  // maps from integers to nonterminals and nonterminals to integers
  /**
   * A map from nonterminal labels (<code>Symbol</code> objects) to
   * unique integers that are indices in the
   * {@linkplain #nonterminalArr nonterminal array}.
   */
  protected Map nonterminalMap;
  /**
   * An array of all nonterminal labels, providing a mapping of unique integers
   * (indices into this array) to nonterminal labels.  The inverse map is
   * contained in {@link #nonterminalMap}.
   */
  protected Symbol[] nonterminalArr;

  /**
   * Constructs a new <code>ModelCollection</code> that initially contains
   * no data.
   */
  public ModelCollection() {}

  /**
   * Sets all the data members of this object.
   *
   * @param lexPriorModel the model for prior probabilities of
   * lexical elements (for the estimation of the joint event that is a
   * fully lexicalized nonterminal)
   * @param nonterminalPriorModel the model for prior probabilities of
   * nonterminals given the lexical components (for the estimation of the
   * joint event that is a fully lexicalized nonterminal)
   * @param topNonterminalModel the head-generation model for heads whose
   * parents are {@link Training#topSym()}
   * @param topLexModel the head-word generation model for heads of entire
   * sentences
   * @param headModel the head-generation model
   * @param gapModel the gap-generation model
   * @param leftSubcatModel the left subcat-generation model
   * @param rightSubcatmodel the right subcat-generation mode,l
   * @param modNonterminalModel the modifying nonterminal-generation model
   * @param modWordModel the modifying word-generation model
   * @param vocabCounter a table of counts of all "known" words of the
   * training data
   * @param wordFeatureCounter a table of counts of all word features ("unknown"
   * words) of the training data
   * @param nonterminals a table of counts of all nonterminals occurring in
   * the training data
   * @param posMap a mapping from lexical items to all of their possible parts
   * of speech
   * @param leftSubcatMap a mapping from left subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param rightSubcatMap a mapping from right subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param modNonterminalMap a mapping from the last level of back-off of
   * modifying nonterminal conditioning contexts to all possible modifying
   * nonterminals
   * @param prunedPreterms the set of preterminals pruned during training
   * @param prunedPunctuation the set of punctuation preterminals pruned
   * during training
   * @param canonicalEvents the reflexive map used to canonicalize objects
   * created when deriving counts for all models in this model collection
   */
  public void set(Model lexPriorModel,
		  Model nonterminalPriorModel,
		  Model topNonterminalModel,
		  Model topLexModel,
		  Model headModel,
		  Model gapModel,
		  Model leftSubcatModel,
		  Model rightSubcatModel,
		  Model modNonterminalModel,
		  Model modWordModel,
		  CountsTable vocabCounter,
		  CountsTable wordFeatureCounter,
		  CountsTable nonterminals,
		  Map posMap,
		  Map headToParentMap,
		  Map leftSubcatMap,
		  Map rightSubcatMap,
		  Map modNonterminalMap,
		  Set prunedPreterms,
		  Set prunedPunctuation,
		  FlexibleMap canonicalEvents) {
    this.lexPriorModel = lexPriorModel;
    this.nonterminalPriorModel = nonterminalPriorModel;
    this.topNonterminalModel = topNonterminalModel;
    this.topLexModel = topLexModel;
    this.headModel = headModel;
    this.gapModel = gapModel;
    this.leftSubcatModel = leftSubcatModel;
    this.rightSubcatModel = rightSubcatModel;
    this.modNonterminalModel = modNonterminalModel;
    this.modWordModel = modWordModel;

    createModelArray();

    this.vocabCounter = vocabCounter;
    this.wordFeatureCounter = wordFeatureCounter;
    this.nonterminals = nonterminals;
    this.posMap = posMap;
    this.headToParentMap = headToParentMap;
    this.leftSubcatMap = leftSubcatMap;
    this.rightSubcatMap = rightSubcatMap;
    this.modNonterminalMap = modNonterminalMap;
    this.prunedPreterms = prunedPreterms;
    this.prunedPunctuation = prunedPunctuation;

    createNonterminalMap();

    this.canonicalEvents = canonicalEvents;
  }

  protected void createModelArray() {
    modelArr = new Model[] {
      lexPriorModel,
      nonterminalPriorModel,
      topNonterminalModel,
      topLexModel,
      headModel,
      gapModel,
      leftSubcatModel,
      rightSubcatModel,
      modNonterminalModel,
      modWordModel,
    };
  }

  public List modelList() {
    return Collections.unmodifiableList(Arrays.asList(modelArr));
  }

  public Iterator modelIterator() {
    return modelList().iterator();
  }

  private void createNonterminalMap() {
    nonterminalMap = new HashMap(nonterminals.size());
    nonterminalArr = new Symbol[nonterminals.size()];
    Iterator nts = nonterminals.keySet().iterator();
    for (int uid = 0; nts.hasNext(); uid++) {
      Symbol nonterminal = (Symbol)nts.next();
      nonterminalArr[uid] = nonterminal;
      nonterminalMap.put(nonterminal, new Integer(uid));
    }
  }

  /**
   * Somewhat of a hack: we allow counts for a back-off level from
   * one model to be shared with another model; in this case, the
   * last level of back-off from the modWordModel is being
   * shared (i.e., will be used) as the last level of back-off for
   * topLexModel, as the last levels of both these models should just
   * be estimating p(w | t).
   *
   * @param verbose indicates whether to print a message to
   * <code>System.err</code>
   */
  public void shareCounts(boolean verbose) {
    if (verbose)
      System.err.println("Sharing last level of modWordModel to be " +
			 "last level of topLexModel.");
    int modWordLastLevel =
	modWordModel.getProbStructure().numLevels() - 1;
    int topLexLastLevel =
	topLexModel.getProbStructure().numLevels() - 1;
    modWordModel.share(modWordLastLevel, topLexModel, topLexLastLevel);
  }

  public int numNonterminals() { return nonterminalArr.length; }
  public Map getNonterminalMap() { return nonterminalMap; }
  public Symbol[] getNonterminalArr() { return nonterminalArr; }

  // accessors
  public Model lexPriorModel() { return lexPriorModel; }
  public Model nonterminalPriorModel() { return nonterminalPriorModel; }
  public Model topNonterminalModel() { return topNonterminalModel; }
  public Model topLexModel() { return topLexModel; }
  public Model headModel() { return headModel; }
  public Model gapModel() { return gapModel; }
  public Model leftSubcatModel() { return leftSubcatModel; }
  public Model rightSubcatModel() { return rightSubcatModel; }
  public Model modNonterminalModel() { return modNonterminalModel; }
  public Model modWordModel() { return modWordModel; }
  public CountsTable vocabCounter() { return vocabCounter; }
  public CountsTable wordFeatureCounter() { return wordFeatureCounter; }
  public CountsTable nonterminals() { return nonterminals; }
  public Map posMap() { return posMap; }
  public Map headToParentMap() { return headToParentMap; }
  public Map leftSubcatMap() { return leftSubcatMap; }
  public Map rightSubcatMap() { return rightSubcatMap; }
  public Map modNonterminalMap() { return modNonterminalMap; }
  public Set prunedPreterms() { return prunedPreterms; }
  public Set prunedPunctuation() { return prunedPunctuation; }

  public FlexibleMap canonicalEvents() { return canonicalEvents; }

  // utility method
  public String getModelCacheStats() {
    StringBuffer sb = new StringBuffer(300 * modelArr.length);
    int numModels = modelArr.length;
    for (int i = 0; i < numModels; i++) {
      sb.append(modelArr[i].getCacheStats());
    }
    return sb.toString();
  }

  // I/O methods

  protected void internalWriteObject(java.io.ObjectOutputStream s)
    throws IOException {

    s.defaultWriteObject();

    Time tempTimer = null;
    if (verbose)
      tempTimer = new Time();

    if (verbose) {
      System.err.print("Writing out canonicalEvents...");
      tempTimer.reset();
    }
    boolean precomputeProbs =
      Boolean.valueOf(Settings.get(Settings.precomputeProbs)).booleanValue();
    if (precomputeProbs)
      canonicalEvents = null;
    else {
      String writeCanonicalEventsStr =
	Settings.get(Settings.writeCanonicalEvents);
      boolean writeCanonicalEvents =
	Boolean.valueOf(writeCanonicalEventsStr).booleanValue();
      if (!writeCanonicalEvents) {
	if (verbose)
	  System.err.print("emptying...");
	canonicalEvents.clear();
      }
    }
    s.writeObject(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out lexPriorModel...");
      tempTimer.reset();
    }
    s.writeObject(lexPriorModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out nonterminalPriorModel...");
      tempTimer.reset();
    }
    s.writeObject(nonterminalPriorModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out topNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(topNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out topLexModel...");
      tempTimer.reset();
    }
    s.writeObject(topLexModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out headModel...");
      tempTimer.reset();
    }
    s.writeObject(headModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out gapModel...");
      tempTimer.reset();
    }
    s.writeObject(gapModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out leftSubcatModel...");
      tempTimer.reset();
    }
    s.writeObject(leftSubcatModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightSubcatModel...");
      tempTimer.reset();
    }
    s.writeObject(rightSubcatModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(modNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modWordModel...");
      tempTimer.reset();
    }
    s.writeObject(modWordModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out vocabCounter...");
      tempTimer.reset();
    }
    s.writeObject(vocabCounter);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out wordFeatureCounter...");
      tempTimer.reset();
    }
    s.writeObject(wordFeatureCounter);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out nonterminals...");
      tempTimer.reset();
    }
    s.writeObject(nonterminals);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out posMap...");
      tempTimer.reset();
    }
    s.writeObject(posMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out headToParentMap...");
      tempTimer.reset();
    }
    s.writeObject(headToParentMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out leftSubcatMap...");
      tempTimer.reset();
    }
    s.writeObject(leftSubcatMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightSubcatMap...");
      tempTimer.reset();
    }
    s.writeObject(rightSubcatMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out modNonterminalMap...");
      tempTimer.reset();
    }
    s.writeObject(modNonterminalMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out prunedPreterms...");
      tempTimer.reset();
    }
    s.writeObject(prunedPreterms);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out prunedPunctuation...");
      tempTimer.reset();
    }
    s.writeObject(prunedPunctuation);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
  }

  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

    internalWriteObject(s);
    /*
    if (verbose)
      System.err.println("Total time writing out ModelCollection object: " +
			 totalTime + ".");
    */
  }

  protected void internalReadObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {

    Time tempTimer = null;
    if (verbose)
      tempTimer = new Time();

    if (verbose) {
      System.err.print("Reading canonicalEvents...");
      tempTimer.reset();
    }
    canonicalEvents = (FlexibleMap)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading lexPriorModel...");
      tempTimer.reset();
    }
    lexPriorModel = (Model)s.readObject();
    lexPriorModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading nonterminalPriorModel...");
      tempTimer.reset();
    }
    nonterminalPriorModel = (Model)s.readObject();
    nonterminalPriorModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading topNonterminalModel...");
      tempTimer.reset();
    }
    topNonterminalModel = (Model)s.readObject();
    topNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading topLexModel...");
      tempTimer.reset();
    }
    topLexModel = (Model)s.readObject();
    topLexModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading headModel...");
      tempTimer.reset();
    }
    headModel = (Model)s.readObject();
    headModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading gapModel...");
      tempTimer.reset();
    }
    gapModel = (Model)s.readObject();
    gapModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading leftSubcatModel...");
      tempTimer.reset();
    }
    leftSubcatModel = (Model)s.readObject();
    leftSubcatModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightSubcatModel...");
      tempTimer.reset();
    }
    rightSubcatModel = (Model)s.readObject();
    rightSubcatModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modNonterminalModel...");
      tempTimer.reset();
    }
    modNonterminalModel = (Model)s.readObject();
    modNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modWordModel...");
      tempTimer.reset();
    }
    modWordModel = (Model)s.readObject();
    modWordModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading vocabCounter...");
      tempTimer.reset();
    }
    vocabCounter = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading wordFeatureCounter...");
      tempTimer.reset();
    }
    wordFeatureCounter = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading nonterminals...");
      tempTimer.reset();
    }
    nonterminals = (CountsTable)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading posMap...");
      tempTimer.reset();
    }
    posMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading headToParentMap...");
      tempTimer.reset();
    }
    headToParentMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading leftSubcatMap...");
      tempTimer.reset();
    }
    leftSubcatMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightSubcatMap...");
      tempTimer.reset();
    }
    rightSubcatMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading modNonterminalMap...");
      tempTimer.reset();
    }
    modNonterminalMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading prunedPreterms...");
      tempTimer.reset();
    }
    prunedPreterms = (Set)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading prunedPunctuation...");
      tempTimer.reset();
    }
    prunedPunctuation = (Set)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
  }

  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

    s.defaultReadObject();

    internalReadObject(s);

    createModelArray();

    if (verbose)
      System.err.println("Total time reading ModelCollection object: " +
			 totalTime + ".");

    if (callGCAfterReadingObject) {
      if (verbose)
	System.err.print("gc...");
      System.gc();
      if (verbose)
	System.err.println("done");
    }
  }
}
