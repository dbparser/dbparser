package danbikel.parser;

import java.io.Serializable;

/**
 * Class for grouping the three <code>CountsTable</code>
 * objects necessary for counting transitions, histories and
 * unique transitions.
 */
public class CountsTrio implements Serializable {
  // constants
  private static final int TRANS = 0;
  private static final int HIST = 1;
  private static final int UNIQUE = 2;
  private static final int NUM_COUNTS_TABLES = 3;

  CountsTable[] trio = new CountsTable[NUM_COUNTS_TABLES];
  CountsTrio() {
    for (int i = 0; i < trio.length; i++)
      trio[i] = new CountsTable();
  }
  /**
   * Gets the <code>CountsTable</code> for transitions.
   * @return the transitions <code>CountsTable</code>.
   */
  public CountsTable transition() { return trio[TRANS]; }
  /**
   * Gets the <code>CountsTable</code> for histories.
   * @return the histories <code>CountsTable</code>.
   */
  public CountsTable history() { return trio[HIST]; }
  /**
   * Gets the <code>CountsTable</code> for unique transitions.
   * @return the unique transitions <code>CountsTable</code>.
   */
  public CountsTable unique() { return trio[UNIQUE]; }
}
