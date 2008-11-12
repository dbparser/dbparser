package danbikel.parser;

import java.io.Serializable;

/**
 * An interface specifying the resources necessary at runtime.
 */
public interface Runtime extends Serializable {
  public interface Language extends Serializable {
    // elements of the language package (formerly singletons in Language class)
    public String encoding();
    public String getLanguage();
    public String getLanguagePackage();
    public WordFeatures wordFeatures();
    public HeadFinder headFinder();
    public Treebank treebank();
    public Training training();
  }

  public Settings settings();
  public Language language();
  // factories that used to be singletons
  public WordFactory words();
  public SubcatFactory subcats();
  // other necessary instances that used to be singletons
  public NonterminalMapper ntMapper();
  public Shift shifter();
}
