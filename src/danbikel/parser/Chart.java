package danbikel.parser;

import danbikel.lisp.*;
import danbikel.util.ObjectPool;
import java.util.*;
import java.io.Serializable;

/**
 * Provides the skeletal infrastructure for a chart indexed by start and
 * end words, as well as by arbitrary labels taken from the chart items.
 *
 * @see Item
 */

public abstract class Chart implements Serializable {
  // debugging constants
  private final static boolean debug = false;
  private final static boolean debugPrune = true;
  private final static boolean debugAddToChart = false;
  private final static boolean debugPoolUsage = true;
  private final static boolean debugNumItemsGenerated = true;
  private final static boolean debugCellSize = true;

  // constants
  private final static int defaultChartSize = 200;
  private final static String className = Chart.class.getName();

  // inner classes

  /**
   * Contains all information and items covering a particular span.
   */
  protected final static class Entry implements Serializable {
    Map map;
    double topLogProb;

    Entry() {
      map = new HashMap();
      topLogProb = Constants.logOfZero;
    }
    void clear() {
      map.clear();
      topLogProb = Constants.logOfZero;
    }
  }

  // data members
  /**
   * A chart is a two-dimensional array of maps, each of which maps Item
   * objects to their logProbs.  More specifically, a chart is a two-dimensional
   * array of <code>Chart.Entry</code> objects, each of which contains one
   * such map, as well as a data member that stores the top log probability of
   * all the items covering the span of the chart entry.
   *
   * @see Chart.Entry
   */
  protected Entry[][] chart;
  /** The current size of the chart. */
  protected int size;
  protected int cellLimit = -1;
  protected double pruneFact = 0.0;
  protected int totalItems = 0;
  protected transient ObjectPool itemPool;
  protected int totalItemsGenerated = 0;
  protected int numPruned = 0;
  protected int numPrePruned = 0;

  /**
   * Constructs a new chart with the default chart size.  This instructor
   * will be called, often implicitly, by the constructor of a subclass.
   */
  protected Chart() {
    this(defaultChartSize);
  }

  /**
   * Constructs a new chart with the specified chart size.  This connstructor
   * must be called by the constructor of a subclass.
   *
   * @param size the initial size of this chart
   */
  protected Chart(int size) {
    if (size <= 0)
      throw new IllegalArgumentException();
    chart = new Entry[size][size];
    this.size = size;
    setUpItemPool();
  }

  protected Chart(int cellLimit, double pruneFact) {
    this(defaultChartSize, cellLimit, pruneFact);
  }

  protected Chart(int size, int cellLimit, double pruneFact) {
    this(size);
    this.cellLimit = cellLimit;
    this.pruneFact = pruneFact;
  }

  /**
   * Sets this chart to be the specified size and clears it for parsing
   * sentences of length less than or equal to the specified size.
   * This method should be called prior to parsing every sentence.
   * The default implementation of this method simply calls
   * <code>setSize(size)</code> and then <code>clear()</code>.
   *
   * @param size the size to be set for this chart
   */
  protected void setSizeAndClear(int size) {
    reclaimItemsInChart();
    setSize(size);
    clear();
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
    if (debugPrune) {
      numPrePruned = numPruned = 0;
    }
    for (int i = 0; i < size; i++)
      for (int j = i; j < size; j++)
	if (chart[i][j] == null)
	  chart[i][j] = new Entry();
	else
	  chart[i][j].clear();
  }

