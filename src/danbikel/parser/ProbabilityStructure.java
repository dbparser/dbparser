package danbikel.parser;

import danbikel.lisp.*;
import java.io.*;

/**
 * Abstract class to represent the probability structure--the entire
 * set of of back-off levels, including the top level--for the
 * estimation of a particular parameter class in the overall parsing
 * model (using "class" in the statistical, non-Java sense of the
 * word).  Providing this abstract structure is intended to facilitate
 * the experimentation with differing smoothing or back-off schemes.
 * Various data members are provided to enable efficient construction
 * of <code>SexpEvent</code> objects to represent events in the
 * back-off scheme, but any class that implements the {@link Event}
 * interface may be used to record events in a concrete subclass of
 * this class.
 * <p>
 * <b>Concurrency note</b>: A separate <code>ProbabiityStructure</code> object
 * needs to be constructed for each thread that needs to use its facilities,
 * to avoid concurrent access and modification of its data members (which
 * are intended to improve efficiency and are thus not designed for
 * concurrent access via <code>synchronized</code> blocks).
 *
 * @see Model
 * @see Trainer
 */
public abstract class ProbabilityStructure implements Serializable {
  protected transient int topLevelCacheSize;

  /**
   * A reusable list to enable efficient construction of <code>SexpEvent</code>
   * objects of various sizes to represent history contexts.
   *
   * @deprecated Ever since the <code>Event</code> and
   * <code>MutableEvent</code> interfaces were re-worked to include
   * methods to add and iterate over event components and the
   * <code>SexpEvent</code> class was retrofitted to these new
   * specifications, this object became superfluous, as
   * <code>SexpEvent</code> objects can now be efficiently constructed
   * directly, by using the <code>SexpEvent.add(Object)</code> method.
   *
   * @see SexpEvent#add(Object)
   * @see #histories
   * @see #historiesWithSubcats
   */
  protected SexpList historyList;

  /**
   * A reusable list to enable efficient construction of <code>SexpEvent</code>
   * objects of various sizes to represent futures.
   *
   * @deprecated Ever since the <code>Event</code> and
   * <code>MutableEvent</code> interfaces were re-worked to include
   * methods to add and iterate over event components and the
   * <code>SexpEvent</code> class was retrofitted to these new
   * specifications, this object became superfluous, as
   * <code>SexpEvent</code> objects can now be efficiently constructed
   * directly, by using the <code>SexpEvent.add(Object)</code> method.
   *
   * @see SexpEvent#add(Object)
   * @see #futures
   * @see #futuresWithSubcats
   */
  protected SexpList futureList;

  /**
   * A reusable <code>SexpEvent</code> array to represent history
   * contexts; the array will be initialized to have the size of
   * {@link #numLevels()}.  These objects may be used as the return values of
   * <code>getHistory(TrainerEvent,int)</code>.
   * @see #getHistory(TrainerEvent,int)
   */
  protected MutableEvent[] histories;
  /**
   * A reusable <code>SexpEvent</code> array to represent futures;
   * the array will be initialized to have the size of {@link #numLevels()}.
   * These objects may be used as the return values of
   * <code>getFuture(TrainerEvent,int)</code>.
   * @see #getFuture(TrainerEvent,int)
   */
  protected MutableEvent[] futures;

  /**
   * A reusable <code>SexpSubcatEvent</code> array to represent
   * histories; the array will be initialized to have the size of
   * {@link #numLevels()}.
   * These objects may be used as the return values of
   * <code>getHistory(TrainerEvent,int)</code>.
   * @see #getHistory(TrainerEvent,int)
   */
  protected MutableEvent[] historiesWithSubcats;

  /**
   * A reusable <code>SexpSubcatEvent</code> array to represent futures;
   * the array will be initialized to have the size of
   * {@link #numLevels()}. These objects may be used as the return values of
   * <code>getFuture(TrainerEvent,int)</code>.
   * @see #getFuture(TrainerEvent,int)
   */
  protected MutableEvent[] futuresWithSubcats;

