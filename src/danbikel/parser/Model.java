package danbikel.parser;

import java.util.HashMap;
import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * This class computes the probability of generating an output element
 * of this parser.  It derives counts from top-level <code>TrainerEvent</code>
 * objects, storing these derived counts in its internal data structures.
 * The derived counts are necessary for the smoothing of the top-level
 * probabilities used by the parser, and the particular structure of those
 * levels of smoothing (or, less accurately, back-off) are specified by the
 * <code>ProbabilityStructure</code> argument to the
 * {@link #Model(ProbabilityStructure) constructor} and to the
 * {@link #estimateLogProb(int,TrainerEvent)}
 * method.
 * <p>
 * <b>N.B.</b>: While the name of this class is "Model", more strictly
 * speaking it computes the probabilities for an entire class of
 * parameters used by the overall parsing model.  As such--using a looser
 * definition of the term "model"--this class can be considered to represent
 * a "submodel", in that it contains a model of the generation of a particular
 * type of output element of this parser.
 *
 * @see ProbabilityStructure
 */
public class Model implements Serializable {
  // constants
  private final static boolean useCache = true;
  private final static int minCacheSize = 1000;

  // data members

  // the prob structure to use
  private ProbabilityStructure structure;
  // the prob structures for individual clients in a multithreaded environment
  private Map structureMap = Collections.synchronizedMap(new HashMap());
  // some handles on info available from structure object
  private String structureClassName;
  private int numLevels;
  private int specialLevel;
  // the actual counts
  private CountsTrio[] counts;
  private int numCanonicalizableEvents = 0;
  // whether to report to stderr what this class is doing
  private boolean verbose = true;
  // for temporary storage of histories (so we don't have to copy histories
  // created by deriveHistories() to create transition objects)
  private transient HashMap[] histories;
  private transient ProbabilityCache topLevelCache;
  private transient ProbabilityCache[] cache;
  private transient int[] cacheHits;
  private transient int[] cacheAccesses;

  private Map canonicalEvents;
  static int numCacheAdds = 0;
  static int numCanonicalHits = 0;

  /**
   * Constructs a new object for deriving all counts using the specified
   * probability structure.
   *
   * @param structure the probability structure to use when deriving counts
   */
  public Model(ProbabilityStructure structure) {
    this.structure = structure;
    structureClassName = structure.getClass().getName();
    numLevels = structure.numLevels();
    specialLevel = structure.specialLevel();
    counts = new CountsTrio[numLevels];
    for (int i = 0; i < counts.length; i++)
      counts[i] = new CountsTrio();
    if (useCache)
      setUpCaches();
  }

  private void setUpCaches() {
    int cacheSize = Math.max(structure.cacheSize(0), minCacheSize);
    //topLevelCache = new ProbabilityCache(cacheSize, cacheSize / 4 + 1);

    cacheHits = new int[numLevels];
    cacheAccesses = new int[numLevels];

    cache = new ProbabilityCache[numLevels];
    for (int i = 0; i < cache.length; i++) {
      cacheSize = Math.max(structure.cacheSize(i), minCacheSize);
      /*
      System.err.println("setting up " + structure.getClass().getName() +
                         " cache at level " + i + "\n\tto have max. cap. of " +
                         cacheSize + " and init. cap. of " +
                         (cacheSize / 4 + 1));
      */
      cache[i] = new ProbabilityCache(cacheSize, cacheSize / 4 + 1);
    }
  }

  /**
   * Derives all counts from the specified counts table, using the
   * probability structure specified in the constructor.
   *
   * @param counts a <code>CountsTable</code> containing {@link TrainerEvent}
   * objects from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter) {
    deriveHistories(trainerCounts, filter);

    Time time = null;
    if (verbose)
      time = new Time();

    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      HashMapInt.Entry entry = (HashMapInt.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      int count = entry.getIntValue();
      if (!filter.pass(event))
	continue;

      // store all non-special level transitions
      for (int level = 0; level < numLevels; level++) {
	if (level == specialLevel)
	  continue;
	Event lookupHistory = structure.getHistory(event, level);

	/*
	System.err.println("level: " + level +
			   "; lookup hist: " + lookupHistory);
	*/

	if (counts[level].history().containsKey(lookupHistory)) {

	  /*
	  System.err.println("yea! found it!");
	  */

	  Event history = (Event)histories[level].get(lookupHistory);
	  Event future = structure.getFuture(event, level);
	  Transition transition = new Transition(future.copy(), history);
	  counts[level].transition().add(transition, count);

	  /*
	  System.err.println("level: " + level +
			     "; transition: " + transition +
			     "; count: " + count.get());
	  */
	}
      }

      // store all special level transitions, if a special level exists
      if (specialLevel >= 0)
	deriveSpecialLevelTransitions(event, count);
    }

    if (verbose)
      System.err.println("Derived transitions for " + structureClassName +
			 " in " + time + ".");

    deriveUniqueCounts();

    deriveSpecialLevelUniqueCounts();

    histories = null;
  }

  /**
   *
   */
  public double estimateLogProb(int id, TrainerEvent event) {
    Integer idInt = new Integer(id);
    ProbabilityStructure clientStructure =
      (ProbabilityStructure)structureMap.get(idInt);
    if (clientStructure == null) {
      clientStructure = structure.copy();
      structureMap.put(idInt, clientStructure);
    }
    return Math.log(estimateProb(clientStructure, event));
  }

  protected double estimateProb(ProbabilityStructure structure,
				TrainerEvent event) {
    if (Debug.level >= 20) {
      System.err.println(structure.getClass().getName() + "\n\t" + event);
    }

    /*
    if (useCache) {
      Double cacheProb = topLevelCache.getProb(event);
      if (cacheProb != null)
        return cacheProb.doubleValue();
    }
    */
    int highestCachedLevel = numLevels;

    structure.prevHistCount = 0;
    for (int level = 0; level < numLevels; level++) {
      if (level == specialLevel)
	estimateSpecialLevelProb(structure, event);

      Event history = structure.getHistory(event, level);
      Transition transition = structure.getTransition(event, level);

      // check cache here!!!!!!!!!!!!
      if (useCache) {
        cacheAccesses[level]++;
        if (Debug.level >= 21) {
          System.err.print("getting prob for " + transition +
                           " at level " + level + ": ");
        }
        double cacheProb = cache[level].getProb(transition);
        if (Debug.level >= 21) {
          System.err.println(cacheProb);
        }
        if (Double.isNaN(cacheProb) == false) {
          if (Debug.level >= 21) {
            System.err.println("yea! " + structure.getClass().getName() +
                               "; level=" + level);
          }
          structure.estimates[level] = cacheProb;
          cacheHits[level]++;
          highestCachedLevel = level;
          break;
        }
        else {
          if (Debug.level >= 21) {
            System.err.println("nope");
          }
        }
      }
      CountsTrio trio = counts[level];
      double historyCount = trio.history().count(history);
      double transitionCount = trio.transition().count(transition);
      double uniqueCount = trio.unique().count(history);

      double lambda, estimate, adjustment = 1.0;
      double fudge = structure.lambdaFudge(level);
      if (historyCount == 0) {
	lambda = 0;
	estimate = 0;
      }
      else {
	if (structure.prevHistCount <= historyCount)
	  adjustment = 1 - structure.prevHistCount / historyCount;
	lambda =
	  adjustment * (historyCount / (historyCount + fudge * uniqueCount));
	estimate = transitionCount / historyCount;
      }

      if (Debug.level >= 20) {
        for (int i = 0; i < level; i++)
          System.err.print("  ");
        System.err.println(transitionCount + "    " +
                           historyCount + "    " + uniqueCount + "    " +
                           adjustment + "    " + estimate);
      }

      structure.lambdas[level] = lambda;
      structure.estimates[level] = estimate;
      structure.prevHistCount = historyCount;
    }
    double prob = 0.0;
    if (useCache) {
      prob = (highestCachedLevel == numLevels ?
              0.0 : structure.estimates[highestCachedLevel]);
    }
    for (int level = highestCachedLevel - 1; level >= 0; level--) {
      double lambda = structure.lambdas[level];
      double estimate = structure.estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (useCache  && prob > Constants.logOfZero) {
        Transition transition = structure.transitions[level];
	if (Debug.level >= 21) {
	  System.err.println("adding " + transition + " to cache at level " +
			     level);
	}

        if (!cache[level].containsKey(transition)) {
          numCacheAdds++;
          // don't want to copy history and future if we don't have to
          Event transHist = transition.history();
          Event transFuture = transition.future();
          CountsTrio trio = counts[level];
          Event hist = (Event)canonicalEvents.get(transHist);
          if (hist == null) {
            hist = transHist.copy();
            //System.err.println("no hit: " + hist);
          }
          else {
            numCanonicalHits++;
            //System.err.println("hit: " + hist);
          }
          Event future = (Event)canonicalEvents.get(transFuture);
          if (future == null) {
            future = transFuture.copy();
            //System.err.println("no hit: " + future);
          }
          else {
            numCanonicalHits++;
            //System.err.println("hit: " + future);
          }
          cache[level].put(new Transition(future, hist), prob);
          //cache[level].put(transition.copy(), prob);
        }
      }
    }

    /*
    if (useCache && prob > Constants.logOfZero) {
      if (!topLevelCache.containsKey(event))
        topLevelCache.put(event.copy(), prob);
    }
    */
    return prob;
  }

  protected double estimateProbOld(ProbabilityStructure structure,
				   TrainerEvent event,
				   int level, double prevHistCount) {
    if (level == numLevels)
      return 0.0;
    if (level == specialLevel)
      return estimateSpecialLevelProb(structure, event);

    // check cache here!!!!!!!!!!!!!!!!!!!!!!!

    Event history = structure.getHistory(event, level);
    Transition transition = structure.getTransition(event, level);
    CountsTrio trio = counts[level];
    double historyCount = trio.history().count(history);
    double transitionCount = trio.transition().count(transition);
    double uniqueCount = trio.unique().count(history);

    double lambda, estimate, adjustment = 1.0;
    double fudge = structure.lambdaFudge(level);
    if (historyCount == 0) {
      lambda = 0;
      estimate = 0;
    }
    else {
      if (prevHistCount <= historyCount)
	adjustment = 1 - prevHistCount / historyCount;
      lambda =
	adjustment * (historyCount / (historyCount + fudge * uniqueCount));
      estimate = transitionCount / historyCount;
    }
    double backOffProb = estimateProbOld(structure,
				      event, level + 1, historyCount);
    double probVal = (lambda * estimate) + ((1 - lambda) * backOffProb);

    // cache probVal here!!!!!!!!!!!!!!!!!!!!!

    return probVal;
  }

  /**
   * If a special level exists, this method should be overridden to
   * fill in values at <code>structure.probs[level]</code>,
   * <code>structure.lambdas[level]</code> and
   * <code>structure.prevHistCount</code>.
   */
  protected double estimateSpecialLevelProb(ProbabilityStructure structure,
					    TrainerEvent event) {
    return 1.0;
  }

  /**
   * Called by
   * {@link #deriveCounts(CountsTable,Filter)}, for each
   * type of transition observed, this method derives the number of
   * unique transitions from the history context to the possible
   * futures.  This number of unique transitions, called the
   * <i>diversity</i> of a random variable, is used in a modified
   * version of Witten-Bell smoothing.
   */
  protected void deriveUniqueCounts() {
    // derive unique counts for non-special levels
    Time time = null;
    if (verbose)
      time = new Time();

    for (int level = 0; level < numLevels; level++) {
      if (level == specialLevel)
	continue;
      Iterator it = counts[level].transition().keySet().iterator();
      while (it.hasNext()) {
	Transition transition = (Transition)it.next();
	counts[level].unique().add(transition.history());
      }
    }

    if (verbose)
      System.err.println("Derived unique counts for " + structureClassName +
			 " in " + time + ".");
  }

  /**
   * Called by {@link #deriveUniqueCounts}, this method provides a hook
   * to counts unique transitions at the special back-off level of a model,
   * if one exists.
   */
  protected void deriveSpecialLevelUniqueCounts() {
  }

  /**
   * Called by {@link #deriveCounts(CountsTable,Filter)},
   * this method provides a hook to count transition(s) for the special
   * level of back-off of the model, if one exists.
   */
  protected void deriveSpecialLevelTransitions(TrainerEvent event,
					       int count) {
  }

  private void deriveHistories(CountsTable trainerCounts, Filter filter) {
    Time time = null;
    if (verbose)
      time = new Time();

    histories = new HashMap[numLevels];
    for (int level = 0; level < numLevels; level++)
      histories[level] = new HashMap();

    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      HashMapInt.Entry entry = (HashMapInt.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      int count = entry.getIntValue();
      if (!filter.pass(event))
	continue;
      // store all histories for all non-special back-off levels
      for (int level = 0; level < numLevels; level++) {
	if (level == specialLevel)
	  continue;
	Event history = structure.getHistory(event, level).copy();
	counts[level].history().add(history, count);
	histories[level].put(history, history);

	/*
	System.err.println("level: " + level + "; history: " + history);
	*/
      }

      // store all histories for the special back-off level (if one exists)
      if (specialLevel >= 0)
	deriveSpecialLevelHistories(event, count);
    }

    if (verbose)
      System.err.println("Derived histories for " + structureClassName +
			 " in " + time + ".");
  }

  /**
   * This method provides a hook for counting histories at the special level
   * of back-off, if one exists.
   */
  protected void deriveSpecialLevelHistories(TrainerEvent event,
					     int count) {
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in the events contained in this object.  Use of this
   * method is intended to conserve memory by removing duplicate
   * copies of event information in different event objects.
   */
  public void canonicalize() {
    canonicalize(new HashMap());
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in the events contained in this object using the
   * specified map.  Use of this method is intended to conserve memory
   * by removing duplicate copies of event information in different
   * event objects.
   *
   * @param map a map of canonical information structures of the
   * <code>Event</code> objects contained in this object; this parameter
   * allows multiple <code>Model</code> objects to have their
   * events structures canonicalized with respect to each other
   */
  public void canonicalize(Map map) {
    canonicalEvents = map;
    int prevMapSize = map.size();
    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalize(trio.history(), map);
      canonicalize(trio.unique(), map);
      canonicalize(trio.transition(), map);
    }

    if (verbose) {
      System.err.println("Canonicalized Sexp objects; " +
                         "now canonicalizing Event objects");
    }

    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalizeEvents(trio.history(), map);
      canonicalizeEvents(trio.unique(), map);
      canonicalizeEvents(trio.transition(), map);
    }
    int increase = map.size() - prevMapSize;

    if (verbose) {
      System.err.print("Canonicalized " + numCanonicalizableEvents +
		       " events; map grew by " + increase + " elements to " +
		       map.size() + " (gc ... ");
      System.err.flush();
    }
    System.gc();
    if (verbose)
      System.err.println("done).");

    // reset for use by this method if it is called again
    numCanonicalizableEvents = 0;
  }

  private void canonicalize(CountsTable table, Map map) {
    Iterator it = table.keySet().iterator();
    while (it.hasNext()) {
      Object histOrTrans = it.next();
      if (histOrTrans instanceof Event) {
	if (((Event)histOrTrans).canonicalize(map) != -1)
	  numCanonicalizableEvents++;
      }
      else {
	Transition trans = (Transition)histOrTrans;
	if (trans.history().canonicalize(map) != -1)
	  numCanonicalizableEvents++;
	if (trans.future().canonicalize(map) != -1)
	  numCanonicalizableEvents++;
      }
    }
  }

  private void canonicalizeEvents(CountsTable table, Map map) {
    Iterator it = table.keySet().iterator();
    while (it.hasNext()) {
      Object histOrTrans = it.next();
      if (map.containsKey(histOrTrans) == false)
        map.put(histOrTrans, histOrTrans);
    }
    numCanonicalizableEvents += table.size();
  }

  // accessors
  /**
   * Returns the type of <code>ProbabilityStructure</code> object used
   * during the invocation of {@link #deriveCounts}.
   *
   * <p>A copy of this object should be created and stored for each
   * parsing client thread, for use when the clients need to call the
   * probability-computation methods of this class.  This scheme
   * allows the reusable data members inside the
   * <code>ProbabilityStructure</code> objects to be used by multiple
   * clients without any concurrency problems, thereby maintaining
   * their efficiency and thread-safety.
   */
  public ProbabilityStructure getProbStructure() { return structure; }

  // mutators
  /**
   * Causes this class to be verbose in its output to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter)}.
   */
  public void beVerbose() { verbose = true;}
  /**
   * Causes this class not to output anything to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter)}.
   */
  public void beQuiet() { verbose = false; }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (useCache)
      setUpCaches();
  }

  protected void finalize() throws Throwable {
    synchronized (Model.class) {
      System.err.println("cache data for " + structure.getClass().getName() +
                         ":");
      for (int level = 0; level < cacheHits.length; level++) {
        System.err.println("\tlevel " + level + ": " +
                           cacheHits[level] + "/" + cacheAccesses[level] + "/" +
                           ((float)cacheHits[level]/cacheAccesses[level]) +
                           " (hits/accesses/hit rate)");
        System.err.println("\t\t" + cache[level].getStats().
                                    replace('\n', ' ').replace('\t', ' '));
      }
    }
  }
}