  /**
   * Returns <code>true</code> if the specified chart item should not
   * be added to the specified set of items because its probability is not
   * within {@link #pruneFact} of the highest-ranked item. This method is
   * guaranteed to be called by {@link #add(int,int,Item)} when a sorted set
   * of chart items already exists (and hence contains at least one chart
   * item).
   *
   * @param start the start of the span for the specified item
   * @param end the end of the span for the specified item
   * @param item the item being added to <code>items</code>
   */
  protected boolean toPrune(int start, int end, Item item) {
    double topProb = chart[start][end].topLogProb;
    if (debugPrune) {
      System.err.println(className +
                         ": pruning away item: " + item + " because " +
                         "its prob " + item.logProb() +
                         " is less than " + topProb + " - " + pruneFact +
                         " = " + (topProb - pruneFact));
      numPrePruned++;
    }
    return item.logProb() < (topProb - pruneFact);
  }

  public void prune(int start, int end) {
    Map items = chart[start][end].map;
    if (pruneFact > 0.0) {
      // remove the lowest probability elements until the lowest one
      // left is within pruneFact of highest
      double topProb = chart[start][end].topLogProb;
      double lowestProbAllowed = topProb - pruneFact;
      Iterator it = items.keySet().iterator();
      while (it.hasNext()) {
	CKYItem currItem = (CKYItem)it.next();
	if (currItem.logProb() < lowestProbAllowed) {
	  if (debugPrune) {
	    System.err.println(className + ": pruning away item: " + currItem +
			       " because its prob is less than " +
			       lowestProbAllowed);
            numPruned++;
	  }
	  totalItems--;
	  it.remove();
	  reclaimItem(currItem);
	}
	else
	  break;
      }
    }
    if (cellLimit > 0) {
      /*
        int numItems = items.size();
        if (numItems > cellLimit) { // don't create iterator if no need
	// remove lowest prob items until the set size is equal to the limit
	int numToRemove = numItems - cellLimit;
	Iterator it = items.keySet().iterator();
	while (it.hasNext() && numToRemove > 0) {
	Item currItem = (Item)it.next();  // get the item so we can remove it
	numToRemove--;
	chart[start][end].numItems--;
	totalItems--;
	it.remove();
	reclaimItem(currItem);
	}
        }
      */
      throw new UnsupportedOperationException("need to implement cell " +
					      "limiting, now that we no " +
					      "longer have sorted [NT,i,j] " +
					      "cells!");
    }
  }

  /**
   * Adds the specified item covering the specified span to this chart.
   *
   * @param start the index of the first word in the span covered by
   * <code>item</code>
   * @param end the index of the last word in the span covered by
   * <code>item</code>
   * @param item the item to be added to this chart
   * @return <code>true</code> if <code>item</code> was added to the
   * chart, <code>false</code> otherwise
   */
  public boolean add(int start, int end, Item item) {
    if (debugNumItemsGenerated) {
      totalItemsGenerated++;
    }
    if (item.logProb() <= Constants.logOfZero)
      return false;

    boolean added = false;

    Map items = chart[start][end].map;

    if (!toPrune(start, end, item)) {
      // see if equal (technically, equivalent) item already exists
      // but has a lower probability, replace it with current item
      // N.B.: replacement *must* be implemented by a remove followed by a
      // put, so that item, which is a new equivalent, higher-probability
      // object, is guaranteed to be in the Map
      Double itemProb = (Double)items.get(item);

      /*
      if (debug) {
        System.err.println("got item with logProb " + itemProb +
                           "; comparing to " + item.logProb());
      }
      */
      boolean itemExists = itemProb != null;
      if (!itemExists ||
          (itemExists && itemProb.doubleValue() < item.logProb())) {
        boolean removedOldItem = false;
        if (itemExists) { // would be great if we could reclaim, but can't
          items.remove(item);  // crucial!  (see comment above)
          removedOldItem = true;
        }
        added = true;
	items.put(item, new Double(item.logProb()));
        // if we removed an old item, there's no net gain in number of items
        if (!removedOldItem) {
          totalItems++;
        }
        // update top prob
        if (item.logProb() > chart[start][end].topLogProb)
          chart[start][end].topLogProb = item.logProb();
      }

      if (debugAddToChart) {
        if (itemExists) {
          if (itemProb.doubleValue() < item.logProb()) {
            System.err.println(className + ": adding item because equivalent " +
                               "item exists with lower prob; existing item " +
                               "prob=" + itemProb +
                               "; item to add prob=" + item.logProb());
          }
          else if (itemProb.doubleValue() == item.logProb()) {
            System.err.println(className + ": not adding item because " +
                               "equivalent item exists with " +
                               "equal prob; prob=" + itemProb);
            System.err.println("\titem that was to be added(" +
                               start + "," + end + "): " + item);
          }
          else {
            System.err.println(className + ": not adding item because " +
                               "equivalent item exists with higher prob; " +
                               "existing item prob=" + itemProb +
                               "; item to add prob=" + item.logProb());
            System.err.println("\titem that was to be added(" +
                               start + "," + end + "): " + item);
          }
        }
      }
    }
    else {
      // item's logProb already below threshold, so *caller* should reclaim the
      // item
      //reclaimItem(item);
    }
    if (debugAddToChart) {
      if (added)
        System.err.println(className +
                           ": added item(" + start + "," + end + "): " + item);
    }

    return added;
  }

