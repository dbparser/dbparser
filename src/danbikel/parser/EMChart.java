package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a chart for performing constrained CKY parsing so as
 * to perform the E-step of the Inside-Outside algorithm.
 */
public class EMChart extends CKYChart {
  // constants
  private final static String className = EMChart.class.getName();

  // inner class
  /**
   * Contains all information and items covering a particular span.
   */
  protected static class Entry extends Chart.Entry implements Serializable {
    int[] numItemsAtLevel = new int[10];
    int numLevels;
    Entry() { super(); }
  }

  // additional data member
  protected Entry[][] chart;

  // constructors
  /**
   * Constructs a new chart with the default chart size.
   */
  public EMChart() {
    super();
    super.chart = chart;
  }
  /**
   * Constructs a new chart with the specified chart size.
   *
   * @param size the initial size of this chart
   */
  public EMChart(int size) {
    super(size);
    super.chart = chart;
  }

  /**
   * Ensures that the size of the chart is at least as large as the
   * specified size.
   *
   * @param size the size to be set for this chart
   */
  protected void setSize(int size) {
    if (this.size < size) {
      this.size = size;
      chart = new Entry[size][size];
    }
  }

  /**
   * Checks every map of the chart covering a span less than or equal to
   * size and clears it; if a chart entry is <code>null</code>, then a
   * new map is created.
   */
  protected void clear() {
    totalItems = 0;
    if (debugNumItemsGenerated) {
      totalItemsGenerated = 0;
    }
    if (debugNumPrunedItems) {
      numPrePruned = numPruned = 0;
    }
    for (int i = 0; i < size; i++)
      for (int j = i; j < size; j++)
	if (chart[i][j] == null)
	  chart[i][j] = new Entry();
	else {
	  chart[i][j].clear();
          chart[i][j].numLevels = 0;
          int[] levelCounts = chart[i][j].numItemsAtLevel;
          for (int k = 0; k < levelCounts.length; k++)
            levelCounts[k] = 0;
	}
  }

  protected boolean outsideBeam(Item item, double topProb) {
    return false;
  }

  protected boolean toPrune(int start, int end, Item item) {
    return false;
  }

  /**
   * This method has been overloaded so that it simply throws an
   * <code>UnsupportedOperationException</code>, since pruning is
   * inappropriate when performing the E-step of the Inside-Outside
   * algorithm.
   */
  public void doPruning() {
    String msg = "pruning is not appropriate for EM";
    throw new UnsupportedOperationException(msg);
  }

  /**
   * Adds this item that has no antecedents to the chart.
   *
   * @param start the start of the span of this item
   * @param end the end of the span of this item
   * @param item the item to be added
   */
  public void add(int start, int end, EMItem item) {
    add(start, end, item, null, null, null, null);
  }

  /**
   * Adds this item to the chart, recording its antecedents and the events
   * and their probabilities that allowed this item (consequent) to be produced.
   *
   * @param start the start of the span of the item to be added
   * @param end the end of the span of the item to be added
   * @param item the item to be added
   * @param ante1 the first of two possible antecedents of the item to be added
   * @param ante2 the second of two possible antecedents of the item to be
   * added; if the item has only one antecedent, the value of this parameter
   * should be <code>null</code>
   * @param event the single event that allowed this item (consequent) to be
   * produced from its antecedent(s)
   * @param prob the probability of the specified event
   */
  public void add(int start, int end, EMItem item,
		  EMItem ante1, EMItem ante2,
		  TrainerEvent event, double prob) {
    add(start, end, item, ante1, ante2,
	new TrainerEvent[]{event}, new double[]{prob});
  }

