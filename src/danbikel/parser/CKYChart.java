package danbikel.parser;

import danbikel.util.ObjectPool;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a chart for probabilistic Cocke-Kasami-Younger
 * (CKY) parsing.
 */
public class CKYChart extends Chart {
  // data members (in addition to protected members of superclass)


  // constructors

  /**
   * Constructs a new chart with the default chart size.
   */
  public CKYChart() {
    super();
  }
  /**
   * Constructs a new chart with the specified chart size.
   *
   * @param size the initial size of this chart
   */
  public CKYChart(int size) {
    super(size);
  }

  public CKYChart(int cellLimit, double pruneFact) {
    super(cellLimit, pruneFact);
  }

  public CKYChart(int size, int cellLimit, double pruneFact) {
    super(size, cellLimit, pruneFact);
  }

  protected void setUpItemPool() {
    itemPool = new ObjectPool(CKYItem.class, 50000);
  }

  public CKYItem getNewItem() {
    return (CKYItem)itemPool.get();
    // return new CKYItem();
  }
}
