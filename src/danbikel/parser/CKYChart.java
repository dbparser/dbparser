package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a chart for probabilistic Cocke-Kasami-Younger
 * (CKY) parsing.
 */
public class CKYChart extends Chart {
  // constants
  private final static boolean collinsNPPruneHack =
    Boolean.valueOf(Settings.get(Settings.collinsNPPruneHack)).booleanValue();
  private final static double log10 = Math.log(10);
  private final static double log100 = 2 * Math.log(10);

  private final static double[] variablePruneFact = new double[200];

  private final static double variablePruneFn(int span) {
    if (span < 5)
      return 4.0;
    else
      return Math.log(10) * Math.max(2.0, (-0.08 * span + 3.8));
  }
  static {
    for (int i = 0; i < variablePruneFact.length; i++) {
      variablePruneFact[i] = variablePruneFn(i);
    }
  }

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

  public void clearNonPreterminals() {
    for (int i = 0; i < size; i++) {
      for (int j = i; j < size; j++) {
        if (chart[i][j] == null)
          chart[i][j] = new Entry();
        else {
          if (i == j) {
            // remove non preterminal items
            Iterator it = chart[i][j].map.keySet().iterator();
            while (it.hasNext()) {
              CKYItem item = (CKYItem)it.next();
              if (!item.isPreterminal())
                it.remove();
            }
            chart[i][j].setTopInfo();
          }
          else {
            chart[i][j].clear();
          }
        }
      }
    }
  }

  protected boolean outsideBeam(Item item, double topProb) {
    CKYItem currItem = (CKYItem)item;
    Symbol label = (Symbol)currItem.label();
    if (currItem.isPreterminal())
      return false;
    // if this is an NP or NP-A with more than one child, then we use wider beam
    /*
    if ((currItem.leftChildren() != null || currItem.rightChildren() != null) &&
	Language.treebank.isNP(label))
    */
    if (collinsNPPruneHack &&
	(currItem.leftChildren() != null || currItem.rightChildren() != null) &&
	Language.treebank.stripAugmentation(label) ==
	Language.treebank.NPLabel())
      return item.logProb() < topProb - pruneFact - 3;
    /*
    else if (currItem.stop()) {
      // much smaller beam for stopped items
      int span = currItem.end() - currItem.start();
      if (span < variablePruneFact.length)
        return item.logProb() < topProb - variablePruneFact[span];
      else
        return item.logProb() < topProb - variablePruneFn(span);
    }
    */
    else
      return item.logProb() < topProb - pruneFact;
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
