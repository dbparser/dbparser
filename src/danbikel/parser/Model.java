package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * This class computes the probability of generating an output element
 * of this parser, where an output element might be, for example, a word,
 * a part of speech tag, a nonterminal label or a subcat frame.  It derives
 * counts from top-level <code>TrainerEvent</code> objects, storing these
 * derived counts in its internal data structures.  The derived counts are
 * necessary for the smoothing of the top-level probabilities used by the
 * parser, and the particular structure of those levels of smoothing (or,
 * less accurately, back-off) are specified by the
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
  private final static boolean verboseDebug = false;
  protected final static boolean precomputeProbs =
    Boolean.valueOf(Settings.get(Settings.precomputeProbs)).booleanValue();
  private final static boolean deficientEstimation;
  static {
    String deficientEstimationString =
      Settings.get(Settings.collinsDeficientEstimation);
    deficientEstimation =
      Boolean.valueOf(deficientEstimationString).booleanValue();
  }
  protected final static boolean useCache = true;
  private final static int minCacheSize = 1000;
  private final static boolean doGCBetweenCanonicalizations = false;
  private final static boolean saveBackOffMap = false;

  private final static int structureMapArrSize = 1000;

  protected final static Symbol baseNPLabel = Language.treebank().baseNPLabel();

  // data members

  /** The probability structure for this model to use. */
  protected ProbabilityStructure structure;
  // the prob structures for individual clients in a multithreaded environment
  private ProbabilityStructure[] structureMapArr =
    new ProbabilityStructure[structureMapArrSize];
  private Map structureMap = new danbikel.util.HashMap();
  private IntCounter idInt = new IntCounter();
  // some handles on info available from structure object
  protected String structureClassName;
  protected String shortStructureClassName;
  protected int numLevels;
  protected double[] lambdaFudge;
  protected double[] lambdaFudgeTerm;
  protected double[] lambdaPenalty;
  protected double[] logOneMinusLambdaPenalty;
  // the actual counts
  protected CountsTrio[] counts;
  private int numCanonicalizableEvents = 0;
  /** Indicates whether to report to stderr what this class is doing. */
  protected boolean verbose = true;

  // for the storage of precomputed probabilities and lambdas
  protected HashMapDouble[] precomputedProbs;
  protected HashMapDouble[] precomputedLambdas;
  protected transient int[] precomputedProbHits;
  protected transient int precomputedProbCalls;
  protected transient int[] precomputedNPBProbHits;
  protected transient int precomputedNPBProbCalls;
  /**
   * A set of {@link #numLevels}<code>&nbsp;-&nbsp;1</code> maps, where map
   * <i>i</i> is a map from back-off level <i>i</i> transitions to
   * <i>i</i>&nbsp;+&nbsp;1 transitions.  These maps are only used temporarily
   * when precomputing probs (and are necessary for incremental training).
   *
   * @see #savePrecomputeData(CountsTable,Filter)
   */
  protected java.util.HashMap[] backOffMap;

  // for temporary storage of histories (so we don't have to copy histories
  // created by deriveHistories() to create transition objects)
  protected transient ProbabilityCache topLevelCache;
  protected transient ProbabilityCache[] cache;
  protected transient int[] cacheHits;
  protected transient int[] cacheAccesses;

  protected transient FlexibleMap canonicalEvents;

  protected transient String smoothingParamsFile;
  protected transient boolean saveSmoothingParams;
  protected transient boolean dontAddNewParams;
  protected transient boolean useSmoothingParams;
  protected transient CountsTable[] smoothingParams;

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
    int structureClassNameBegin =
      structureClassName.lastIndexOf('.') + 1;
    shortStructureClassName =
      structureClassName.substring(structureClassNameBegin);
    numLevels = structure.numLevels();
    counts = new CountsTrio[numLevels];
    for (int i = 0; i < counts.length; i++)
      counts[i] = new CountsTrio();
    lambdaFudge = new double[numLevels];
    lambdaPenalty = new double[numLevels];
    logOneMinusLambdaPenalty = new double[numLevels];
    for (int i = 0; i < lambdaFudge.length; i++) {
      lambdaFudge[i] = structure.lambdaFudge(i);
      lambdaPenalty[i] = structure.lambdaPenalty(i);
      logOneMinusLambdaPenalty[i] = Math.log(1 - lambdaPenalty[i]);
    }
    lambdaFudgeTerm = new double[numLevels];

    if (precomputeProbs)
      setUpPrecomputedProbTables();

    setUpSmoothingParamsSettings();

    for (int i = 0; i < lambdaFudgeTerm.length; i++)
      lambdaFudgeTerm[i] = structure.lambdaFudgeTerm(i);
    if (useCache)
      setUpCaches();
    if (precomputeProbs)
      setUpPrecomputeProbStatTables();
  }

  private void setUpSmoothingParamsSettings() {
    smoothingParamsFile = structure.smoothingParametersFile();
    String smoothingParamsDir = Settings.get(Settings.smoothingParamsDir);
    if (smoothingParamsDir != null)
      smoothingParamsFile =
        smoothingParamsDir + File.separator + smoothingParamsFile;
    saveSmoothingParams = Settings.getBoolean(Settings.saveSmoothingParams) ||
                          structure.saveSmoothingParameters();
    dontAddNewParams = Settings.getBoolean(Settings.dontAddNewParams) ||
                       structure.dontAddNewParameters();
    useSmoothingParams = Settings.getBoolean(Settings.useSmoothingParams) ||
                         structure.useSmoothingParameters();

  }

  private void setUpPrecomputedProbTables() {
    precomputedProbs = new HashMapDouble[numLevels];
    for (int i = 0; i < precomputedProbs.length; i++)
      precomputedProbs[i] = new HashMapDouble();
    precomputedLambdas = new HashMapDouble[numLevels - 1];
    for (int i = 0; i < precomputedLambdas.length; i++)
      precomputedLambdas[i] = new HashMapDouble();

    backOffMap = new java.util.HashMap[numLevels - 1];
    for (int i = 0; i < backOffMap.length; i++) {
      backOffMap[i] = new java.util.HashMap();
    }
  }

  private void setUpPrecomputeProbStatTables() {
    precomputedProbHits = new int[numLevels];
    precomputedNPBProbHits = new int[numLevels];
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

  public void setCanonicalEvents(FlexibleMap canonical) {
    canonicalEvents = canonical;
  }

  /**
   * Derives all counts from the specified counts table, using the
   * probability structure specified in the constructor.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param threshold a (currently unused) count cut-off threshold
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter,
			   double threshold, FlexibleMap canonical) {
    deriveCounts(trainerCounts, filter, threshold, canonical, false);
  }

  /**
   * Derives all counts from the specified counts table, using the
   * probability structure specified in the constructor.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param threshold a (currently unused) count cut-off threshold
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   * @param deriveOtherModelCounts an unused parameter, as this class
   * does not contain other, internal <code>Model</code> instances
   */
  public void deriveCounts(CountsTable trainerCounts, Filter filter,
			   double threshold, FlexibleMap canonical,
			   boolean deriveOtherModelCounts) {
    if (useSmoothingParams || dontAddNewParams)
      readSmoothingParams();
    setCanonicalEvents(canonical);
    //deriveHistories(trainerCounts, filter, canonical);

    Time time = null;
    if (verbose)
      time = new Time();

    Transition trans = new Transition(null, null);

    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (!filter.pass(event))
	continue;

      // store all transitions for all levels
      for (int level = 0; level < numLevels; level++) {
        Event history = structure.getHistory(event, level);
        if (useSmoothingParams || dontAddNewParams)
          if (smoothingParams[level].containsKey(history) == false)
            continue;
        history = canonicalizeEvent(history, canonical);
        counts[level].history().add(history, CountsTrio.hist, count);

        Event future = structure.getFuture(event, level);
        trans.setFuture(canonicalizeEvent(future, canonical));
        trans.setHistory(history);

        if (verboseDebug)
          System.err.println(shortStructureClassName +
                             "(" + level + "): " + trans +
                             "; count=" + (float)count);

        if (counts[level].transition().count(trans) == 0)
          counts[level].history().add(trans.history(), CountsTrio.diversity);
        counts[level].transition().add(getCanonical(trans, canonical), count);
      }
    }

    if (verbose)
      System.err.println("Derived transitions for " + structureClassName +
			 " in " + time + ".");

    /*
    if (threshold > 1) {
      pruneHistoriesAndTransitions(threshold);
    }
    */

    //deriveDiversityCounts();

    /*
    if (precomputeProbs)
      precomputeProbs(trainerCounts, filter);
    */
   if (precomputeProbs)
     savePrecomputeData(trainerCounts, filter);

   /*
   if (structure.doCleanup())
     cleanup();
   */
  }

  protected void cleanup() {
    int numHistoriesRemoved = 0;
    int numTransitionsRemoved = 0;
    Time time = null;
    if (verbose)
      time = new Time();
    if (precomputeProbs) {
      int lastLevel = numLevels - 1;
      for (int level = 0; level < numLevels; level++) {
	Iterator it = precomputedProbs[level].keySet().iterator();
	while (it.hasNext()) {
	  if (structure.removeTransition(level, (Transition)it.next())) {
	    it.remove();
	    numTransitionsRemoved++;
	  }
	}
	if (level < lastLevel) {
	  it = precomputedLambdas[level].keySet().iterator();
	  while (it.hasNext()) {
	    if (structure.removeHistory(level, (Event)it.next())) {
	      it.remove();
	      numHistoriesRemoved++;
	    }
	  }
	}
      }
    }
    else {
      for (int level = 0; level < numLevels; level++) {
	BiCountsTable histories = counts[level].history();
	Iterator it = histories.keySet().iterator();
	while (it.hasNext())
	  if (structure.removeHistory(level, (Event)it.next())) {
	    it.remove();
	    numHistoriesRemoved++;
	  }
	CountsTable transitions = counts[level].transition();
	it = transitions.keySet().iterator();
	while (it.hasNext())
	  if (structure.removeTransition(level, (Transition)it.next())) {
	    it.remove();
	    numTransitionsRemoved++;
	  }
      }
    }
    if (verbose)
      System.err.println("Cleaned up in " + time + "; removed " +
			 numHistoriesRemoved + " histories and " +
			 numTransitionsRemoved + " transitions.");
  }

  /**
   * This method first canonicalizes the information in the specified event
   * (a Sexp or a Subcat and a Sexp), then it returns a canonical version
   * of the event itself, copying it into the map if necessary.
   */
  protected final static Event canonicalizeEvent(Event event,
						 FlexibleMap canonical) {
    Event canonicalEvent = (Event)canonical.get(event);
    if (canonicalEvent == null) {
      canonicalEvent = event.copy();
      canonicalEvent.canonicalize(canonical);
      canonical.put(canonicalEvent, canonicalEvent);
    }
    return canonicalEvent;
  }

  /**
   * This method assumes trans already contains a canonical history and a
   * canonical future.  If an equivalent transition is found in the canonical
   * map, it is returned; otherwise, a new Transition object is created
   * with the canonical future and canonical history contained in the specified
   * transition, and that new Transition object is added to the canonical map
   * and returned.
   */
  protected final static Transition getCanonical(Transition trans,
						 FlexibleMap canonical) {
    Transition canonicalTrans = (Transition)canonical.get(trans);
    if (canonicalTrans == null) {
      canonicalTrans = new Transition(trans.future(), trans.history());
      canonical.put(canonicalTrans, canonicalTrans);
    }
    return canonicalTrans;
  }

  /**
   *
   */
  public double estimateLogProb(int id, TrainerEvent event) {
    ProbabilityStructure clientStructure = getClientProbStructure(id);
    return (precomputeProbs ?
	    estimateLogProbUsingPrecomputed(clientStructure, event) :
	    Math.log(estimateProb(clientStructure, event)));
  }

  public double estimateProb(int id, TrainerEvent event) {
    if (precomputeProbs)
      throw
	new UnsupportedOperationException("precomputed probs are in log-space");
    ProbabilityStructure clientStructure = getClientProbStructure(id);
    return estimateProb(clientStructure, event);
  }

  private final ProbabilityStructure getClientProbStructure(int id) {
    ProbabilityStructure clientStructure = null;

    if (id < structureMapArrSize) {
      clientStructure = structureMapArr[id];
      if (clientStructure == null)
	structureMapArr[id] = (clientStructure = structure.copy());
    }
    else {
      IntCounter localIdInt = idInt;
      synchronized (structureMap) {
	if (structureMap.size() > 1) {
	  localIdInt.set(id);
	  clientStructure = (ProbabilityStructure)structureMap.get(localIdInt);
	  if (clientStructure == null) {
	    clientStructure = structure.copy();
	    structureMap.put(new IntCounter(id), clientStructure);
	  }
	}
      }
    }
    return clientStructure;
  }

  /**
   * Estimates the log prob using precomputed probabilities and lambdas.
   */
  protected double estimateLogProbUsingPrecomputed(ProbabilityStructure
						     structure,
						   TrainerEvent event) {
    boolean npbParent = event.parent() == baseNPLabel;

    precomputedProbCalls++;
    if (npbParent)
      precomputedNPBProbCalls++;

    MapToPrimitive.Entry transEntry = null, lambdaEntry = null;
    double logLambda = 0.0;
    int lastLevel = numLevels - 1;
    /*
    Transition lastLevelTrans = structure.getTransition(event, lastLevel);
    if (precomputedProbs[lastLevel].getEntry(lastLevelTrans) == null)
      return Constants.logOfZero;
    */
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      transEntry = precomputedProbs[level].getEntry(transition);
      if (transEntry != null) {
	precomputedProbHits[level]++;
	if (npbParent)
	  precomputedNPBProbHits[level]++;
	return logLambda + transEntry.getDoubleValue();
      }
      else if (level < lastLevel) {
	Event history = transition.history();
	lambdaEntry = precomputedLambdas[level].getEntry(history);
	logLambda += (lambdaEntry == null ? logOneMinusLambdaPenalty[level] :
		      lambdaEntry.getDoubleValue());
      }
    }
    return Constants.logOfZero;
  }

  /**
   * Returns the smoothed probability estimate of a transition contained
   * in the specified <code>TrainerEvent</code> object.
   *
   * @param probStructure a <code>ProbabilityStructure</code> object that
   * is either {@link #structure} or a copy of it, used for temporary
   * storage during the computation performed by this method
   * @param event the <code>TrainerEvent</code> containing a transition
   * from a history to a future whose smoothed probability is to be computed
   * @return the smoothed probability estimate of a transition contained
   * in the specified <code>TrainerEvent</code> object
   */
  protected double estimateProb(ProbabilityStructure probStructure,
				TrainerEvent event) {
    ProbabilityStructure structure = probStructure;
    if (Debug.level >= 20) {
      System.err.println(structureClassName + "\n\t" + event);
    }

    /*
    if (useCache) {
      Double cacheProb = topLevelCache.getProb(event);
      if (cacheProb != null)
	return cacheProb.doubleValue();
    }
    */
    int highestCachedLevel = numLevels;
    int lastLevel = numLevels - 1;

    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    //structure.prevHistCount = 0;
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      Event history = transition.history();

      // check cache here!!!!!!!!!!!!
      if (useCache) {
	cacheAccesses[level]++;
	if (Debug.level >= 21) {
	  System.err.print("level " + level + ": getting cached  P");
	}
	MapToPrimitive.Entry cacheProbEntry = cache[level].getProb(transition);
	if (cacheProbEntry != null) {
	  if (Debug.level >= 21) {
	    System.err.println(cacheProbEntry);
	  }
	  estimates[level] = cacheProbEntry.getDoubleValue();
	  cacheHits[level]++;
	  highestCachedLevel = level;
	  break;
	}
	else {
	  if (Debug.level >= 21) {
	    System.err.print(transition);
	    System.err.println("=null");
	  }
	}
      }
      CountsTrio trio = counts[level];
      MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
      double historyCount = (histEntry == null ? 0.0 :
			     histEntry.getDoubleValue(CountsTrio.hist));
      double transitionCount = trio.transition().count(transition);
      double diversityCount = (histEntry == null ? 0.0 :
			       histEntry.getDoubleValue(CountsTrio.diversity));

      double lambda, estimate; //, adjustment = 1.0;
      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null) {
          lambda = smoothingParamEntry.getDoubleValue();
	  estimate = transitionCount / historyCount;
	}
	else {
	  lambda = lambdaPenalty[level];
	  estimate = 0;
	}
      }
      else if (historyCount == 0) {
	lambda = lambdaPenalty[level];
	estimate = 0;
      }
      else {
	/*
	if (structure.prevHistCount <= historyCount)
	  adjustment = 1 - structure.prevHistCount / historyCount;
	*/
	lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		  historyCount /
		  (historyCount + fudgeTerm + fudge * diversityCount));
	  //adjustment * (historyCount / (historyCount + fudge * diversityCount));
	estimate = transitionCount / historyCount;
      }

      if (Debug.level >= 20) {
	for (int i = 0; i < level; i++)
	  System.err.print("  ");
	/*
	System.err.println(transitionCount + "    " +
			   historyCount + "    " + diversityCount + "    " +
			   adjustment + "    " + estimate);
	*/
	System.err.println(transitionCount + "    " +
			   historyCount + "    " + diversityCount + "    " +
			   lambda + "    " + estimate);
      }

      lambdas[level] = lambda;
      estimates[level] = estimate;
      //structure.prevHistCount = historyCount;
    }
    double prob = Constants.probImpossible;
    if (useCache) {
      prob = (highestCachedLevel == numLevels ?
	      Constants.probImpossible : estimates[highestCachedLevel]);
    }
    for (int level = highestCachedLevel - 1; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (useCache  && prob > Constants.probImpossible) {
	Transition transition = structure.transitions[level];
	if (Debug.level >= 21) {
	  System.err.println("adding P" + transition + " = " + prob +
			     " to cache at level " + level);
	}

	putInCache:
	if (!cache[level].containsKey(transition)) {
	  numCacheAdds++;
	  Transition canonTrans = (Transition)canonicalEvents.get(transition);
	  if (canonTrans != null) {
	    cache[level].put(canonTrans, prob);
	    numCanonicalHits++;
	  }
	  else {
	    // don't want to copy history and future if we don't have to
	    Event transHist = transition.history();
	    Event transFuture = transition.future();
	    Event hist = (Event)canonicalEvents.get(transHist);
	    if (hist == null) {
	      //break putInCache;
	      hist = transHist.copy();
	      //System.err.println("no hit: " + hist);
	    }
	    else {
	      numCanonicalHits++;
	      //System.err.println("hit: " + hist);
	    }
	    Event future = (Event)canonicalEvents.get(transFuture);
	    if (future == null) {
	      //break putInCache;
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
    // check cache here!!!!!!!!!!!!!!!!!!!!!!!

    Transition transition = structure.getTransition(event, level);
    Event history = transition.history();
    CountsTrio trio = counts[level];
    MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
    double historyCount = (histEntry == null ? 0.0 :
			   histEntry.getDoubleValue(CountsTrio.hist));
    double transitionCount = trio.transition().count(transition);
    double diversityCount = (histEntry == null ? 0.0 :
			     histEntry.getDoubleValue(CountsTrio.diversity));

    double lambda, estimate, adjustment = 1.0;
    double fudge = structure.lambdaFudge(level);
    double fudgeTerm = structure.lambdaFudgeTerm(level);
    if (historyCount == 0.0) {
      lambda = lambdaPenalty[level];
      estimate = 0.0;
    }
    else {
      if (prevHistCount <= historyCount)
	adjustment = 1 - prevHistCount / historyCount;
      lambda =
	adjustment * (historyCount /
		      (historyCount + fudgeTerm + fudge * diversityCount));
      estimate = transitionCount / historyCount;
    }
    double backOffProb = estimateProbOld(structure,
				      event, level + 1, historyCount);
    double probVal = (lambda * estimate) + ((1 - lambda) * backOffProb);

    // cache probVal here!!!!!!!!!!!!!!!!!!!!!

    return probVal;
  }

  /**
   * Called by
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}, for each
   * type of transition observed, this method derives the number of
   * unique transitions from the history context to the possible
   * futures.  This number of unique transitions, called the
   * <i>diversity</i> of a random variable, is used in a modified
   * version of Witten-Bell smoothing.
   *
   * @deprecated This method used to be called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}, but
   * diversity counts are now derived directly by that method.
   */
  protected void deriveDiversityCounts() {
    // derive diversity counts for all levels
    Time time = null;
    if (verbose)
      time = new Time();

    for (int level = 0; level < numLevels; level++) {
      Iterator it = counts[level].transition().keySet().iterator();
      while (it.hasNext()) {
	Transition transition = (Transition)it.next();
	counts[level].history().add(transition.history(), CountsTrio.diversity);
      }
    }

    if (verbose)
      System.err.println("Derived diversity counts for " + structureClassName +
			 " in " + time + ".");
  }

  /**
   * Derives all history-context counts from the specified counts table, using
   * this <code>Model</code> object's probability structure.
   *
   * @param trainerCounts a map from {@link TrainerEvent} objects to
   * their counts (as <code>double</code>s) from which to derive counts
   * @param filter used to filter out <code>TrainerEvent</code> objects
   * whose derived counts should not be derived for this model
   * @param canonical a reflexive map used to canonicalize objects
   * created when deriving counts
   *
   * @deprecated This method used to be called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}, but
   * histories are now derived directly by that method.
   */
  protected void deriveHistories(CountsTable trainerCounts, Filter filter,
				 FlexibleMap canonical) {
    Time time = null;
    if (verbose)
      time = new Time();
    Iterator entries = trainerCounts.entrySet().iterator();
    while (entries.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (!filter.pass(event))
	continue;
      // store all histories for all back-off levels
      for (int level = 0; level < numLevels; level++) {
	Event history = structure.getHistory(event, level);
        if (useSmoothingParams || dontAddNewParams)
          if (smoothingParams[level].containsKey(history) == false)
            continue;
	history = canonicalizeEvent(history, canonical);
	counts[level].history().add(history, CountsTrio.hist, count);
      }
    }

    if (verbose)
      System.err.println("Derived histories for " + structureClassName +
			 " in " + time + ".");
  }

  private void pruneHistoriesAndTransitions(int threshold) {
    for (int level = 0; level < numLevels; level++) {
      Iterator it = counts[level].transition().entrySet().iterator();
      while (it.hasNext()) {
	MapToPrimitive.Entry transEntry = (MapToPrimitive.Entry)it.next();
	Transition trans = (Transition)transEntry.getKey();
	double transCount = transEntry.getDoubleValue();
	if (transCount < threshold) {
	  MapToPrimitive.Entry histEntry =
	    counts[level].history().getEntry(trans.history());
	  histEntry.add(CountsTrio.hist, -transCount);
	  if (histEntry.getDoubleValue(CountsTrio.hist) <= 0.0) {
	    if (histEntry.getDoubleValue(CountsTrio.hist) < 0.0)
	      System.err.println("yikes!!!");
	    counts[level].history().remove(histEntry.getKey());
	  }
	  it.remove();
	}
      }
    }
  }

  protected void initializeSmoothingParams() {
    smoothingParams = new CountsTableImpl[numLevels];
    for (int i = 0; i < numLevels; i++)
      smoothingParams[i] = new CountsTableImpl();
  }

  /**
   * Saves the back-off chain for each event derived from each
   * <code>TrainerEvent</code> in the key set of the specified counts table.
   * This method is called by {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)} when
   * {@link Settings#precomputeProbs} is <code>true</code>.
   *
   * @param trainerCounts a counts table containing some or all of the
   * <code>TrainerEvent</code> objects collected during training
   * @param filter a filter specifying which <code>TrainerEvent</code>
   * objects to ignore in the key set of the specified counts table
   *
   * @see #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)
   * @see #backOffMap
   */
  protected void savePrecomputeData(CountsTable trainerCounts, Filter filter) {
    Transition oldTrans;
    Transition currTrans = null;
    Iterator keys = trainerCounts.keySet().iterator();
    while (keys.hasNext()) {
      TrainerEvent event = (TrainerEvent)keys.next();
      if (!filter.pass(event))
        continue;
      for (int level = 0; level < numLevels; level++) {
	oldTrans = currTrans;
        Transition transition = structure.getTransition(event, level);
        Event history = transition.history();

        CountsTrio trio = counts[level];
        MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
        MapToPrimitive.Entry transEntry = trio.transition().getEntry(transition);
        // take care of case where history was removed due to its low count
        // or because we are using smoothing parameters gotten from a file
        if (histEntry == null)
          continue;

        currTrans = (Transition)transEntry.getKey();
        if (level > 0) {
          backOffMap[level - 1].put(oldTrans, currTrans);
	}
      }
    }
  }

  /**
   * Precomputes all probabilities and smoothing values for events seen during
   * all previous invocations of {@link
   * #deriveCounts(CountsTable,Filter,double,FlexibleMap,boolean)}.
   *
   * @see #precomputeProbs(MapToPrimitive.Entry,double[],double[],
   * Transition[],Event[],int) precomputeProbs(MapToPrimitive.Entry, ...)
   * @see #storePrecomputedProbs(double[],double[],Transition[],Event[],int)
   * storePrecomputedProbs
   */
  public void precomputeProbs() {
    if (!precomputeProbs)
      return;

    Time time = null;
    if (verbose)
      time = new Time();

    if (saveSmoothingParams && smoothingParams == null)
      initializeSmoothingParams();
    else if (useSmoothingParams)
      readSmoothingParams(); // only reads from file if smoothingParams != null

    // go through all transitions at each level of counts array, grabbing
    // histories and getting to next level via backOffMap

    Transition[] transitions = new Transition[numLevels];
    Event[] histories = new Event[numLevels];

    int lastLevel = numLevels - 1;

    Iterator topLevelTrans = counts[0].transition().entrySet().iterator();
    while (topLevelTrans.hasNext()) {
      MapToPrimitive.Entry transEntry =
	(MapToPrimitive.Entry)topLevelTrans.next();
      double[] lambdas = structure.lambdas;
      double[] estimates = structure.estimates;
      precomputeProbs(transEntry, lambdas, estimates, transitions, histories,
		      lastLevel);
      storePrecomputedProbs(lambdas, estimates, transitions, histories,
			    lastLevel);
    }
    if (!saveBackOffMap)
      backOffMap = null; // no longer needed!
    if (structure.doCleanup())
      cleanup();
    if (saveSmoothingParams) {
      writeSmoothingParams();
      smoothingParams = null;
    }
    if (verbose)
      System.err.println("Precomputed probabilities for " +
			 structureClassName + " in " + time + ".");
  }

  /**
   * Precomputes the probabilities and smoothing values for the
   * {@link Transition} object contained as a key within the specified
   * map entry, where the value is the count of the transition.
   *
   * @param transEntry a map entry mapping a <code>Transition</code>
   * object to its count (a <code>double</code>)
   * @param lambdas an array in which to store the smoothing value for
   * each of the back-off levels
   * @param estimates an array in which to store the maximum-likelihood
   * estimate at each of the back-off levels
   * @param transitions an array in which to store the <code>Transition</code>
   * instance for each of the back-off levels
   * @param histories an array in which to store the history, an
   * <code>Event</code> instance, for each of the back-off levels
   * @param lastLevel the last back-off level (the value equal to
   * {@link #numLevels}<code>&nbsp;-&nbsp;1</code>)
   *
   * @see #precomputeProbs()
   */
  protected void precomputeProbs(MapToPrimitive.Entry transEntry,
				 double[] lambdas,
				 double[] estimates,
				 Transition[] transitions,
				 Event[] histories,
				 int lastLevel) {
    for (int level = 0; level < numLevels; level++) {
      Transition currTrans = (Transition)transEntry.getKey();
      Event history = currTrans.history();
      MapToPrimitive.Entry histEntry =
	(MapToPrimitive.Entry)counts[level].history().getEntry(history);

      if (histEntry == null)
	System.err.println("yikes! something is very wrong");

      transitions[level] = currTrans;
      histories[level] = (Event)histEntry.getKey();
	
      double historyCount = histEntry.getDoubleValue(CountsTrio.hist);
      double transitionCount = transEntry.getDoubleValue();
      double diversityCount = histEntry.getDoubleValue(CountsTrio.diversity);

      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      double lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		       historyCount /
		       (historyCount + fudgeTerm + fudge * diversityCount));
      if (useSmoothingParams) {
	MapToPrimitive.Entry smoothingParamEntry =
	  smoothingParams[level].getEntry(history);
	if (smoothingParamEntry != null)
	  lambda = smoothingParamEntry.getDoubleValue();
	else
	  System.err.println("uh-oh: couldn't get smoothing param entry " +
			     "for " + history);
      }
      double estimate = transitionCount / historyCount;
      lambdas[level] = lambda;
      estimates[level] = estimate;

      if (level < lastLevel) {
	Transition nextLevelTrans  =
	  (Transition)backOffMap[level].get(currTrans);
	transEntry = counts[level + 1].transition().getEntry(nextLevelTrans);
      }
    }
  }

  /**
   * Stores the specified smoothing values (lambdas) and smoothed probability
   * estimates in the {@link #precomputedProbs} and {@link #smoothingParams}
   * map arrays.
   *
   * @param lambdas an array containing the smoothing value for each of the
   * back-off levels
   * @param estimates an array containing the maximum-likelihood estimate at
   * each of the back-off levels
   * @param transitions an array containing the <code>Transition</code>
   * instance for each of the back-off levels
   * @param histories an array in which to store the history, an
   * <code>Event</code> instance, for each of the back-off levels
   * @param lastLevel the last back-off level (the value equal to
   * {@link #numLevels}<code>&nbsp;-&nbsp;1</code>)
   *
   * @see #precomputeProbs()
   */
  protected void storePrecomputedProbs(double[] lambdas,
				       double[] estimates,
				       Transition[] transitions,
				       Event[] histories,
				       int lastLevel) {
    double prob = 0.0;
    for (int level = lastLevel; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (transitions[level] != null)
	precomputedProbs[level].put(transitions[level], Math.log(prob));
      if (level < lastLevel && histories[level] != null)
	precomputedLambdas[level].put(histories[level], Math.log(1 - lambda));
      if (saveSmoothingParams)
	smoothingParams[level].put(histories[level], lambda);
    }
  }

  /**
   * Stores precomputed probabilities and smoothing values for events derived
   * from the maximal-context <code>TrainerEvent</code> instances and their
   * counts contained in the specified counts table.
   *
   * @param trainerCounts a map of <code>TrainerEvent</code> instances
   * to their observed counts
   * @param filter a filter indicating which of the <code>TrainerEvent</code>
   * objects in the specified counts table should be ignored by this method
   * as it iterates over all entires in the counts table
   *
   * @deprecated This method has been superseded by {@link #precomputeProbs()}.
   */
  protected void precomputeProbs(CountsTable trainerCounts, Filter filter) {
    Time time = null;
    if (verbose)
      time = new Time();
    if (saveSmoothingParams && smoothingParams == null)
      initializeSmoothingParams();
    else if (useSmoothingParams)
      readSmoothingParams(); // only reads from file if smoothingParams != null
    Transition[] transitions = new Transition[numLevels];
    Event[] histories = new Event[numLevels];
    Iterator keys = trainerCounts.keySet().iterator();
    while (keys.hasNext()) {
      TrainerEvent event = (TrainerEvent)keys.next();
      if (!filter.pass(event))
	continue;
      precomputeProbs(event, transitions, histories);
    }
    if (saveSmoothingParams) {
      writeSmoothingParams();
      smoothingParams = null;
    }
    //counts = null;
    if (verbose)
      System.err.println("Precomputed probabilities and lambdas in " + time +
			 ".");
  }

  /**
   * Precomputes probabilities for the specified event, using the specified
   * arrays as temporary storage during this method invocation.
   *
   * @param event the <code>TrainerEvent</code> object from which probabilities
   * are to be precomputed
   * @param transitions temporary storage to be used during an invocation
   * of this method
   * @param histories temporary storage to be used during an invocation
   * of this method
   *
   * @deprecated This method is called by {@link
   * #precomputeProbs(CountsTable,Filter)}, which is also deprecated.
   */
  protected void precomputeProbs(TrainerEvent event,
				 Transition[] transitions, Event[] histories) {
    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    int lastLevel = numLevels - 1;
    for (int level = 0; level < numLevels; level++) {
      Transition transition = structure.getTransition(event, level);
      Event history = transition.history();

      CountsTrio trio = counts[level];
      MapToPrimitive.Entry histEntry = trio.history().getEntry(history);
      MapToPrimitive.Entry transEntry = trio.transition().getEntry(transition);

      // take care of case where history was removed due to its low count
      // or because we are using smoothing parameters gotten from a file
      if (histEntry == null) {
	estimates[level] = lambdas[level] = 0.0;
	histories[level] = null;
	transitions[level] = null;
	continue;
      }

      // the keys of the map entries are guaranteed to be canonical
      histories[level] = (Event)histEntry.getKey();
      transitions[level] =
	transEntry == null ? null : (Transition)transEntry.getKey();

      double historyCount = histEntry.getDoubleValue(CountsTrio.hist);
      double transitionCount =
	transEntry == null ? 0.0 : transEntry.getDoubleValue();
      double diversityCount = histEntry.getDoubleValue(CountsTrio.diversity);

      double fudge = lambdaFudge[level];
      double fudgeTerm = lambdaFudgeTerm[level];
      double lambda = ((!deficientEstimation && level == lastLevel) ? 1.0 :
		       historyCount /
		       (historyCount + fudgeTerm + fudge * diversityCount));
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null)
          lambda = smoothingParamEntry.getDoubleValue();
      }
      double estimate = transitionCount / historyCount;
      lambdas[level] = lambda;
      estimates[level] = estimate;
    }

    double prob = 0.0;
    for (int level = lastLevel; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = lambda * estimate + ((1 - lambda) * prob);
      if (transitions[level] != null)
	precomputedProbs[level].put(transitions[level], Math.log(prob));
      if (level < lastLevel && histories[level] != null)
	precomputedLambdas[level].put(histories[level], Math.log(1 - lambda));
      if (saveSmoothingParams)
        smoothingParams[level].put(histories[level], lambda);
    }
  }

  // I/O methods for smoothing parameters file

  protected void readSmoothingParams() {
    readSmoothingParams(true);
  }

  protected void readSmoothingParams(boolean verboseOutput) {
    if (smoothingParams != null)
      return;
    try {
      if (verboseOutput)
	System.err.println("Reading smoothing parameters from \"" +
			   smoothingParamsFile + "\" for " +
			   structureClassName + ".");
      int bufSize = Constants.defaultFileBufsize;
      BufferedInputStream bis =
        new BufferedInputStream(new FileInputStream(smoothingParamsFile),
                                bufSize);
      ObjectInputStream ois = new ObjectInputStream(bis);
      smoothingParams = (CountsTable[])ois.readObject();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
  }

  protected void writeSmoothingParams() {
    writeSmoothingParams(true);
  }

  protected void writeSmoothingParams(boolean verboseOutput) {
    try {
      if (verboseOutput)
	System.err.println("Writing smoothing parameters to \"" +
			   smoothingParamsFile + "\" for " +
			   structureClassName + ".");
      int bufSize = Constants.defaultFileBufsize;
      BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(smoothingParamsFile),
                                 bufSize);
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(smoothingParams);
      oos.close();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  /**
   * Indicates to use counts or precomputed probabilities from the specified
   * back-off level of this model when estimating probabilities for the
   * specified back-off level of another model.<br>
   * <b>N.B.</b>: Note that invoking this method <b>destructively alters the
   * specified <code>Model</code></b>.
   *
   * @param backOffLevel the back-off level of this model that is to be
   * shared by another model
   * @param otherModel the other model that will share a particular
   * back-off level with this mdoel (that is, use the counts or precomputed
   * probabilities from this model)
   * @param otherModelBackOffLevel the back-off level of the other model
   * that is to be made the same as the specified back-off level
   * of this model (that is, use the counts or precomputed probabilities
   * from this model)
   */
  public void share(int backOffLevel,
                    Model otherModel, int otherModelBackOffLevel) {
    if (precomputeProbs) {
      otherModel.precomputedProbs[otherModelBackOffLevel] =
	this.precomputedProbs[backOffLevel];
      if (backOffLevel < numLevels - 1 &&
	  otherModelBackOffLevel < otherModel.numLevels - 1)
	otherModel.precomputedLambdas[otherModelBackOffLevel] =
	  this.precomputedLambdas[backOffLevel];
    }
    else {
      otherModel.counts[otherModelBackOffLevel] = this.counts[backOffLevel];
    }
  }

  /**
   * Since events are typically read-only, this method will allow for
   * canonicalization (or "unique-ifying") of the information
   * contained in the events contained in this object.  Use of this
   * method is intended to conserve memory by removing duplicate
   * copies of event information in different event objects.
   */
  public void canonicalize() {
    canonicalize(new danbikel.util.HashMap());
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
  public void canonicalize(FlexibleMap map) {
    setCanonicalEvents(map);
    int prevMapSize = map.size();
    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalize(trio.history(), map);
      //canonicalize(trio.unique(), map);
      canonicalize(trio.transition(), map);
    }

    if (verbose) {
      System.err.println("Canonicalized Sexp objects; " +
			 "now canonicalizing Event objects");
    }

    for (int level = 0; level < numLevels; level++) {
      CountsTrio trio = counts[level];
      canonicalizeEvents(trio.history(), map);
      //canonicalizeEvents(trio.unique(), map);
      canonicalizeEvents(trio.transition(), map);
    }
    int increase = map.size() - prevMapSize;

    if (verbose) {
      System.err.print("Canonicalized " + numCanonicalizableEvents +
		       " events; map grew by " + increase + " elements to " +
		       map.size());
      if (doGCBetweenCanonicalizations) {
	System.err.print(" (gc ... ");
	System.err.flush();
      }
      else
	System.err.println();
    }
    if (doGCBetweenCanonicalizations) {
      System.gc();
      if (verbose)
	System.err.println("done).");
    }

    // reset for use by this method if it is called again
    numCanonicalizableEvents = 0;
  }

  private void canonicalize(MapToPrimitive table, Map map) {
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

  private void canonicalizeEvents(MapToPrimitive table, Map map) {
    int numKeysReplaced = 0;
    Iterator it = table.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      Object histOrTrans = entry.getKey();
      Object mapHistOrTrans = map.get(histOrTrans);
      boolean alreadyInMap = mapHistOrTrans != null;
      if (alreadyInMap) {
	if (entry.replaceKey(mapHistOrTrans))
	  numKeysReplaced++;
      }
      else
	map.put(histOrTrans, histOrTrans);
    }
    numCanonicalizableEvents += table.size();
    /*
    System.err.println(structureClassName +
		       ": No. of canonicalized Event objects: " +
		       numKeysReplaced);
    */
  }

  // accessors
  /**
   * Returns the type of <code>ProbabilityStructure</code> object used
   * during the invocation of
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
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

  /**
   * Returns this model object.
   * @param idx an unused parameter, as this object does not contain any other,
   * internal <code>Model</code> instances.
   * @return this model object
   */
  public Model getModel(int idx) {
    return this;
  }

  // mutators
  /**
   * Causes this class to be verbose in its output to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
   */
  public void beVerbose() { verbose = true;}
  /**
   * Causes this class not to output anything to <code>System.err</code>
   * during the invocation of its methods, such as
   * {@link #deriveCounts(CountsTable,Filter,double,FlexibleMap)}.
   */
  public void beQuiet() { verbose = false; }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (useCache && !precomputeProbs)
      setUpCaches();
    if (precomputeProbs)
      setUpPrecomputeProbStatTables();
    setUpSmoothingParamsSettings();
    if (useSmoothingParams) {
      System.err.print("reading smoothing parameters...");
      readSmoothingParams(false);
    }
  }

  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
    if (precomputeProbs)
      counts = null;
    s.defaultWriteObject();
  }

  public String getCacheStats() {
    StringBuffer sb = new StringBuffer(300);
    if (precomputeProbs) {
      sb.append("precomputed prob data for ").
	 append(structure.getClass().getName()).append(":\n");
      sb.append("\ttotal No. of calls: ").append(precomputedProbCalls).
	 append("\n");
      int total = 0;
      int sum = 0;
      for (int level = 0; level < precomputedProbHits.length; level++) {
	int hitsThisLevel = precomputedProbHits[level];
	total += hitsThisLevel;
	if (level > 0)
	  sum += hitsThisLevel * level;
	sb.append("\tlevel ").append(level).append(": ").append(hitsThisLevel).
	   append("\n");
      }
      sb.append("\taverage hit level: ").append((float)sum/total).append("\n");

      sb.append("\ttotal No. of NPB calls: ").append(precomputedNPBProbCalls).
	 append("\n");
      total = 0;
      sum = 0;
      for (int level = 0; level < precomputedNPBProbHits.length; level++) {
	int hitsThisLevel = precomputedNPBProbHits[level];
	total += hitsThisLevel;
	if (level > 0)
	  sum += hitsThisLevel * level;
	sb.append("\tNPB level ").append(level).append(": ").
	   append(hitsThisLevel).append("\n");
      }
      sb.append("\taverage NPB hit level: ").append((float)sum/total).
	 append("\n");
    }
    else {
      sb.append("cache data for ").append(structure.getClass().getName()).
	 append(":\n");
      for (int level = 0; level < cacheHits.length; level++) {
	sb.append("\tlevel ").append(level).append(": ").
	   append(cacheHits[level]).append("/").append(cacheAccesses[level]).append("/").
	   append((float)cacheHits[level]/cacheAccesses[level]).
	   append(" (hits/accesses/hit rate)\n");
	sb.append("\t\t").
	   append(cache[level].getStats().
		  replace('\n', ' ').replace('\t', ' ')).
	   append("\n");
      }
    }
    return sb.toString();
  }
}