  /**
   * Returns the highest log probability of an item covering the specified span.
   */
  public double getTopLogProb(int start, int end) {
    return chart[start][end].topLogProb;
  }

  /**
   * Returns an iterator over all chart items (having all labels) covering
   * the specified span.  The iterator returned is for read-only access, and
   * thus an <code>UnsupportedOperationException</code> will be thrown if
   * its <code>remove</code> method is invoked.
   *
   * @param start the start index of the span for which to get all items
   * @param end the end index of the span for which to get all items
   * @return an iterator over all chart items covering the specified span
   */
  public Iterator get(int start, int end) {
    return chart[start][end].map.keySet().iterator();
  }

  protected void reclaimItem(Item item) {
    itemPool.putBack(item);
  }

  /**
   * Called by <code>Decoder.parse</code> after parsing has finished for a
   * particular sentence.  This default implementation simply calls
   * {@link #reclaimItemsInChart}.
   */
  public void postParseCleanup() {
    reclaimItemsInChart();
  }

  protected void reclaimItemsInChart() {
    int numCells = 0, maxCellSize = 0;
    if (debugPrune) {
      System.err.println(className +
                         ": number of items pre-pruned: " + numPrePruned);
      System.err.println(className + ": number of items pruned: " + numPruned);
      System.err.println(className + ": total number of pruned items: " +
                         (numPrePruned + numPruned));
    }
    if (debugCellSize) {
      System.err.println(className +
                         ": total No. of items in chart: " + totalItems);
    }
    if (totalItems > 0) {
      if (debugNumItemsGenerated) {
	System.err.println(className +
                           ": generated " + totalItemsGenerated + " for the " +
			   "previous sentence");
      }
      if (debugPoolUsage) {
        System.err.println(className + ": pool has " + itemPool.size() +
                           " items; capacity = " + itemPool.capacity() +
                           "; reclaiming " + totalItems + " items");
      }
      for (int i = 0; i < size; i++) {
        for (int j = i; j < size; j++) {
	  Map map = chart[i][j].map;
	  itemPool.putBackAll(map.keySet());
          if (debugCellSize) {
            if (map.size() > 0)
              numCells++;
            if (map.size() > maxCellSize)
              maxCellSize = map.size();
          }
        }
      }
    }
    if (debugPoolUsage) {
      System.err.println(className + ": pool has " + itemPool.size() +
                         " items; capacity = " + itemPool.capacity());
    }
    if (debugCellSize) {
      if (numCells > 0)
	System.err.println(className + ": num. cells: " + numCells +
			   "; avg. cell size: " +
                           (totalItems / (float)numCells) +
			   "; max. cell size: " + maxCellSize);
    }
  }

  /**
   * Sets up the item object pools.
   */
  protected abstract void setUpItemPool();
}