  /**
   * A reusable <code>Transition</code> array to store transitions.
   * The <code>Transition</code> objects in this array may be used as the
   * return values of {@link #getTransition(TrainerEvent,int)}.
   */
  public Transition[] transitions;

  /**
   * An array used only during the computation of top-level probabilities,
   * used to store the ML estimates of all the levels of back-off.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   *
   */
  public double[] estimates;
  /**
   * An array used only during the computation of top-level probabilities,
   * used to store the lambdas calculated at all the levels of back-off.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   */
  public double[] lambdas;

  public int[] historyHashCodes;
  public int[] transitionHashCodes;

  /**
   * A temporary value used in the computation of top-level probabilities,
   * used in the computation of lambdas.
   *
   * @see Model#estimateLogProb(int,TrainerEvent)
   */
  public double prevHistCount;

  /**
   * Usually called implicitly, this constructor initializes the
   * internal, reusable {@link #historyList} to have an initial capacity of
   * the return value of <code>maxEventComponents</code>.
   *
   * @see #historyList
   * @see #futureList
   * @see #maxEventComponents
   */
  protected ProbabilityStructure() {
    histories = new SexpEvent[numLevels()];
    for (int i = 0; i < histories.length; i++)
      histories[i] = new SexpEvent(maxEventComponents());
    futures = new SexpEvent[numLevels()];
    for (int i = 0; i < futures.length; i++)
      futures[i] = new SexpEvent(maxEventComponents());
    historiesWithSubcats = new SexpSubcatEvent[numLevels()];
    for (int i = 0; i < historiesWithSubcats.length; i++)
      historiesWithSubcats[i] = new SexpSubcatEvent(maxEventComponents());
    futuresWithSubcats = new SexpSubcatEvent[numLevels()];
    for (int i = 0; i < futuresWithSubcats.length; i++)
      futuresWithSubcats[i] = new SexpSubcatEvent(maxEventComponents());

    transitions = new Transition[numLevels()];
    for (int i = 0; i < transitions.length; i++)
      transitions[i] = new Transition(null, null);

    ///////////////////////////////////////////////////////////////////////////
    // no longer needed
    historyList = new SexpList(maxEventComponents());
    futureList = new SexpList(maxEventComponents());
    ///////////////////////////////////////////////////////////////////////////

    estimates = new double[numLevels()];
    lambdas = new double[numLevels()];
    historyHashCodes = new int[numLevels()];
    transitionHashCodes = new int[numLevels()];

    topLevelCacheSize = getTopLevelCacheSize();
  }

  /**
   * This method converts the value of the setting named
   * <code>getClass().getName()&nbsp;+&nbsp;".topLevelCacheSize"</code>
   * to an integer and returns it.  This method is used within the
   * constructor of this abstract class to set the value of the
   * {@link #topLevelCacheSize} data member.  Subclasses should override
   * this method if such a setting may not be available or if a different
   * mechanism for determining the top-level cache size is desired.
   *
   * @see Settings#get(String)
   */
  protected int getTopLevelCacheSize() {
    String topLevelCacheSizeStr =
      Settings.get(getClass().getName() + ".topLevelCacheSize");
    /*
    System.err.println(getClass().getName() + ": setting top-level cache " +
                       "size to be " + topLevelCacheSizeStr);
    */
    return (topLevelCacheSizeStr == null ?
            0 : Integer.parseInt(topLevelCacheSizeStr));
  }

  /**
   * Allows subclasses to specify the maximum number of event components,
   * so that the constructor of this class may pre-allocate space in its
   * internal, reusable <code>MutableEvent</code> objects (used for efficient
   * event construction).  The default implementation simply returns 0.
   *
   * @return 0 (subclasses should override this method)
   * @see MutableEvent#ensureCapacity
   */
  protected int maxEventComponents() { return 0; }

