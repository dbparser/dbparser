package danbikel.util;

/**
 * Specification of a single method to allow for an arbitrary object filter.
 */
public interface Filter {
  /**
   * Returns <code>true</code> if this filter allows the  specified object to
   * pass through.
   */
  public boolean pass(Object obj);
}
