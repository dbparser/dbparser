package danbikel.util;

/**
 * A trivial filter that lets all objects pass through.
 */
public class AllPass implements Filter {
  /**
   * Constructs a new filter that lets all objects pass through.
   */
  public AllPass() {}

  /**
   * Returns <code>true</code> regardless of the value of the specified
   * object (lets all objects pass through).
   */
  public boolean pass(Object obj) { return true; }
}