  /**
   * Adds this item to the chart, recording its antecedents and the events
   * and their probabilities that allowed this item (consequent) to be produced.
   *
   * @param start the start of the span of the item to be added
   * @param end the end of the span of the item to be added
   * @param item the item to be added
   * @param ante1 the first of two possible antecedents of the item to be added
   * @param ante2 the second of two possible antecedents of the item to be
   * added; if the item has only one antecedent, the value of this parameter
   * should be <code>null</code>
   * @param events the events that allowed this item (consequent) to be
   * produced from its antecedent(s)
   * @param probs an array of probabilities of the same size and coindexed
   * with the specified array of events, where each probability
   * is that for its coindexed event
   */
  public void add(int start, int end, EMItem item,
		  EMItem ante1, EMItem ante2,
		  TrainerEvent[] events, double[] probs) {
    if (debugNumItemsGenerated) {
      totalItemsGenerated++;
    }
    if (item.insideProb() == Constants.probImpossible)
      return;

    Entry chartEntry = chart[start][end];
    MapToPrimitive items = chartEntry.map;
    MapToPrimitive.Entry itemEntry = items.getEntry(item);
    boolean itemExists = itemEntry != null;
    EMItem existingItem = null;
    int unaryLevel = item.unaryLevel();
    if (itemExists) {
      existingItem = (EMItem)itemEntry.getKey();
      EMItem.AntecedentPair currList = existingItem.antecedentPairs();
      EMItem.AntecedentPair newList  =
	new EMItem.AntecedentPair(ante1, ante2, events, probs, currList);
      existingItem.setAntecedentPairs(newList);
      existingItem.increaseInsideProb(item.insideProb());
      if (unaryLevel != existingItem.unaryLevel())
        System.err.println(className + ": warning: re-deriving an item with " +
                           "a different unary level!");
    }
    else {
      if (ante1 != null) {
        EMItem.AntecedentPair newPair =
          new EMItem.AntecedentPair(ante1, ante2, events, probs, null);
        item.setAntecedentPairs(newPair);
      }
      if (unaryLevel > chartEntry.numLevels) {
        System.err.println(className + ": error: trying to add item with " +
                           "unary level that is too large (largest seen so " +
                           " far: " + chartEntry.numLevels +
                           "; current item's unary level: " + unaryLevel + ")");
        System.err.println("\titem[" + start + "," + end + "]: " + item);
      }
      else if (unaryLevel == chartEntry.numLevels) {
        // increase capacity of counter array, if necessary
        int currCapacity = chartEntry.numItemsAtLevel.length;
        if (unaryLevel == currCapacity) {
          int[] oldArr = chartEntry.numItemsAtLevel;
          chartEntry.numItemsAtLevel = new int[currCapacity * 2];
          System.arraycopy(oldArr, 0, chartEntry.numItemsAtLevel,
                           0, currCapacity);
        }
        chartEntry.numLevels++;
        chartEntry.numItemsAtLevel[unaryLevel]++;
      }
      else {
        chartEntry.numItemsAtLevel[unaryLevel]++;
      }
      items.put(item, item.insideProb());
      totalItems++;
    }
  }

  protected void setUpItemPool() {
    itemPool = new ObjectPool(EMItem.class, 50000);
  }
  public EMItem getNewEMItem() {
    return (EMItem)super.getNewItem();
  }

  /**
   * Reclaims this chart item.  This method returns the specified item
   * to the object pool of available items, after clearing its antecedent
   * list.
   *
   * @param item the item to be reclaimed
   */
  protected void reclaimItem(Item item) {
    EMItem emItem = (EMItem)item;
    emItem.antecedentPairs = null;
    itemPool.putBack(item);
  }

  protected void reclaimItemCollection(Collection c) {
    if (c.size() > 0) {
      Iterator it  = c.iterator();
      while (it.hasNext()) {
	EMItem item = (EMItem)it.next();
	item.antecedentPairs = null;
      }
      super.reclaimItemCollection(c);
    }
  }


  public int numUnaryLevels(int start, int end) {
    return chart[start][end].numLevels;
  }

  public int[] unaryLevelCounts(int start, int end) {
    return chart[start][end].numItemsAtLevel;
  }
}