  /**
   * Returns the number of back-off levels.
   */
  abstract public int numLevels();

  /**
   * Returns a distinguished level of back-off, or -1 if there is no
   * such distinguished level (the default implementation returns -1).
   */
  public int specialLevel() { return -1; }


  /**
   * Returns the level that corresponds to the prior for
   * that which is being predicted (the future); if there is no such
   * level, this method returns -1 (the default implementation returns -1).
   */
  public int priorLevel() { return -1; }

  /**
   * Returns the "fudge factor" for the lambda computation for
   * <code>backOffLevel</code>.  The default implementation returns
   * <code>5.0</code>.
   *
   * @param backOffLevel the back-off level for which to return a "fudge
   * factor"
   */
  public double lambdaFudge(int backOffLevel) { return 5.0; }

  /**
   * Extracts the history context for the specified back-off level
   * from the specified trainer event.
   *
   * @param trainerEvent the event for which a history context is desired
   * for the specified back-off level
   * @param backOffLevel the back-off level for which to get a history context
   * from the specified trainer event
   * @return an <code>Event</code> object that represents the history context
   * for the specified back-off level
   */
  abstract public Event getHistory(TrainerEvent trainerEvent,
				   int backOffLevel);

  /**
   * Extracts the future for the specified level of back-off from the specified
   * trainer event.  Typically, futures remain the same regardless of back-off
   * level.
   *
   * @param trainerEvent the event from which a future is to be extracted
   * @param backOffLevel the back-off level for which to get the future event
   * @return an <code>Event</code> object that represents the future
   * for the specified back-off level
   */
  abstract public Event getFuture(TrainerEvent trainerEvent,
				  int backOffLevel);


  /**
   * Returns the reusable transition object for the specified back-off level,
   * with its history set to the result of calling
   * <code>getHistory(trainerEvent, backOffLevel)</code> and its
   * future the result of <code>getFuture(trainerEvent, backOffLevel)</code>.
   *
   * @param trainerEvent the event from which a transition is to be extracted
   * @param backOffLevel the back-off level for which to get the transition
   * @return the reusable transition object containing the history and future
   * of the specified back-off level
   */
  public Transition getTransition(TrainerEvent trainerEvent,
				  int backOffLevel) {
    Transition transition = transitions[backOffLevel];
    transition.setHistory(getHistory(trainerEvent, backOffLevel));
    transition.setFuture(getFuture(trainerEvent, backOffLevel));
    return transition;
  }

  Transition getTransition(TrainerEvent trainerEvent, Event history,
                           int backOffLevel) {
    Transition transition = transitions[backOffLevel];
    transition.setHistory(history);
    transition.setFuture(getFuture(trainerEvent, backOffLevel));
    return transition;
  }

  /**
   * Returns the recommended cache size for the specified back-off level
   * of the model that uses this probability structure.  This default
   * implementation simply returns <code>topLevelCacheSize / 2^level</code>.
   *
   * @see #topLevelCacheSize
   */
  public int cacheSize(int level) {
    int size = topLevelCacheSize;
    for (int i = 0; i < level; i++)
      size /= 2;
    return size;
  }


  /**
   * Returns a deep copy of this object.  Currently, all data members
   * of <code>ProbabilityStructure</code> objects are used solely as
   * temporary storage during certain method invocations; therefore,
   * this copy method should simply return a new instance of the runtime
   * type of this <code>ProbabilityStructure</code> object, with
   * freshly-created data members that are <i>not</i> deep copies of
   * the data members of this object.  The general contract of the
   * copy method is slightly violated here, but without undue harm,
   * given the lack of persistent data of these types of objects. If a
   * concrete subclass has specific requirements for its data members
   * to be deeply copied, this method should be overridden.
   */
  public abstract ProbabilityStructure copy();

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    topLevelCacheSize = getTopLevelCacheSize();
  }
}
