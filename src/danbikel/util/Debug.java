package danbikel.util;

import bsh.Interpreter;
import java.io.*;
import java.util.*;

/**
 * <code>Debug</code> is a static class that stores the current debugging level
 * (default is zero, for no debugging output), debugging options, and other
 * utility functions for debugging.  Its main function also allows the use
 * of BeanShell to perform on-the-fly debugging tests.
 */
public class Debug implements Serializable {
  // data members
  public static int counter = 0;  // to count stuff
  public static int level = 0;

  public static void setLevel(int newLevel) {
    level = newLevel;
  }

  public static void setOutputStream(PrintStream err) {
    System.setErr(err);
  }
  public static void fillStringArray(String[] arr, String filler) {
    StringTokenizer st = new StringTokenizer(filler);
    for (int i = 0; st.hasMoreTokens(); i++)
      arr[i] = st.nextToken();
  }

  public static void main(String[] args) {
    Interpreter i = new Interpreter();
    try {
      i.eval("server(" + args[0] + ")");
    } catch (bsh.EvalError e) { System.err.println(e); }
  }
}
