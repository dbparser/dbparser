package danbikel.parser;

import danbikel.util.Time;
import danbikel.util.FlexibleMap;
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
  private final static boolean verbose = true;
  private final static boolean callGCAfterReadingObject = false;

  // data members
  private transient Model lexPriorModel;
  private transient Model nonterminalPriorModel;
  private transient Model topNonterminalModel;
  private transient Model topLexModel;
  private transient Model headModel;
  private transient Model gapModel;
  private transient Model leftSubcatModel;
  private transient Model rightSubcatModel;
  private transient Model leftModNonterminalModel;
  private transient Model rightModNonterminalModel;
  private transient Model leftModWordModel;
  private transient Model rightModWordModel;
  private transient CountsTable vocabCounter;
  private transient CountsTable wordFeatureCounter;
  private transient CountsTable nonterminals;
  private transient Map posMap;
  private transient Map leftSubcatMap;
  private transient Map rightSubcatMap;
  private transient Map leftModNonterminalMap;
  private transient Map rightModNonterminalMap;
  private transient Set prunedPreterms;
  private transient Set prunedPunctuation;
  private transient FlexibleMap canonicalEvents;

  // derived transient data
  // maps from integers to nonterminals and nonterminals to integers
  private Map nonterminalMap;
  private Symbol[] nonterminalArr;

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
   * @param leftModNonterminalModel the left modifying nonterminal-generation
   * model
   * @param rightModNonterminalModel the right modifying nonterminal-generation
   * model
   * @param leftModWordModel the left modifying word-generation model
   * @param vocabCounter a table of counts of all "known" words of the
   * training data
   * @param wordFeatureCounter a table of counts of all word features ("unknown"
   * words) of the training data
   * @param nonterminals a table of counts of all nonterminals occurring in
   * the training data
   * @param rightModWordModel the right modifying word-generation model
   * @param posMap a mapping from lexical items to all of their possible parts
   * of speech
   * @param leftSubcatMap a mapping from left subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param leftSubcatMap a mapping from right subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   */
  public void set(Model lexPriorModel,
		  Model nonterminalPriorModel,
		  Model topNonterminalModel,
		  Model topLexModel,
		  Model headModel,
		  Model gapModel,
		  Model leftSubcatModel,
		  Model rightSubcatModel,
		  Model leftModNonterminalModel,
		  Model rightModNonterminalModel,
		  Model leftModWordModel,
		  Model rightModWordModel,
		  CountsTable vocabCounter,
		  CountsTable wordFeatureCounter,
		  CountsTable nonterminals,
		  Map posMap,
		  Map leftSubcatMap,
		  Map rightSubcatMap,
                  Map leftModNonterminalMap,
                  Map rightModNonterminalMap,
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
    this.leftModNonterminalModel = leftModNonterminalModel;
    this.rightModNonterminalModel = rightModNonterminalModel;
    this.leftModWordModel = leftModWordModel;
    this.rightModWordModel = rightModWordModel;
    this.vocabCounter = vocabCounter;
    this.wordFeatureCounter = wordFeatureCounter;
    this.nonterminals = nonterminals;
    this.posMap = posMap;
    this.leftSubcatMap = leftSubcatMap;
    this.rightSubcatMap = rightSubcatMap;
    this.leftModNonterminalMap = leftModNonterminalMap;
    this.rightModNonterminalMap = rightModNonterminalMap;
    this.prunedPreterms = prunedPreterms;
    this.prunedPunctuation = prunedPunctuation;

    this.canonicalEvents = canonicalEvents;

    createNonterminalMap();
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
  public Model leftModNonterminalModel() { return leftModNonterminalModel; }
  public Model rightModNonterminalModel() { return rightModNonterminalModel; }
  public Model leftModWordModel() { return leftModWordModel; }
  public Model rightModWordModel() { return rightModWordModel; }
  public CountsTable vocabCounter() { return vocabCounter; }
  public CountsTable wordFeatureCounter() { return wordFeatureCounter; }
  public CountsTable nonterminals() { return nonterminals; }
  public Map posMap() { return posMap; }
  public Map leftSubcatMap() { return leftSubcatMap; }
  public Map rightSubcatMap() { return rightSubcatMap; }
  public Map leftModNonterminalMap() { return leftModNonterminalMap; }
  public Map rightModNonterminalMap() { return rightModNonterminalMap; }
  public Set prunedPreterms() { return prunedPreterms; }
  public Set prunedPunctuation() { return prunedPunctuation; }

  public FlexibleMap canonicalEvents() { return canonicalEvents; }


  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

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
      System.err.print("Writing out leftModNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(leftModNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightModNonterminalModel...");
      tempTimer.reset();
    }
    s.writeObject(rightModNonterminalModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out leftModWordModel...");
      tempTimer.reset();
    }
    s.writeObject(leftModWordModel);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightModWordModel...");
      tempTimer.reset();
    }
    s.writeObject(rightModWordModel);
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
      System.err.print("Writing out leftModNonterminalMap...");
      tempTimer.reset();
    }
    s.writeObject(leftModNonterminalMap);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Writing out rightModNonterminalMap...");
      tempTimer.reset();
    }
    s.writeObject(rightModNonterminalMap);
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
    /*
    if (verbose)
      System.err.println("Total time writing out ModelCollection object: " +
                         totalTime + ".");
    */
  }

  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException {
    Time totalTime = null;
    if (verbose)
      totalTime = new Time();

    s.defaultReadObject();

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
      System.err.print("Reading leftModNonterminalModel...");
      tempTimer.reset();
    }
    leftModNonterminalModel = (Model)s.readObject();
    leftModNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightModNonterminalModel...");
      tempTimer.reset();
    }
    rightModNonterminalModel = (Model)s.readObject();
    rightModNonterminalModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading leftModWordModel...");
      tempTimer.reset();
    }
    leftModWordModel = (Model)s.readObject();
    leftModWordModel.setCanonicalEvents(canonicalEvents);
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightModWordModel...");
      tempTimer.reset();
    }
    rightModWordModel = (Model)s.readObject();
    rightModWordModel.setCanonicalEvents(canonicalEvents);
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
      System.err.print("Reading leftModNonterminalMap...");
      tempTimer.reset();
    }
    leftModNonterminalMap = (Map)s.readObject();
    if (verbose)
      System.err.println("done (" + tempTimer + ").");
    if (verbose) {
      System.err.print("Reading rightModNonterminalMap...");
      tempTimer.reset();
    }
    rightModNonterminalMap = (Map)s.readObject();
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
