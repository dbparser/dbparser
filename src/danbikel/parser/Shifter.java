package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * A class containing only static methods that mirror the signatures of the
 * {@link Shift} interface, allowing a convenient flow-through mechanism to
 * an internal static {@link Shift} object, the exact type of which is
 * determined by the value of {@link Settings#shifterClass}.
 */
public class Shifter {
  private static final String className = Shifter.class.getName();
  private static Shift shifter;

  static {
    String shifterStr= Settings.get(Settings.shifterClass);
    if (shifterStr != null) {
      try {
	shifter = (Shift)Class.forName(shifterStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + shifterStr + ":\n\t" + e +
			   "\n\tusing DefaultShifter instead");
	shifter = new DefaultShifter();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.shifterClass + " was not set;\n\t" +
			 "using DefaultShifter");
      shifter = new DefaultShifter();
    }
  }

  public static void shift(TrainerEvent event, SexpList list,
		    Sexp prevMod, Sexp currMod) {
    shifter.shift(event, list, prevMod, currMod);
  }
  public static void shift(TrainerEvent event, WordList wordList,
		    Word prevWord, Word currWord) {
    shifter.shift(event, wordList, prevWord, currWord);
  }

  public static boolean skip(CKYItem item, Sexp prevMod, Sexp currMod) {
    return shifter.skip(item, prevMod, currMod);
  }
  public static boolean skip(CKYItem item, Word prevWord, Word currWord) {
    return shifter.skip(item, prevWord, currWord);
  }
}
