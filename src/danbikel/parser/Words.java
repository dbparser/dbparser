package danbikel.parser;

import danbikel.lisp.*;

public class Words {

  private final static String className = Words.class.getName();

  private static WordFactory factory;

  private Words() { }

  static {
    String wordFactStr = Settings.get(Settings.wordFactoryClass);
    if (wordFactStr != null) {
      try {
	factory = (WordFactory)Class.forName(wordFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + wordFactStr + ":\n\t" + e +
			   "\n\tusing DefaultWordFactory instead");
	factory = new DefaultWordFactory();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.wordFactoryClass + " was not set;\n\t" +
			 "using DefaultWordFactory");
      factory = new DefaultWordFactory();
    }
  }

  public static Word get(Sexp s) {
    return factory.get(s);
  }
  public static Word get(Symbol word, Symbol tag) {
    return factory.get(word, tag);
  }

  public static Word get(Symbol word, Symbol tag, Symbol features) {
    return factory.get(word, tag, features);
  }
}