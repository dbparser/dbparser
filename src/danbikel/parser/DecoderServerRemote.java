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

  public Map leftSubcatMap() throws RemoteException;

  public Map rightSubcatMap() throws RemoteException;

  public CountsTable nonterminals() throws RemoteException;

  public Set prunedPreterms() throws RemoteException;

  public Set prunedPunctuation() throws RemoteException;

  /**
   * Replaces every low-frequency or "unknown" word of the specified sentence
   * with a two-element <code>SexpList</code> object, whose first element
   * is the original word and whose second element is the word feature vector.
   */
  public SexpList convertUnknownWords(SexpList sentence) throws RemoteException;

  public ProbabilityStructure leftSubcatProbStructure() throws RemoteException;

  public ProbabilityStructure rightSubcatProbStructure() throws RemoteException;

  /** Returns a test probability (for debugging purposes). */
  public double testProb() throws RemoteException;

  /**
   * Returns the prior probability of generating the nonterminal contained
   * in the specified <code>HeadEvent</code>.
   */
  public double logPrior(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a new head (and possibly
   * its left- and right-subcat frames.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a new head
   */
  public double logProbHead(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a right modifier.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a new right modifier
   */
  public double logProbRight(int id, TrainerEvent event) throws RemoteException;

  /**
   * Returns the log of the probability of generating a left modifier.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a new left modifier
   */
  public double logProbLeft(int id, TrainerEvent event) throws RemoteException;

  public double logProbMod(int id, TrainerEvent event, boolean side)
    throws RemoteException;

  /**
   * Returns the log of the probability of generating a gap.
   *
   * @param id the unique id of the client invoking the method
   * @param event the top-level <code>TrainerEvent</code>, containing the
   * complete context needed to compute the requested probability
   * @return the log of the probability of generating a gap
   */
  public double logProbGap(int id, TrainerEvent event) throws RemoteException;
}
