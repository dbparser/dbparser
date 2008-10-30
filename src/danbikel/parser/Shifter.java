package danbikel.parser;

import danbikel.lisp.*;

import java.util.Map;

/**
 * A class containing only static methods that mirror the signatures of the
 * {@link Shift} interface, allowing a convenient flow-through mechanism to an
 * internal static {@link Shift} object, the exact type of which is determined
 * by the value of {@link Settings#shifterClass}.
 */
public class Shifter {

  private Shifter() {}

  private static final String className = Shifter.class.getName();
  private static Shift shifter = getShifter();

  private static Shift getShifter() {
    Shift shifter;
    String shifterStr = Settings.get(Settings.shifterClass);
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
    return shifter;
  }

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.shifterClass)) {
	  shifter = getShifter();
	}
      }
    };
    Settings.register(Shifter.class, change, null);
  }

  /**
   * Uses the internal {@link Shifter} instance to shift the newly-generated
   * (and therefore previously-generated) modifier into the history, which is a
   * {@link SexpList}.
   *
   * @param event   the event whose history is being updated
   * @param list    the history list from the event to be updated
   * @param prevMod the previously-generated modifier
   */
  public static void shift(TrainerEvent event, SexpList list, Sexp prevMod) {
    shifter.shift(event, list, prevMod);
  }

  /**
   * Uses the internal {@link Shifter} instance to shift the newly-generated
   * (and therefore previously-generated) head word into the history, which is a
   * {@link WordList}.
   *
   * @param event    the event whose history is being updated
   * @param wordList the history list from the event to be updated
   * @param prevWord the previously-generated modifier
   */
  public static void shift(TrainerEvent event, WordList wordList,
			   Word prevWord) {
    shifter.shift(event, wordList, prevWord);
  }

  /**
   * Uses the internal {@link Shifter} instance to determine whether the
   * specified modifier should be skipped when constructing a history for the
   * specified chart item.
   *
   * @param item    the chart item whose history is being constructed
   * @param prevMod the previously-generated modifier to be tested
   * @return whether the specified modifier should be skipped when constructing
   *         the previous-modifier history
   */
  public static boolean skip(CKYItem item, Sexp prevMod) {
    return shifter.skip(item, prevMod);
  }

  /**
   * Uses the internal {@link Shifter} instance to determine whether the
   * specified modifier head word should be skipped when constructing a history
   * for the specified chart item.
   *
   * @param item     the chart item whose history is being constructed
   * @param prevWord the previously-generated modifier head word to be tested
   * @return whether the specified modifier head word should be skipped when
   *         constructing the previous-modifier head word history
   */
  public static boolean skip(CKYItem item, Word prevWord) {
    return shifter.skip(item, prevWord);
  }
}
