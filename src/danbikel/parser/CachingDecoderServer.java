package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.rmi.RemoteException;
import java.util.*;

public class CachingDecoderServer implements DecoderServerRemote {
  protected DecoderServerRemote stub;
  protected ProbabilityCache cache;
  protected int numAccesses = 0;
  protected int numHits = 0;


  public CachingDecoderServer(DecoderServerRemote stub) {
    this.stub = stub;
    String cacheSizeStr = Settings.get(Settings.decoderLocalCacheSize);
    int cacheSize = Integer.parseInt(cacheSizeStr);
    cache = new ProbabilityCache(cacheSize);
  }

  protected void putInCache(TrainerEvent key, double value) {
    cache.put(key.copy(), value);
  }

  public int id() throws RemoteException {
    return stub.id();
  }

  public boolean acceptClientsOnlyByRequest() throws RemoteException {
    return stub.acceptClientsOnlyByRequest();
  }

  public int maxClients() throws RemoteException {
    return stub.maxClients();
  }

  public boolean alive() throws RemoteException {
    return stub.alive();
  }

  public void die(boolean now) throws RemoteException {
    stub.die(now);
  }

  public String host() throws RemoteException {
    return stub.host();
  }

  public Map posMap() throws RemoteException {
    return stub.posMap();
  }

  public Map headToParentMap() throws RemoteException {
    return stub.headToParentMap();
  }

  public Map leftSubcatMap() throws RemoteException {
    return stub.leftSubcatMap();
  }

  public Map rightSubcatMap() throws RemoteException {
    return stub.rightSubcatMap();
  }

  public Map modNonterminalMap() throws RemoteException {
    return stub.modNonterminalMap();
  }

  public CountsTable nonterminals() throws RemoteException {
    return stub.nonterminals();
  }

  public Set prunedPreterms() throws RemoteException {
    return stub.prunedPreterms();
  }

  public Set prunedPunctuation() throws RemoteException {
    return stub.prunedPunctuation();
  }

  /**
   * Returns either the specified word untouched, or a 3-element list as would
   * be created by {@link #convertUnknownWords(SexpList)}.
   *
   * @param originalWord the original word to be (potentially) converted
   * @param index the index of the specified word
   * @return if the specified word is unknown, a 3-element list is returned,
   * as described in {@link #convertUnknownWords(SexpList)}, or, if the
   * specified word is not unknown, then it is returned untouched
   */
  public Sexp convertUnknownWord(Symbol originalWord, int index)
    throws RemoteException {
    return stub.convertUnknownWord(originalWord, index);
  }

  /**
   * Replaces all unknown words in the specified sentence with
   * three-element lists, where the first element is the word itself, the
   * second element is a word-feature vector, as determined by the
   * implementation of {@link WordFeatures#features(Symbol,boolean)}, and
   * the third element is {@link Constants#trueSym} if this word was never
   * observed during training or {@link Constants#falseSym} if it was
   * observed at least once during training.
   *
   * @param sentence a list of symbols representing a sentence to be parsed
   */
  public SexpList convertUnknownWords(SexpList sentence) throws RemoteException {
    return stub.convertUnknownWords(sentence);
  }

  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException {
    return stub.leftSubcatProbStructure();
  }

  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException {
    return stub.rightSubcatProbStructure();
  }

  public ProbabilityStructure modNonterminalProbStructure() throws RemoteException {
    return stub.modNonterminalProbStructure();
  }

  /** Returns a test probability (for debugging purposes). */
  public double testProb() throws RemoteException {
    return stub.testProb();
  }

  /**
   * Returns the prior probability of generating the nonterminal contained
   * in the specified <code>HeadEvent</code>.
   */
  public double logPrior(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logPrior = stub.logPrior(id, event);
      putInCache(event, logPrior);
      return logPrior;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  /**
   * Returns the log of the probability of generating a new head and
   * its left and right subcat frames.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a new head and its
   * left and right subcat frames
   */
  public double logProbHeadWithSubcats(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbHeadWithSubcats = stub.logProbHeadWithSubcats(id, event);
      putInCache(event, logProbHeadWithSubcats);
      return logProbHeadWithSubcats;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbHead(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbHead = stub.logProbHead(id, event);
      putInCache(event, logProbHead);
      return logProbHead;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbLeftSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbLeftSubcat = stub.logProbLeftSubcat(id, event);
      putInCache(event, logProbLeftSubcat);
      return logProbLeftSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbRightSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbRightSubcat = stub.logProbRightSubcat(id, event);
      putInCache(event, logProbRightSubcat);
      return logProbRightSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbSubcat(int id, TrainerEvent event, boolean side)
    throws RemoteException {
    return
      (side == Constants.LEFT ?
       logProbLeftSubcat(id, event) : logProbRightSubcat(id, event));
  }

  /**
   * Returns the log of the probability of generating the head nonterminal
   * of an entire sentence.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating the head nonterminal
   * of an entire sentence
   */
  public double logProbTop(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbTop = stub.logProbTop(id, event);
      putInCache(event, logProbTop);
      return logProbTop;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbMod(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbMod = stub.logProbMod(id, event);
      putInCache(event, logProbMod);
      return logProbMod;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double logProbModNT(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbModNT = stub.logProbModNT(id, event);
      putInCache(event, logProbModNT);
      return logProbModNT;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  /**
   * Returns the log of the probability of generating a gap.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a gap
   */
  public double logProbGap(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double logProbGap = stub.logProbGap(id, event);
      putInCache(event, logProbGap);
      return logProbGap;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  // non-log prob methods
  public double probHead(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probHead = stub.probHead(id, event);
      putInCache(event, probHead);
      return probHead;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }

  public double probMod(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probMod = stub.probMod(id, event);
      putInCache(event, probMod);
      return probMod;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probLeftSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probLeftSubcat = stub.probLeftSubcat(id, event);
      putInCache(event, probLeftSubcat);
      return probLeftSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probRightSubcat(int id, TrainerEvent event)
    throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probRightSubcat = stub.probRightSubcat(id, event);
      putInCache(event, probRightSubcat);
      return probRightSubcat;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
  public double probTop(int id, TrainerEvent event) throws RemoteException {
    numAccesses++;
    MapToPrimitive.Entry entry = cache.getEntry(event);
    if (entry == null) {
      double probTop = stub.probTop(id, event);
      putInCache(event, probTop);
      return probTop;
    }
    else {
      numHits++;
      return entry.getDoubleValue();
    }
  }
}
