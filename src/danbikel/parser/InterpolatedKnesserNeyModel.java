package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Implements a model that uses interpolated Knesser-Ney smoothing.
 */

public class InterpolatedKnesserNeyModel extends Model {
  // constants
  private final static boolean verboseDebug = false;

  // data members
  protected double optimalDiscountEstimate;

  public InterpolatedKnesserNeyModel(ProbabilityStructure structure) {
    super(structure);
  }

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

        boolean newTransition = counts[level].transition().count(trans) == 0;

        counts[level].transition().add(getCanonical(trans, canonical), count);

        if (newTransition)
          counts[level].history().add(trans.history(), CountsTrio.diversity);
        else
          break;
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

    // calculate optimal discount
    // optimalDiscountEstimate = n_1 / n_1 + 2*n_2
    // where n_1 and n_2 are the number of transitions at back-off level 0
    // that occur exactly once and twice, respectively
    entries = counts[0].transition().entrySet().iterator();
    int n1 = 0, n2 = 0;
    while (entries.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)entries.next();
      double count = entry.getDoubleValue(0);
      if (count == 1)
	n1++;
      else if (count == 2)
	n2++;
    }
    if (n1 > 0 && n2 > 0) {
      optimalDiscountEstimate = n1 / (n1 + (2.0 * n2));
      System.err.println("Optimal discount is " + optimalDiscountEstimate);
    }

    if (precomputeProbs)
      precomputeProbs(trainerCounts, filter);

    if (structure.doCleanup())
      cleanup();
  }

  protected double estimateProb(ProbabilityStructure probStructure,
                                TrainerEvent event) {
    ProbabilityStructure structure = probStructure;
    if (Debug.level >= 20) {
      System.err.println(structureClassName + "\n\t" + event);
    }

    int highestCachedLevel = numLevels;
    int lastLevel = numLevels - 1;

    // here, lambda at level i refers to the weight given to the smoothed
    // estimate for level i + 1 (referred to as 1 - lambda in the Witten-Bell
    // code)
    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    for (int level = 0; level < numLevels; level++) {
      double discount = level == lastLevel ? 0 : optimalDiscountEstimate;
      Transition transition = structure.getTransition(event, level);
      Event history = transition.history();

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
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null) {
          lambda = smoothingParamEntry.getDoubleValue();
          estimate = (transitionCount - discount) / historyCount;
        }
        else {
          lambda = 1.0 - lambdaPenalty[level];
          estimate = 0;
        }
      }
      else if (historyCount == 0) {
        lambda = 1.0 - lambdaPenalty[level];
        estimate = 0;
      }
      else {
        lambda = diversityCount * discount / historyCount;
        estimate = (transitionCount - discount) / historyCount;
      }

      if (Debug.level >= 20) {
        for (int i = 0; i < level; i++)
          System.err.print("  ");
        System.err.println(transitionCount + "    " +
                           historyCount + "    " + diversityCount + "    " +
                           lambda + "    " + estimate);
      }

      lambdas[level] = lambda;
      estimates[level] = estimate;
    }
    double prob = Constants.probImpossible;
    if (useCache) {
      prob = (highestCachedLevel == numLevels ?
              Constants.probImpossible : estimates[highestCachedLevel]);
    }
    for (int level = highestCachedLevel - 1; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = estimate + (lambda * prob);
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

    return prob;
  }

  protected void precomputeProbs(TrainerEvent event,
                                 Transition[] transitions, Event[] histories) {
    double[] lambdas = structure.lambdas;
    double[] estimates = structure.estimates;
    int lastLevel = numLevels - 1;
    for (int level = 0; level < numLevels; level++) {
      double discount = level == lastLevel ? 0 : optimalDiscountEstimate;
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
      double lambda = diversityCount * discount / historyCount;
      if (useSmoothingParams) {
        MapToPrimitive.Entry smoothingParamEntry =
          smoothingParams[level].getEntry(history);
        if (smoothingParamEntry != null)
          lambda = smoothingParamEntry.getDoubleValue();
      }
      double estimate = (transitionCount - discount) / historyCount;
      lambdas[level] = lambda;
      estimates[level] = estimate;
    }

    double prob = 0.0;
    for (int level = lastLevel; level >= 0; level--) {
      double lambda = lambdas[level];
      double estimate = estimates[level];
      prob = estimate + lambda * prob;
      if (transitions[level] != null)
        precomputedProbs[level].put(transitions[level], Math.log(prob));
      if (level < lastLevel && histories[level] != null)
        precomputedLambdas[level].put(histories[level], Math.log(lambda));
      if (saveSmoothingParams)
        smoothingParams[level].put(histories[level], lambda);
    }
  }
}
