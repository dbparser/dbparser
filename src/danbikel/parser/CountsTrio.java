package danbikel.parser;

import java.io.Serializable;

/**
 * Class for grouping the three counts tables necessary for counting
 * transitions, histories and unique transitions (or <i>diversity</i> counts
 * for the history events).
 * <p>
 * <b>Implementation note</b>: The three tables are stored internally as
 * two objects, a <code>CountsTable</code> and a <code>BiCountsTable</code>
 * object.  These two objects are available from the {@link #transition()}
 * and {@link #history()} accessor methods, respectively.
 */
public class CountsTrio implements Serializable {
  // public constants
  /**
   * The constant to be used as an index when adding or retrieving
   * history counts from the <code>BiCountsTable</code>.
   */
  public final static int hist = 0;
  /**
   * The constant to be used as an index when adding or retrieving
   * diversity counts from the <code>BiCountsTable</code>.
   */
  public final static int diversity = 1;


  // data members
  CountsTable trans;
  BiCountsTable histAndDiversity;

  CountsTrio() {
    trans = new CountsTable();
    histAndDiversity = new BiCountsTable();
  }
  /**
   * Gets the <code>CountsTable</code> for transitions.
   * @return the transitions <code>CountsTable</code>.
   */
  public CountsTable transition() { return trans; }
  /**
   * Gets the <code>CountsTable</code> for histories counts and diversity
   * statistics.
   * @return the histories <code>CountsTable</code>.
   */
  public BiCountsTable history() { return histAndDiversity; }
}
