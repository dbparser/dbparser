package danbikel.util;

import java.text.*;

/**
 * A simple class for keeping track of wall-clock time.  This class
 * also contains a utility method for converting a <code>long</code>
 * representing milliseconds to a string representation of minutes,
 * seconds and milliseconds.
 *
 * @see #elapsedTime(long)
 */
public class Time {
  // number formatters (for timing output)
  private static NumberFormat longNF = NumberFormat.getInstance();
  private static NumberFormat doubleNF = NumberFormat.getInstance();
  static {
    doubleNF.setMinimumFractionDigits(3);
    doubleNF.setMaximumFractionDigits(3);
    doubleNF.setMinimumIntegerDigits(2);
    longNF.setMinimumIntegerDigits(2);
  }

  // data members
  private long startTime;

  /** Creates a <code>Time</code> object whose start time is
      <code>System.currentTimeMillis</code>. */
  public Time() {
    this(System.currentTimeMillis());
  }

  /** Creates a <code>Time</code> object with the specified start time. */
  public Time(long startTime) {
    this.startTime = startTime;
  }

  /** Resets the internal start time to be the current time. */
  public void reset() {
    this.startTime = System.currentTimeMillis();
  }

  /** Returns a string representation of the elapsed time since the start
      time of this <code>Time</code> object, using the output of
      {@link #elapsedTime(long)}. */
  public String toString() {
    return elapsedTime(System.currentTimeMillis() - startTime);
  }

  /** Returns the start time of this object. */
  public long startTime() { return startTime; }

  /**
   * Returns a string representing the length of the specified time
   * of the form <pre>MM:SS.mmm</pre>
   * where <tt>MM</tt> is the number of minutes, <tt>SS</tt> is the nubmer
   * of seconds and <tt>mmm</tt> is the number of milliseconds.
   */
  public static final String elapsedTime(long elapsedMillis) {
    long minutes = elapsedMillis / 60000;
    double seconds = (elapsedMillis / 1000.0) - (minutes * 60);
    return longNF.format(minutes) + ":" + doubleNF.format(seconds);
  }

  /** An alias for <code>System.currentTimeMillis</code>. */
  public static final long current() { return System.currentTimeMillis(); }
}
