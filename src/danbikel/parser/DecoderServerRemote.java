package danbikel.parser;

import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.rmi.*;
import java.util.*;

/**
 * Specifies all methods necessary for a decoder client to get its settings
 * and top-level probabilities from a server object.
 */
public interface DecoderServerRemote extends Server {

  public Map posMap() throws RemoteException;

  public Map headToParentMap() throws RemoteException;

  public Map leftSubcatMap() throws RemoteException;

  public Map rightSubcatMap() throws RemoteException;

  public Map modNonterminalMap() throws RemoteException;

  public CountsTable nonterminals() throws RemoteException;

  public Set prunedPreterms() throws RemoteException;

  public Set prunedPunctuation() throws RemoteException;

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
    throws RemoteException;

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
  public SexpList convertUnknownWords(SexpList sentence) throws RemoteException;

  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException;

  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException;

  public ProbabilityStructure modNonterminalProbStructure() throws RemoteException;

  /** Returns a test probability (for debugging purposes). */
  public double testProb() throws RemoteException;

  /**
   * Returns the prior probability of generating the nonterminal contained
   * in the specified <code>HeadEvent</code>.
   */
  public double logPrior(int id, TrainerEvent event) throws RemoteException;

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
  public double logProbHeadWithSubcats(int id, TrainerEvent event) throws RemoteException;

  public double logProbHead(int id, TrainerEvent event) throws RemoteException;

  public double logProbLeftSubcat(int id, TrainerEvent event)
    throws RemoteException;

  public double logProbRightSubcat(int id, TrainerEvent event)
    throws RemoteException;

  public double logProbSubcat(int id, TrainerEvent event, boolean side)
    throws RemoteException;

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
  public double logProbTop(int id, TrainerEvent event) throws RemoteException;

  public double logProbMod(int id, TrainerEvent event) throws RemoteException;

  public double logProbModNT(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a gap.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a gap
   */
  public double logProbGap(int id, TrainerEvent event) throws RemoteException;

  // non-log prob methods
  public double probHead(int id, TrainerEvent event) throws RemoteException;
  public double probMod(int id, TrainerEvent event) throws RemoteException;
  public double probLeftSubcat(int id, TrainerEvent event)
    throws RemoteException;
  public double probRightSubcat(int id, TrainerEvent event)
    throws RemoteException;
  public double probTop(int id, TrainerEvent event) throws RemoteException;
}
