package danbikel.parser;

import java.util.Map;
import java.io.Serializable;

/**
 * Represents the transition from a particular history to a particular future,
 * to be used when computing the conditional probability of seeing a particular
 * future in the context of a particular history.
 */
public class Transition implements Serializable {
  // N.B.: Make sure to update the copy method if any changes are made to
  // the data members of this class!
  private Event history;
  private Event future;

  /**
   * Constructs this transition with the specified future and history events.
   *
   * @param future the future event
   * @param history the history event
   */
  public Transition(Event future, Event history) {
    this.future = future;
    this.history = history;
  }

  // accessors
  /** Gets the future event of this transition object. */
  public Event future() { return future; }
  /** Gets the history event of this transition object. */
  public Event history() { return history; }

  // mutators
  /** Sets the future event of this transition. */
  public void setFuture(Event future) { this.future = future; }
  /** Sets the history event of this transition. */
  public void setHistory(Event history) { this.history = history; }

  /**
   * Returns the hash code of this transition object, based on the hash
   * codes of its component history and future events.
   */
  public int hashCode() {
    int code = history.hashCode();
    code = (code << 2) ^ future.hashCode();
    return code;
  }

  /**
   * Returns <code>true</code> if <code>obj</code> is an instance of
   * <code>Transition</code> and has future and history components that
   * are respectively equal to this object's future and history components.
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof Transition))
      return false;
    Transition other = (Transition)obj;
    return future.equals(other.future) && history.equals(other.history);
  }

  /** Returns a human-readable string representation of this transition. */
  public String toString() {
    return ("(" + SymbolicCollectionWriter.valueOf(future) + " | " +
	    SymbolicCollectionWriter.valueOf(history) + ")");
  }

  /** Returns a deep copy of this <code>Transition</code> object. */
  public Transition copy() {
    return new Transition(future.copy(), history.copy());
  }

  public Transition copyCanonical(Map canonicalFutures,
                                  Map canonicalHistories) {
    Event canonicalHistory = (Event)canonicalHistories.get(history);
    if (canonicalHistory == null) {
      canonicalHistory = history.copy();
      canonicalHistories.put(canonicalHistory, canonicalHistory);
    }
    Event canonicalFuture = (Event)canonicalFutures.get(future);
    if (canonicalFuture == null) {
      canonicalFuture = future.copy();
      canonicalFutures.put(canonicalFuture, canonicalFuture);
    }
    return new Transition(canonicalFuture, canonicalHistory);
  }
}
