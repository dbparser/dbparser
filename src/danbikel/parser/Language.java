package danbikel.parser;

import java.lang.*;
import java.util.Map;

/**
 * Provides objects that perform functions specific to a particular language
 * and/or Treebank.  When the method {@link #setLanguage} is called,
 * several objects from a language package are created and stored by
 * this class.  A language package must provide implementations for the
 * following interfaces:
 * <ul>
 * <li>{@link WordFeatures}
 * <li>{@link Treebank}
 * <li>{@link HeadFinder}
 * <li>{@link Training}
 * </ul>
 * Upon initialization, this class will set the language to be the default
 * language, which is English, using classes from the default language package,
 * <code>danbikel.parser.english</code>, using the method {@link #setLanguage}.
 *
 * @see #setLanguage
 * @see Settings
 * @see Settings#language
 * @see Settings#languagePackage */
public class Language implements Runtime.Language, Settings.Change {
  // data members
  /** The <code>Wordfeatures</code> object for the current language. */
  private WordFeatures wordFeatures;
  /** The <code>HeadFinder</code> object for the current language. */
  private HeadFinder headFinder;
  /** The <code>Treebank</code> object for the current language. */
  private Treebank treebank;
  /** The <code>Training</code> object for the current language. */
  private Training training;

  private String encoding;
  private String lang;
  private String langPackage;


  public Language(Settings settings) {
    setEncoding(settings);
    setLanguage(settings);
    settings.register(this);
  }
  
  public void update(Map<String, String> changedSettings, Settings settings) {
    if (changedSettings.containsKey(Settings.language) ||
	changedSettings.containsKey(Settings.languagePackage)) {
      setEncoding(settings);
      setLanguage(settings);
      System.err.println(Language.class.getName() +
			 ": language has changed to " + getLanguage() +
			 ";\n\tnew encoding: " + encoding +
			 ";\n\tnew language classes:" +
			 "\n\t\t" + treebank().getClass().getName() +
			 "\n\t\t" + training().getClass().getName() +
			 "\n\t\t" + headFinder().getClass().getName() +
			 "\n\t\t" + wordFeatures().getClass().getName());
    }
  }

  private void setEncoding(Settings settings) {
    String fileEncodingProperty =
      Settings.fileEncodingPrefix + settings.get(Settings.language);
    encoding = settings.get(fileEncodingProperty);
    if (encoding == null)
      encoding = System.getProperty("file.encoding");
  }


  // accesssors for the above static objects (for classes outside this package)
  /** Gets the <code>WordFeatures</code> object for the current language. */
  public WordFeatures wordFeatures() { return wordFeatures; }
  /** Gets the <code>HeadFinder</code> object for the current language. */
  public HeadFinder headFinder() { return headFinder; }
  /** Gets the <code>Treebank</code> object for the current language. */
  public Treebank treebank() { return treebank; }
  /** Gets the <code>Training</code> object for the current language. */
  public Training training() { return training; }

  /**
   * Gets the file encoding for the current language.<br>
   * If the value
   * <code>Settings.get(Settings.fileEncodingPrefix + Settings.language)</code>
   * is non-<code>null</code>, then it is used as the file encoding; otherwise,
   * the file encoding is to be the value of<br>
   * <code>System.getProperty(&quot;file.encoding&quot;)</code>.
   *
   * @see Settings#fileEncodingPrefix
   * @see Settings#language
   */
  public String encoding() { return encoding; }

  /**
   * Sets the language and language package using the values obtained from the
   * {@link Settings} class.  The language to be set is determined by the value
   * of the <code>parser.language</code> property stored in {@link Settings}.
   * The language package to be set is determined by the value of the
   * <code>parser.language.package</code> property stored in
   * <code>Settings</code>.
   * <p/>
   * A language package is required to provide concrete subclasses of the
   * following abstract classes: <ol> <li>{@link WordFeatures} <li>{@link
   * Treebank} <li>{@link HeadFinder} <li>{@link Training} </ol> This method
   * will create one object of each of the required language package classes
   * using the classes' respective default constructors. The objects are created
   * in the order listed above, so any dependencies in a language package must
   * be from later-instantiated to earlier-instantiated classes.
   * <p/>
   * The class names of the concrete classes in a language package are assumed
   * to be identical to those listed above, prepended with the string
   * <pre>Settings.get(Settings.languagePackage)&nbsp;+&nbsp;"."</pre>
   * If a particular concrete subclass has a different name from the abstract
   * class it extends, the appropriate {@link Settings} property must be set
   * containing the <i>fully-qualified</i> version of the class name: <ul>
   * <li>{@link Settings#wordFeaturesClass} <li>{@link Settings#treebankClass}
   * <li>{@link Settings#headFinderClass} <li>{@link Settings#trainingClass}
   * </ul>
   *
   * @param settings the settings to use when setting up this {@link
   *                 Runtime.Language} instance
   */
  protected void setLanguage(Settings settings) {
    lang = settings.get(Settings.language);
    langPackage = settings.get(Settings.languagePackage);

    // finally, set static language components based on specified language
    try {

      // initialize WordFeatures object
      String wordFeaturesClass = settings.get(Settings.wordFeaturesClass);
      if (wordFeaturesClass == null)
	wordFeaturesClass = langPackage + ".WordFeatures";
      wordFeatures =
	(WordFeatures)Class.forName(wordFeaturesClass).newInstance();

      // initialize Treebank object
      String treebankClass = settings.get(Settings.treebankClass);
      if (treebankClass == null)
	treebankClass = langPackage + ".Treebank";
      treebank =
	(Treebank)Class.forName(treebankClass).newInstance();

      // initialize HeadFinder object
      String headFinderClass = settings.get(Settings.headFinderClass);
      if (headFinderClass == null)
	headFinderClass = langPackage + ".HeadFinder";
      headFinder =
	(HeadFinder)Class.forName(headFinderClass).newInstance();

      // initialize Training object
      String trainingClass = settings.get(Settings.trainingClass);
      if (trainingClass == null)
	trainingClass = langPackage + ".Training";
      training =
	(Training)Class.forName(trainingClass).newInstance();
    }
    catch (InstantiationException ie) {
      System.err.println(ie);
    }
    catch (IllegalAccessException iae) {
      System.err.println(iae);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
  }

  /** Gets the name of the current language. */
  public String getLanguage() { return lang; }
  /** Gets the name of the current language package. */
  public String getLanguagePackage() { return langPackage; }
}
