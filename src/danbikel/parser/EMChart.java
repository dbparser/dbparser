package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a chart for probabilistic Cocke-Kasami-Younger
 * (CKY) parsing.
 */
public class EMChart extends CKYChart {
  // constants
  private final static boolean collinsNPPruneHack =
    Boolean.valueOf(Settings.get(Settings.collinsNPPruneHack)).booleanValue();

  // constructors

  /**
   * Constructs a new chart with the default chart size.
   */
  public EMChart() {
    super();
  }
  /**
   * Constructs a new chart with the specified chart size.
   *
   * @param size the initial size of this chart
   */
  public EMChart(int size) {
    super(size);
  }

  public EMChart(int cellLimit, double pruneFact) {
    super(cellLimit, pruneFact);
  }

  public EMChart(int size, int cellLimit, double pruneFact) {
    super(size, cellLimit, pruneFact);
  }

  protected boolean outsideBeam(Item item, double topProb) {
    return false;
  }

  protected void setUpItemPool() {
    String chartItemClassname = Settings.get(Settings.chartItemClass);
    Class chartItemClass = null;
    try { chartItemClass = Class.forName(chartItemClassname); }
    catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Couldn't find class " +
				 chartItemClassname + "; check " +
				 Settings.chartItemClass + " property");
    }
    itemPool = new ObjectPool(chartItemClass, 50000);
  }

  public CKYItem getNewItem() {
    // return new CKYItem();
    /*
    CKYItem newItem = (CKYItem)itemPool.get();
    if (newItem.garbage())
      throw new RuntimeException();
    return newItem;
    */
    return (CKYItem)itemPool.get();
  }

  protected void reclaimItemCollection(Collection c) {
    if (c.size() > 0)
      itemPool.putBackAll(c);
  }

  /**
   * Returns <code>true</code> if the specified item has received its
   * stop probabilities (that is, if <code>item.stop() == true</code>).
   *
   * @param item the item to be tested
   */
  protected boolean cellLimitShouldApplyTo(Item item) {
    //return ((CKYItem)item).stop();
    return true;
  }
}
