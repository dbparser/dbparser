package danbikel.parser;

import danbikel.lisp.*;
import danbikel.util.*;
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
  private final static boolean debugPrune = false;
  private final static boolean debugNumPrunedItems = true;
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
    MapToPrimitive map;
    Item topItem;
    double topLogProb;

    Entry() {
      map = new HashMapDouble();
      topLogProb = Constants.logOfZero;
    }
    void clear() {
      map.clear();
      topItem = null;
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
  /**
   * The current size of the chart. This value should always be equal
   * to or greater than the length of the parsed sentence.
   *
   * @see #setSizeAndClear(int)
   */
  protected int size;
  /**
   * The maximum number of items allowed in a cell (span) of this chart.
   *
   * @see Settings#decoderUseCellLimit
   */
  protected int cellLimit = -1;
  /**
   * The natural log of the prune factor for this chart.  If items have a log
   * probability that is lower than the top-ranked item's log probability
   * minus this prune factor, they are pruned away.
   */
  protected double pruneFact = 0.0;
  /**
   * The total number of items added to this chart for a particular sentence
   * (between calls to {@link #clear()}).
   */
  protected int totalItems = 0;
  /**
   * The pool of chart items, to be used by the decoder instead of constructing
   * new chart items while decoding.
   */
  protected transient ObjectPool itemPool;
  /**
   * A list of garbage items.
   *
   * @see #add(int,int,Item)
   */
  protected transient ArrayList garbageItems;
  /**
   * The total number of items generated, that is, the total number of items
   * that a decoder <i>attempts</i> to add to this chart (used for debugging).
   */
  protected int totalItemsGenerated = 0;
  /**
   * The total number of items pruned during the parse of a particular
   * sentence.  Typically, after all items have been added for a particular
   * span, a decoder will invoke the {@link #prune(int,int)} method for
   * that span.  The value of this data member after parsing is complete
   * will reflect the total number of items pruned via calls to this method.
   *
   * @see #prune(int,int)
   */
  protected int numPruned = 0;
  /**
   * The total number of items pre-pruned for a particular sentence, via
   * calls to the {@link #toPrune(int,int,Item)} method.
   *
   * @see #toPrune(int,int,Item)
   */
  protected int numPrePruned = 0;

  private Item[] sortedArr = new Item[1000];
  private int numSorted = 0;

  /**
   * Indicates whether the chart is currently doing any pruning.
   *
   * @see #doPruning()
   * @see #dontDoPruning()
   */
  protected boolean pruning = true;

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
    garbageItems = new ArrayList();
  }

  /**
   * Constructs a new chart with a default initial chart size, and with
   * the specified cell limit and prune factor.
   *
   * @param cellLimit the limit to the number of items per cell
   * @param pruneFact that log of the prune factor
   *
   * @see #cellLimit
   * @see #pruneFact
   */
  protected Chart(int cellLimit, double pruneFact) {
    this(defaultChartSize, cellLimit, pruneFact);
  }

  /**
   * Constructs a new chart with the specified initial chart size, cell limit
   * and prune factor.
   *
   * @param cellLimit the limit to the number of items per cell
   * @param pruneFact that log of the prune factor
   *
   * @see #cellLimit
   * @see #pruneFact
   */
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
    if (debugNumPrunedItems) {
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
    if (!pruning)
      return false;

    double topProb = chart[start][end].topLogProb;

    if (debugPrune || debugNumPrunedItems) {
      boolean belowThreshold = item.logProb() < (topProb - pruneFact);
      if (debugPrune){
        if (belowThreshold)
          System.err.println(className +
                             ": pruning away item: " + item + " because " +
                             "its prob " + item.logProb() +
                             " is less than " + topProb + " - " + pruneFact +
                             " = " + (topProb - pruneFact));
      }
      if (debugNumPrunedItems) {
        if (belowThreshold)
          numPrePruned++;
      }
    }

    return item.logProb() < (topProb - pruneFact);
  }

  /**
   * Prunes away items in the specified span that are either below the
   * probability threshold of the top-ranked item for that span, or
   * are outside the cell limit, if one has been specified.
   *
   * @param start the start of the span in which to prune chart items
   * @param end the end of the span in which to prune chart items
   *
   * @see #cellLimit
   * @see #pruneFact
   */
  public void prune(int start, int end) {
    if (!pruning)
      return;
    MapToPrimitive items = chart[start][end].map;
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
	  }
          if (debugNumPrunedItems) {
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
      cellLimit = Math.max(10, Math.min(90, 350 / (end + 1 - start)));
      if (end > start) { // don't do cell limiting on spans of length 1
        int numItems = items.size();
        if (numItems > cellLimit) { // don't create iterator if no need
          // reset sortedArr and numSorted
          if (numItems > sortedArr.length) {
            int newLen = Math.max(sortedArr.length * 2, numItems);
            sortedArr = new Item[newLen];
          }
          else {
            for (int i = 0; i < numSorted; i++)
              sortedArr[i] = null;
          }
          numSorted = 0;
          Iterator itemsIt = items.keySet().iterator();
	  while (itemsIt.hasNext()) {
	    Item item = (Item)itemsIt.next();
	    sortedArr[numSorted++] = item;
	  }
	  items.clear();
	  Arrays.sort(sortedArr, 0, numSorted);
	  int sortedIdx = numSorted - 1;
	  for (int counter = 0; counter < cellLimit; counter++, sortedIdx--)
	    items.put(sortedArr[sortedIdx], sortedArr[sortedIdx].logProb());
	  for ( ; sortedIdx >= 0; sortedIdx--)
	    reclaimItem(sortedArr[sortedIdx]);
	  /*
          // add all items that are eligible for limiting to sortedArr
          Iterator itemsIt = items.keySet().iterator();
          while (itemsIt.hasNext()) {
	    Item item = (Item)itemsIt.next();
	    if (cellLimitShouldApplyTo(item))
	      sortedArr[numSorted++] = item;
	  }
	  if (numSorted > cellLimit) {
	    Arrays.sort(sortedArr, 0, numSorted);
	    // prune away and reclaim items below cellLimit
	    int sortedIdx = numSorted - cellLimit - 1;
	    for ( ; sortedIdx >= 0; sortedIdx--) {
	      Item item = sortedArr[sortedIdx];
	      items.remove(item);
	      reclaimItem(item);
	    }
	  }
	  */
	}
      }
    }
  }

  /**
   * Returns <code>true</code> if cell limiting should apply to the specified
   * item.
   *
   * @param item the item to be tested
   */
  abstract protected boolean cellLimitShouldApplyTo(Item item);

  /**
   * Adds the specified item covering the specified span to this chart.
   * <p>
   * <b>Caution</b>: By convention, the caller is responsible for reclaiming
   * unadded chart items (via {@link #reclaimItem(Item)}).  However, in the
   * case where an old item is removed from the chart because a new,
   * equivalent item has a greater probability, this method does not
   * immediately reclaim the old item, but rather marks it as "garbage",
   * via its {@link Item#setGarbage(boolean) setGarbage} method.  This
   * behvaior is to prevent the following case. Suppose that the behavior
   * of this method <i>were</i> to reclaim such "booted" chart items
   * immediately.  Imagine that the caller is attempting to add a
   * collection of chart items, and that an item in that collection was
   * added, but then was removed due to a subsequent, equivalent item in
   * the same collection having greater probability.  In this case, the
   * caller would have no way to know that the previous item in its
   * collection had been silently reclaimed, so that if it operated over
   * that collection <i>again</i>, there would be a garbage item that, as
   * far as it knew, had been added and was still in the chart.  By
   * marking such "booted" items as garbage instead of immediately
   * reclaiming them, callers can check for items' garbage status when
   * they repeatedly operate over collections of "added" items.
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

    MapToPrimitive items = chart[start][end].map;

    if (!toPrune(start, end, item)) {
      // see if equal (technically, equivalent) item already exists
      // but has a lower probability, replace it with current item
      MapToPrimitive.Entry itemEntry = items.getEntry(item);

      /*
      if (debug) {
        System.err.println("got item with logProb " + itemEntry.getDoubleValue() +
                           "; comparing to " + item.logProb());
      }
      */
      boolean itemExists = itemEntry != null;
      double oldItemProb =
        itemExists ? itemEntry.getDoubleValue() : Constants.logOfZero;
      if (!itemExists ||
          (itemExists && itemEntry.getDoubleValue() < item.logProb())) {
        boolean removedOldItem = false;
        if (itemExists) {
          // replace the key with the new item and set the new item's map
          // value to be its logProb
          Item oldItem = (Item)itemEntry.getKey();
          boolean replaced = itemEntry.replaceKey(item);
          if (replaced) {
            itemEntry.set(0, item.logProb());
            // cannot reclaim item, since caller may still have handle to it
            oldItem.setGarbage(true);
            // garbageItems.add(oldItem);
            removedOldItem = true;
            added = true;
          }
          else
            System.err.println(className +
                               ": assertion failed: couldn't replace item" +
                               "\n\t" +
                               "oldItem.equals(item)=" + oldItem.equals(item) +
                               "; " +
                               "item.equals(oldItem)=" + item.equals(oldItem) +
                               "\n\toldItem: " + oldItem + "\n\titem: " + item);
        }
        else {
          added = true;
          items.put(item, item.logProb());
        }
        // if we removed an old item, there's no net gain in number of items
        if (!removedOldItem) {
          totalItems++;
        }
        // update top prob
        if (item.logProb() > chart[start][end].topLogProb) {
          chart[start][end].topLogProb = item.logProb();
          chart[start][end].topItem = item;
        }
      }

      if (debugAddToChart) {
        if (itemExists) {
          //double itemProb = itemEntry.getDoubleValue();
          double itemProb = oldItemProb;
          if (itemProb < item.logProb()) {
            System.err.println(className + ": adding item because equivalent " +
                               "item exists with lower prob; existing item " +
                               "prob=" + oldItemProb +
                               "; item to add prob=" + item.logProb());
          }
          else if (itemProb == item.logProb()) {
            System.err.println(className + ": not adding item because " +
                               "equivalent item exists with " +
                               "equal prob; prob=" + itemProb);
            System.err.println("\titem that was to be added(" +
                               start + "," + end + "): " + item);
            if (added)
              System.err.println("bad: we set added to true when we didn't add");
          }
          else {
            System.err.println(className + ": not adding item because " +
                               "equivalent item exists with higher prob; " +
                               "existing item prob=" + itemProb +
                               "; item to add prob=" + item.logProb());
            System.err.println("\titem that was to be added(" +
                               start + "," + end + "): " + item);
            if (added)
              System.err.println("bad: we set added to true when we didn't add");
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
   * Returns the item with the highest log probability covering the specified
   * span, or <code>null</code> if this span has no items.
   */
  public Item getTopItem(int start, int end) {
    return chart[start][end].topItem;
  }

  /**
   * Resets the highest log probability of the specified span to be
   * {@link Constants#logOfZero}.
   *
   * @param start the beginning of the span whose highest log prob is to be
   * reset
   * @param end the end of the span whose highest log prob is to be
   * reset
   */
  public void resetTopLogProb(int start, int end) {
    chart[start][end].topLogProb = Constants.logOfZero;
    chart[start][end].topItem = null;
  }

  /**
   * Returns the number of chart items covering the specified span.
   *
   * @param start the start of the span for which to retrieve the number of
   * items
   * @param end the end of the span for which to retrieve the number of items
   * @return the number of items in this chart covering the specified span
   */
  public int numItems(int start, int end) {
    return chart[start][end].map.size();
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

  /**
   * Reclaims this chart item.  This method returns the specified item
   * to the object pool of available items.
   */
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

  /**
   * Reclaims all items currently in the chart, as well as garbage items
   * generated during parsing (see {@link #add(int,int,Item)} for a
   * discussion of garbage items).
   */
  protected void reclaimItemsInChart() {
    int numCells = 0, maxCellSize = 0, maxCellStart = -1, maxCellEnd = -1;
    if (debugNumPrunedItems) {
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
          reclaimItemCollection(map.keySet());
          if (debugCellSize) {
            if (map.size() > 0)
              numCells++;
            if (map.size() > maxCellSize) {
              maxCellSize = map.size();
              maxCellStart = i;
              maxCellEnd = j;
            }
          }
        }
      }
      /*
      int numGarbageItems = garbageItems.size();
      for (int i = 0; i < numGarbageItems; i++) {
        Item item = (Item)garbageItems.get(i);
        item.setGarbage(false); // reset this item's garbage status
      }
      itemPool.putBackAll(garbageItems);
      garbageItems.clear();
      */
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
			   "; max. cell size: " + maxCellSize +
                           "; max cell at [" + maxCellStart + "," +
                           maxCellEnd + "]");
      /*
      if (maxCellStart == maxCellEnd) {
        Iterator it = chart[maxCellStart][maxCellEnd].map.keySet().iterator();
        while (it.hasNext())
          System.err.println(it.next());
      }
      */
    }

    System.err.println("num cache adds: " + Model.numCacheAdds +
                       "; num canonical hits: " + Model.numCanonicalHits);
    Model.numCacheAdds = Model.numCanonicalHits = 0;
  }

  /**
   * Indicates that the chart should prune.  THis method may be invoked
   * during parsing.
   */
  public void doPruning() { pruning = true; }
  /**
   * Tells this chart not to do any pruning.  This method may be invoked
   * during parsing.
   */
  public void dontDoPruning() { pruning = false; }

  /**
   * A hook called by {@link #reclaimItemsInChart()} to allow subclasses
   * to reclaim each span's collection of chart items.
   */
  protected abstract void reclaimItemCollection(Collection c);

  /**
   * Sets up the item object pools.  This allows subclasses to specify
   * the type of item to be held in the object pool.
   */
  protected abstract void setUpItemPool();
}
