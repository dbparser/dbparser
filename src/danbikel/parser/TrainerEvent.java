package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * An extremely simple interface to allow iteration over various kinds of
 * events used by the class {@link Trainer}.
 */
public interface TrainerEvent extends Serializable {
  /**
   * Returns the head word object associated with an event, or <code>null</code>
   * if this <code>TrainerEvent</code> has no such object.
   */
  public Word headWord();

  /**
   * Returns the modifier head word object associated with an event,
   * or <code>null</code> if this <code>TrainerEvent</code> has no such object.
   */
  public Word modHeadWord();

  /**
   * Returns a deep copy of this event of the same run-time type.
   */
  public TrainerEvent copy();

  /**
   * Returns the parent symbol of this event, or <code>null</code> if
   * this event has no such object.
   */
  public Symbol parent();

  /**
   * Returns the side of a modifier event.
   *
   * @exception UnsupportedOperationException if this is not a modifier event
   */
  public boolean side();
}
