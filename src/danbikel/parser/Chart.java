package danbikel.parser;

import danbikel.lisp.*;
import java.util.*;
import java.io.Serializable;

/**
 * Provides the skeletal infrastructure for a chart indexed by start and
 * end words, as well as by arbitrary labels taken from the chart items.
 *
 * @see Item
 */

public abstract class Chart implements Serializable {
  // constants
  private final static int defaultChartSize = 200;

  // data members
  /**
   * A chart is a two-dimensional array of maps, each of which is a map
   * from labels to SortedMap objects containing Item objects, where
   * Items are sorted via their logProbs (their natural ordering).
   */
  protected Map[][] chart;
  /** The current size of the chart. */
  protected int size;
  protected int cellLimit = -1;
  protected double pruneFact = 0.0;

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
    chart = new Map[size][size];
    this.size = size;
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
      chart = new Map[size][size];
    }
  }

  /**
   * Checks every map of the chart covering a span less than or equal to
   * size and clears it; if a chart entry is <code>null</code>, then a
   * new map is created.
   */
  protected void clear() {
    for (int i = 0; i < size; i++)
      for (int j = 0; j < size; j++)
	if (chart[i][j] == null)
	  chart[i][j] = new HashMap();
	else
	  chart[i][j].clear();
  }

  /**
   * Returns <code>true</code> if the specified chart item should not
   * be added to the specified set of items sorted by their probability
   * because its probability is not within {@link #pruneFact} of
   * the highest-ranked item. This method is guaranteed to be called by
   * {@link #add(int,int,Item)} when a sorted set of chart items already
   * exists (and hence contains at least one chart item).
   *
   * @param items the sorted map of items in a cell of the chart
   * @param item the item being added to <code>items</code>
   */
  protected boolean toPrune(SortedMap items, Item item) {
    double topProb = ((Double)items.lastKey()).doubleValue();
    return item.logProb() < (topProb - pruneFact);
  }

  protected void prune(SortedMap items) {
    if (pruneFact > 0.0) {
      // remove the lowest probability elements until the lowest one
      // left is within pruneFact of highest
      // N.B.: since this method is called every time an item is added to
      // to the chart, the while loop should only have at most one iteration
      double topProb = ((Double)items.lastKey()).doubleValue();
      double lowestProbKey = topProb - pruneFact;
      Iterator it = items.keySet().iterator();
      while (it.hasNext()) {
        Double currKey = (Double)it.next();
        if (currKey.doubleValue() < lowestProbKey)
          it.remove();
        else
          break;
      }
    }
    if (cellLimit > 0) {
      if (items.size() > cellLimit) { // don't create iterator if no need

        // remove lowest prob items until the set size is equal to the limit
        // N.B.: since this method is called every time an item is added to
        // the chart, the while loop should only have at most one iteration
        Iterator it = items.keySet().iterator();
        while (it.hasNext() && items.size() > cellLimit)
          it.remove();
      }
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
   */
  public void add(int start, int end, Item item) {
    Map map = chart[start][end];
    Object label = item.label();
    SortedMap items = (SortedMap)map.get(label);
    if (items == null) {
      items = new TreeMap();
      map.put(label, items);
      items.put(item, new Double(item.logProb()));
    }
    else if (!toPrune(items, item)) {
      // see if equal (technically, equivalent) item already exists
      // but has a lower probability, replace it with current item
      Double itemProb = (Double)items.get(item);
      if (itemProb == null ||
          (itemProb != null && itemProb.doubleValue() < item.logProb()))
	items.put(item, new Double(item.logProb()));
      prune(items);
    }
  }

  /**
   * Returns the set of items in this chart covering the specified span
   * and having the specified label.
   *
   * @param start the index of the first word in the span for which
   * to retrieve chart items
   * @param end the index of the last word in the span for which
   * to retrieve chart items
   * @param label the label of chart items covering the
   * span <code>start</code>-<code>end</code> that are to be retrieved
   * @return the set of chart items covering the specified span
   * and having the specified label, sorted by their probability
   * (the last item in the set has the highest probability), or
   * <code>null</code> if there is no such set of items
   */
  public SortedMap get(int start, int end, Object label) {
    return (SortedMap)chart[start][end].get(label);
  }
}
