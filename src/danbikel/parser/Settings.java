package danbikel.parser;

import danbikel.util.Text;
import danbikel.switchboard.SwitchboardRemote;
import java.net.URL;
import java.util.*;
import java.io.*;

/**
 * Provides static settings for this package, primarily via an internal
 * {@link Properties} object.  All recognized properties of this package and
 * the provide language packages are provided as publicly-accessible constants.
 * <p>
 * A settings file for a particular language must provide the property
 * <code>headTablePrefix + language</code>.<br> A settings file for a
 * particular language should normally also provide the property
 * <code>fileEncodingPrefix + language</code> to override the default file
 * encoding as determined by the locale of the Java VM.  Settings files
 * for a particular language and/or Treebank may contain any other settings
 * required by a language package.
 * <p>
 * Variable expansion is performed on property values as in Java security
 * policy files, with the additional provision that properties defined earlier
 * in a settings file can be used as variable names in subsequent lines of the
 * settings file.  See {@link Text#expandVars(Properties,StringBuffer)} for
 * what variables are allowed in the definitions of property values.
 * <p>
 * Upon intialization, this class attempts to read default parser settings from
 * the file <code>settings</code> inside the default settings directory,
 * <tt>$HOME/.db-parser</tt>, where <tt>$HOME</tt> is ther user's home
 * directory.  If either the default settings directory or the default settings
 * file is missing, this class will use fallback default settings from a
 * resource that is bundled with this package.
 * <p>
 * To obtain a default settings file as a template for modification, put the
 * following code in a file called <tt>GetSettings.java</tt> and then run
 * <tt>java&nbsp;GetDefaultSettings</code> from the command line or your
 * Java development environment:
 * <pre>
 * import danbikel.parser.Settings;
 * import java.io.*;
 * public class GetDefaultSettings {
 *   public static void main(String[] args) {
 *     try {
 *       InputStream is = Settings.getDefaultsResource();
 *       BufferedReader reader = new BufferedReader(new InputStreamReader(is));
 *       for (String line = null; (line = reader.readLine()) != null; )
 *	   System.out.println(line);
 *     }
 *     catch (IOException ioe) { System.err.println(ioe); }
 *   }
 * }
 * </pre>
 *
 * @see #headTablePrefix
 * @see #fileEncodingPrefix
 * @see Language#encoding
 * @see #settingsDirOverride
 */
public class Settings implements Serializable {

  private Settings() {}

  // constants

  private final static String className = Settings.class.getName();

  /** The official name of this program. */
  public final static String progName = "WordNet Parser";
  /** The official version of this program. */
  public final static String version = "0.8";
  /**
   * The prefix that all properties for this parser should have
   * (to be used when finding system properties that are meant to be included
   * here).
   */
  private final static String globalPropertyPrefix = "parser.";

  /**
   * The name of the property to override the location of the default settings
   * directory, to be specified at run-time on the command line.  The default
   * settings directory is <tt>$HOME/.db-parser</tt>, where <tt>$HOME</tt> is
   * the user's home directory.
   * The value of this constant is <code>&quot;parser.settings.dir&quot;</code>.
   * <p>
   * Example UNIX usage:
   * <pre>
   *     java -Dparser.settings.dir=$HOME/.tmp-settings ...
   * </pre>
   */
  public final static String settingsDirOverride = "parser.settings.dir";

  /**
   * The property to specify the language to be parsed.
   * <p>
   * The value of this constant is <code>&quot;parser.language&quot;</code>.
   *
   * @see Language
   */
  public final static String language = "parser.language";

  /**
   * The property to specify the language package to be used.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.language.package&quot;</code>.
   *
   * @see Language
   */
  public final static String languagePackage = "parser.language.package";

  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link WordFeatures} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".WordFeatures"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.wordFeatures&quot;</code>.
   *
   * @see Language
   */
  public final static String wordFeaturesClass = "parser.language.wordFeatures";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link Treebank} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".Treebank"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.treebank&quot;</code>.
   *
   * @see Language
   */
  public final static String treebankClass = "parser.language.treebank";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link HeadFinder} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".HeadFinder"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.headFinder&quot;</code>.
   *
   * @see Language
   */
  public final static String headFinderClass = "parser.language.headFinder";
  /**
   * The property to specify the fully-qualified name of the class that
   * extends {@link Training} in a language package.  If this property
   * is set, it will override the default, which is
   * <pre>Settings.get(Settings.languagePackage) + ".Training"</pre>
   * The value of this constant is
   * <code>&quot;parser.language.training&quot;</code>.
   *
   * @see Language
   */
  public final static String trainingClass = "parser.language.training";


  /**
   * The prefix string used to specify a language's head table
   * property. For example, for English, the head table filename is available
   * by calling
   * <pre>
   *   Settings.get(Settings.headTablePrefix + "english");
   * </pre>
   * The value of this constant is <code>&quot;parser.headtable.&quot;</code>.
   *
   * @see HeadFinder
   */
  public final static String headTablePrefix = "parser.headtable.";

  /**
   * The property to specify the fully-qualified classname of the
   * <code>SubcatFactory</code> object to be used by the <code>Subcats</code>
   * static factory class.
   *
   * @see Subcat
   * @see Subcats
   * @see SubcatFactory
   */
  public final static String subcatFactoryClass = "parser.subcatFactoryClass";

  /**
   * The prefix string used to specify a language's file encoding
   * property.  For example, for Chinese, the file encoding is available
   * by calling
   * <pre>
   *  Settings.get(Settings.fileEncodingPrefix + "chinese");
   * </pre>
   * The value of this constant is
   * <code>&quot;parser.file.encoding.&quot;</code>.
   *
   * @see Language#encoding
   */
  public final static String fileEncodingPrefix = "parser.file.encoding.";

  /**
   * The property to specify whether the method
   * {@link HeadFinder#defaultFindHead(Symbol,SexpList)} issues a warning
   * whenever it needs to use the default head-finding rule.  The value of this
   * property should be (the string representation of) a boolean (conversion
   * is performed by the method <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.headfinder.warnDefaultRule"</code>.
   */
  public final static String headFinderWarnDefaultRule =
    "parser.headfinder.warnDefaultRule";

  /**
   * The property to specify the threshold at which words are
   * considered unknown by the trainer. The value of this property
   * must be (the string representation of) an integer.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.unknownWordThreshold"</code>.
   *
   * @see Trainer
   */
  public final static String unknownWordThreshold =
    "parser.trainer.unknownWordThreshold";

  /**
   * The property to specify the interval (in number of sentences) at which the
   * trainer emits reports to <code>System.err</code> when training.
   * The value of this property must be (the string representation of)
   * an integer.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.reportingInterval&quot;</code>.
   *
   * @see Trainer
   */
  public final static String trainerReportingInterval =
    "parser.trainer.reportingInterval";

  /**
   * The property to specify whether or not the trainer keeps all words.
   * Normally, words falling below a threshold are mapped to the unknown word.
   * The value of this property should be (the string representation of) a
   * boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.keepAllWords&quot;</code>.
   *
   * @see #unknownWordThreshold
   * @see Trainer
   */
  public final static String keepAllWords = "parser.trainer.keepAllWords";

  /**
   * The property to specify how many previous modifiers the trainer outputs
   * for its top-level count files.  The value of this property must be
   * (the string representation of) a non-negative integer.
   * <p>
   * The value of this constant is
   * <code>&quot;parser.trainer.numPrevMods&quot;</code>.
   * @see Trainer
   */
  public final static String numPrevMods =
    "parser.trainer.numPrevMods";

  /**
   * The property to specify the model structure number to use when
   * creating <code>ProbabilityStructure</code> objects.  The model
   * structure number will be appended to the end of the canonical
   * names of model structure class name prefixes.  For example, the
   * canonical class name prefix for the head generation model
   * structure is <code>HeadModelStructure</code>, so if this property
   * has a value of <tt>"1"</tt>, then the head model structure
   * instantiated by {@link Trainer#deriveCounts} will be the class
   * <code>HeadModelStructure1</code>.  The model structure numbers
   * for specific model structure classes can be overridden by
   * specifying one of the following, model structure-specific properties:
   * <ul>
   * <li> {@link #topNonterminalModelStructureNumber}
   * <li> {@link #topLexModelStructureNumber}
   * <li> {@link #headModelStructureNumber}
   * <li> {@link #gapModelStructureNumber}
   * <li> {@link #leftSubcatModelStructureNumber}
   * <li> {@link #rightSubcatModelStructureNumber}
   * <li> {@link #modNonterminalModelStructureNumber}
   * <li> {@link #modWordModelStructureNumber}
   * </ul>
   * If the user wishes to use model structure classes outside
   * this package, the following properties may be used to specify
   * fully-qualified model structure classnames, which will override all model
   * structure number property settings (the canonical classname prefixes
   * will not be used):
   * <ul>
   * <li> {@link #topNonterminalModelStructureClass}
   * <li> {@link #topLexModelStructureClass}
   * <li> {@link #headModelStructureClass}
   * <li> {@link #gapModelStructureClass}
   * <li> {@link #leftSubcatModelStructureClass}
   * <li> {@link #rightSubcatModelStructureClass}
   * <li> {@link #modNonterminalModelStructureClass}
   * <li> {@link #modWordModelStructureClass}
   * </ul>
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.globalModelStructureNumber"</code>.
   */
  public final static String globalModelStructureNumber =
    "parser.trainer.globalModelStructureNumber";

  public final static String lexPriorModelStructureNumber =
    "parser.trainer.lexPriorModelStructureNumber";

  public final static String nonterminalPriorModelStructureNumber =
    "parser.trainer.nonterminalPriorModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head-generation submodel for heads whose parents are
   * {@link Training#topSym()}.  This number will be appended to the
   * canonical top nonterminal model structure classname prefix,
   * <code>"danbikel.parser.TopNonterminalModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.TopNonterminalModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topNonterminalModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #topNonterminalModelStructureClass
   */
  public final static String topNonterminalModelStructureNumber =
    "parser.trainer.topNonterminalModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head word-generation submodel for heads of entire sentences.  This
   * number will be appended to the canonical top lexical model
   * structure classname prefix,
   * <code>"danbikel.parser.TopLexModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.TopLexModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topLexModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #topLexModelStructureClass
   */
  public final static String topLexModelStructureNumber =
    "parser.trainer.topLexModelStructureNumber";

  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * head-generation submodel.  This number will be appended to the
   * canonical head model structure classname prefix,
   * <code>"danbikel.parser.HeadModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.HeadModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.headModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #headModelStructureClass
   */
  public final static String headModelStructureNumber =
    "parser.trainer.headModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * gap-generation submodel.  This number will be appended to the
   * canonical gap model structure classname prefix,
   * <code>"danbikel.parser.GapModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.GapModelStructure1"</code>.  This constant
   * overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.gapModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #gapModelStructureClass */
  public final static String gapModelStructureNumber =
    "parser.trainer.gapModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * left-subcat-generation submodel.  This number will be appended to
   * the canonical left-subcat model structure classname prefix,
   * <code>"danbikel.parser.LeftSubcatModelStructure"</code>, to form
   * a classname, such as
   * <code>"danbikel.parser.LeftSubcatModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.leftSubcatModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #leftSubcatModelStructureClass */
  public final static String leftSubcatModelStructureNumber =
    "parser.trainer.leftSubcatModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * right-subcat-generation submodel.  This number will be appended
   * to the canonical right-subcat model structure classname prefix,
   * <code>"danbikel.parser.RightSubcatModelStructure"</code>, to form
   * a classname, such as
   * <code>"danbikel.parser.RightSubcatModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.rightSubcatModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #rightSubcatModelStructureClass */
  public final static String rightSubcatModelStructureNumber =
    "parser.trainer.rightSubcatModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * modifying nonterminal-generation submodel.  This number will be
   * appended to the canonical modifying nonterminal model structure
   * classname prefix,
   * <code>"danbikel.parser.ModNonterminalModelStructure"</code>, to
   * form a classname, such as
   * <code>"danbikel.parser.ModNonterminalModelStructure1"</code>.
   * This constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modNonterminalModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #modNonterminalModelStructureClass */
  public final static String modNonterminalModelStructureNumber =
    "parser.trainer.modNonterminalModelStructureNumber";
  /**
   * The property to specify the model structure number to use when
   * creating the <code>ProbabilityStructure</code> object for the
   * modifying word-generation submodel.  This number will be appended
   * to the canonical modifying word model structure classname prefix,
   * <code>"danbikel.parser.ModWordModelStructure"</code>, to form a
   * classname, such as
   * <code>"danbikel.parser.ModWordModelStructure1"</code>.  This
   * constant overrides the setting of the
   * <code>globalModelStructureNumber</code> property.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modWordModelStructureNumber"</code>.
   *
   * @see #globalModelStructureNumber
   * @see #modWordModelStructureClass */
  public final static String modWordModelStructureNumber =
    "parser.trainer.modWordModelStructureNumber";


  public final static String lexPriorModelStructureClass =
    "parser.trainer.lexPriorModelStructureClass";

  public final static String nonterminalPriorModelStructureClass =
    "parser.trainer.nonterminalPriorModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head-generation submodel for heads whose parents
   * are {@link Training#topSym()}.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #topNonterminalModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topNonterminalModelStructureClass"</code>.
   */
  public final static String topNonterminalModelStructureClass =
    "parser.trainer.topNonterminalModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head word-generation submodel for head
   * words of entire sentences.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #topLexModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.topLexModelStructureClass"</code>.
   */
  public final static String topLexModelStructureClass =
    "parser.trainer.topLexModelStructureClass";

  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the head-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #headModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.headModelStructureClass"</code>.
   */
  public final static String headModelStructureClass =
    "parser.trainer.headModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the gap-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #gapModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.gapModelStructureClass"</code>.
   */
  public final static String gapModelStructureClass =
    "parser.trainer.gapModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the left subcat-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #leftSubcatModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.leftSubcatModelStructureClass"</code>.
   */
  public final static String leftSubcatModelStructureClass =
    "parser.trainer.leftSubcatModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the right subcat-generation submodel.  Specifying this
   * property overrides the {@link #globalModelStructureNumber} and
   * {@link #rightSubcatModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.rightSubcatModelStructureClass"</code>.
   */
  public final static String rightSubcatModelStructureClass =
    "parser.trainer.rightSubcatModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the modifying nonterminal-generation submodel.
   * Specifying this property overrides the {@link #globalModelStructureNumber}
   * and {@link #modNonterminalModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modNonterminalModelStructureClass"</code>.
   */
  public final static String modNonterminalModelStructureClass =
    "parser.trainer.modNonterminalModelStructureClass";
  /**
   * The property to specify the fully-qualified name of a class that
   * extends <code>ProbabilityStructure</code>, to be instantiated by
   * {@link Trainer} for the modifying word-generation submodel.
   * Specifying this property overrides the {@link #globalModelStructureNumber}
   * and {@link #modWordModelStructureNumber} properties.
   * <p>
   * The value of this constant is
   * <code>"parser.trainer.modWordModelStructureClass"</code>.
   */
  public final static String modWordModelStructureClass =
    "parser.trainer.modWordStructureModelClass";

  /**
   * The property to specify whether or not the decoding algorithm should
   * prune away chart entries within a particular factor of the top-ranked
   * chart entry in a given cell.  The value of this property should be (the
   * string representation of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.usePruneFactor"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderUsePruneFactor =
    "parser.decoder.usePruneFactor";

  /**
   * The property to specify the factor by which the decoder should prune
   * away chart entries.  The value of this property should be a floating
   * point number that is the logarithm (base 10) of the desired factor
   * (i.e., the factor employed will effectively be
   * <code>Math.pow(value, 10.0)</code>, where <code>value</code> is the value
   * of this property).  This form of pruning will only occur if the value of
   * {@link #decoderUsePruneFactor} is <code>true</code>.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.pruneFactor"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderPruneFactor =
    "parser.decoder.pruneFactor";

  /**
   * The property to specify whether the decoder should impose a limit on the
   * number of chart items per cell in the chart.  The value of this property
   * should be (the string representation of) a boolean (conversion is
   * performed by the method <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.useCellLimit"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderUseCellLimit =
    "parser.decoder.useCellLimit";

  /**
   * The property to specify the limit on the number of chart items the decoder
   * will have per cell in its chart.  This type of pruning will only occur
   * if the value of {@link #decoderUseCellLimit} is <code>true</code>.
   * The value of this property should be (the string representation of) an
   * integer.
   * <p>
   * The value of this constant is
   * <code>"parser.decoder.cellLimit"</code>.
   * <p>
   *
   * @see Decoder
   */
  public final static String decoderCellLimit =
    "parser.decoder.cellLimit";

  /**
   * The property to specify whether the decoder should employ a constraint
   * on the way commas can appear in and around chart items.  Specifically,
   * the constraint is that for a chart item that represents the CFG rule
   * <pre>Z --> &lt;.. X Y..&gt;</pre>
   * two of its children <tt>X</tt> and <tt>Y</tt> are separated by a comma,
   * then the last word in <tt>Y</tt> must be directly followed by a comma or
   * must be the last word in the sentence.
   */
  public final static String decoderUseCommaConstraint =
    "parser.decoder.useCommaConstraint";

  /**
   * The property to specify whether words are downcased during training
   * and decoding.  The value of this property should be (the string
   * representation of) a boolean (conversion is performed by the method
   * <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is <code>"parser.downcaseWords"</code>.
   */
  public final static String downcaseWords = "parser.downcaseWords";

  /**
   * The property to specify how long, in milliseconds, the SO_TIMEOUT
   * value should be for the switchboard's RMI-client (caller) sockets.
   *
   * @see SwitchboardRemote#socketTimeout
   */
  public final static String sbSocketTimeout = SwitchboardRemote.socketTimeout;

  /**
   * The property to specify how often clients and servers should ping
   * the "keep-alive" socket connected to the switchboard.  The value
   * of this property should be (the string representation of) an
   * integer, representing milliseconds between pings.
   *
   * @see SwitchboardRemote#keepAliveInterval
   */
  public final static String keepAliveInterval =
    SwitchboardRemote.keepAliveInterval;
  /**
   * The property to specify at most how many times the switchboard attempts to
   * contact clients and servers before considering them dead (after an
   * initial failure, thus making 0 a legal value for this property).
   *
   * @see SwitchboardRemote#keepAliveMaxRetries
   */
  public final static String keepAliveMaxRetries =
    SwitchboardRemote.keepAliveMaxRetries;

  /**
   * The property to specify whether the switchboard should kill all
   * of a server's clients when it detects that the server has died.
   * This property should have the value <tt>"false"</tt> when servers
   * are stateless. The value of this property should be (the string
   * representation of) a boolean (conversion is performed by the
   * method <code>Boolean.valueOf</code>).
   *
   * @see SwitchboardRemote#serverDeathKillClients
   */
  public final static String serverDeathKillClients =
  SwitchboardRemote.serverDeathKillClients;

  /**
   * The property to specify how long (in milliseconds) sockets stay alive
   * on the client (switchboard) side for RMI calls to switchboard user objects
   * (subclasses of {@link danbikel.switchboard.AbstractSwitchboardUser
   * AbstractSwitchboardUser}).
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.timeout"</code>.
   */
  public final static String sbUserTimeout = "parser.switchboardUser.timeout";

  /**
   * The property to specify at most how many times switchboard users
   * should try to acquire the switchboard from the bootstrap registry before
   * giving up, either when first starting up or in the event of a switchboard
   * crash.  If the value of this property is identical to the value of
   * {@link danbikel.switchboard.AbstractSwitchboardUser#infiniteTries}, the
   * parsing clients and decoder servers will indefinitely keep trying to
   * re-acquire the switchboard in the event of its failure.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.sbMaxRetries"</code>.
   */
  public final static String sbUserSBMaxRetries =
   "parser.switchboardUser.sbMaxRetries";

  /**
   * The property to specify whether parsing clients should have
   * server failover; that is, whether they should request a new server
   * from the switchboard if their current server fails.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.client.serverFailover"</code>.
   * <p>
   *
   * @see danbikel.switchboard.Failover
   */
  public final static String serverFailover =
    "parser.switchboardUser.client.serverFailover";

  /**
   * The property to specify at most how many times parsing clients should
   * re-try their servers in the event of a method failure before giving up.
   * If the value of this property is identical to the value of
   * {@link danbikel.util.proxy.Retry#retryIndefinitely}, clients will
   * keep re-trying indefinitely.
   * <p>
   * The value of this constant is
   * <code>"parser.switchboardUser.client.serverMaxRetries"</code>.
   * <p>
   *
   * @see danbikel.switchboard.AbstractSwitchboardUser.SBUserRetry
   * @see danbikel.util.proxy.Retry
   */
  public final static String serverMaxRetries =
    "parser.switchboardUser.client.serverMaxRetries";

  /**
   * The property to specify how many milliseconds to sleep between server
   * re-tries.
   * <p> The value of this constant is
   * <code>"parser.switchboardUser.client.serverRetrySleep"</code>.
   * <p>
   *
   * @see #serverMaxRetries
   */
  public final static String serverRetrySleep =
    "parser.switchboardUser.client.serverRetrySleep";

  // constants relating to the location of the settings directory and files
  private final static String settingsDirName = ".db-parser";
  private final static String defaultSettingsDirName =
    System.getProperty("user.home") + File.separator + settingsDirName;
  private final static String settingsFilename = "settings";
  private final static String dataDirFilename = "data";
  private final static String defaultSettingsFileHeader =
    " This default settings file created automatically by\n" +
    "#     " + progName + " v" + version;
  private final static String regularSettingsFileHeaderPrefix =
    " " + progName + " v" + version + " settings\n#";

  // data members

  private static Properties settings = new Properties();

  // static handles onto the settings dir and settings file
  private static File settingsDir = null;
  private static File settingsFile = null;

  static {
    String settingsDirName = System.getProperty(settingsDirOverride);
    if (settingsDirName == null)
      settingsDirName = defaultSettingsDirName;

    settingsDir = new File(settingsDirName);

    if (!settingsDir.exists()) {
      if (System.getProperty(settingsDirOverride) != null)
	System.err.println(className + ": warning: settings directory " +
			   settingsDir + "doesn't exist");
    }
    else if (!settingsDir.isDirectory())
      System.err.println(className + ": warning: " + settingsDir +
			 " exists but is not a directory");

    // load fallback defaults from resource, then try to grab settings
    // from settings file in default location, then grab system settings
    // with prefix "parser."
    try {
      load(new BufferedInputStream(getDefaultsResource()));
      InputStream defaultSettingsStream = getSettingsStream();
      if (defaultSettingsStream != null)
	load(new BufferedInputStream(defaultSettingsStream));
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(className + ": error: couldn't load bootstrap " +
			 "default settings");
      System.err.println(className + ": please check the integrity of the " +
			 "distribution of");
      System.err.println(className + ": this package");
      throw new RuntimeException(fnfe.toString());
    }
    catch (IOException ioe) {
      System.err.println(className + ": error: couldn't load bootstrap " +
			 "default settings");
      throw new RuntimeException(ioe.toString());
    }
    Iterator systemProps = System.getProperties().keySet().iterator();
    while (systemProps.hasNext()) {
      String property = (String)systemProps.next();
      if (property.startsWith(globalPropertyPrefix))
	set(property, System.getProperty(property));
    }
  }

  /**
   * Loads the properties from the file of the specified filename, using
   * {@link Properties#load(InputStream)}.
   *
   * @param filename the name of the file containing properties to load
   */
  public static void load(String filename) throws IOException {
    load(new File(filename));
  }

  /**
   * Loads the properties from the specified file, using
   * {@link Properties#load(InputStream)}.
   *
   * @param file the file containing properties to load
   */
  public static void load(File file) throws IOException {
    try {
      BufferedInputStream bis =
	new BufferedInputStream(new FileInputStream(file));
      load(bis);
      bis.close();
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
  }

  /**
   * Loads the properties from the specified input stream, using
   * {@link Properties#load(InputStream)}.
   *
   * @param is the input stream containing properties to load
   */
  public static void load(InputStream is) throws IOException {
    settings.load(is);
    Enumeration e = settings.propertyNames();
    while (e.hasMoreElements()) {
      String property = (String)e.nextElement();
      set(property, (String)settings.getProperty(property));
    }
  }

  /**
   * Stores the properties of this class to the specified output stream,
   * using {@link Properties#store(OutputStream,String)}.
   *
   * @param os the output stream to which to write the properties contained in
   * this class
   */
  public static void store(OutputStream os) throws IOException {
    settings.store(os, defaultSettingsFileHeader);
  }

  /**
   * Stores the properties of this class to the specified output stream,
   * using {@link Properties#store(OutputStream,String)}.
   *
   * @param os the output stream to which to write the properties contained in
   * this class
   * @param header the header text to put at the beginning of the
   * properties file
   */
  public static void store(OutputStream os, String header) throws IOException {
    settings.store(os, regularSettingsFileHeaderPrefix + header);
  }

  public static void store(ObjectOutputStream os) throws IOException {
    os.writeObject(settings);
  }

  public static void storeSorted(OutputStream os) throws IOException {
    storeSorted(settings, os);
  }

  public static void storeSorted(Properties props, OutputStream os)
    throws IOException {
    storeSorted(props, os, defaultSettingsFileHeader);
  }

  public static void storeSorted(OutputStream os, String header)
    throws IOException {
    storeSorted(settings, os, header);
  }

  public static void storeSorted(Properties props,
				 OutputStream os, String header)
    throws IOException {
    PrintStream ps = new PrintStream(os);
    SortedMap propMap = new TreeMap();
    propMap.putAll(props);
    ps.println("#" + header);
    Iterator it = propMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      ps.println(entry.getKey() + " = " + entry.getValue());
    }
    ps.flush();
  }

  /**
   * Sets the property <code>name</code> to <code>value</code>, using
   * {@link Properties#setProperty(String,String)}.
   *
   * @param name the name of the property to set
   * @param value the value to which to set the property <code>name</code>
   */
  public static void set(String name, String value) {
    StringBuffer valueBuffer = new StringBuffer(value);
    Text.expandVars(settings, valueBuffer);
    settings.setProperty(name, valueBuffer.toString());
  }

  /**
   * Gets the value of the specified property.
   *
   * @param name the name of the property to get
   * @return the value of the specified property
   */
  public static String get(String name) {
    return settings.getProperty(name);
  }

  /** Returns a copy of the internal <code>Properties</code> object. */
  public static Properties getSettings() {
    Properties settingsCopy = new Properties();
    settingsCopy.putAll(settings);
    return settingsCopy;
  }

  /** Allows a class in this package to set the settings of this class
      directly using the specified <code>Properties</code> object. */
  static void setSettings(Properties newSettings) {
    Iterator entries = newSettings.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      set((String)entry.getKey(), (String)entry.getValue());
    }
  }

  /**
   * Attempts to locate the file or resource with the specified name in
   * one of three places:
   * <ol>
   * <li> as a file path relative to <code>$HOME/.db-parser</code>
   * <li> as a file path relative to the current working directory, or
   *      relative to nothing, if <code>name</code> is an absolute path
   * <li> as a resource gotten from the class loader of the specified class
   * </ol>
   * where <code>$HOME</code> is the value of the system property
   * <code>user.home</code>.
   *
   * @param cl the class that needs the file or resource
   * @param name the name of the file or resource
   * @return an <code>InputStream</code> of the specified file or resource,
   * or <code>null</code> if the file or resource could not be found
   */
  public final static InputStream getFileOrResourceAsStream(Class cl,
							    String name)
    throws FileNotFoundException {

    File file1 = new File(Settings.settingsDir + File.separator + name);
    File file2 = new File(name);
    File file = (file1.exists() ? file1 :
		 (file2.exists() ? file2 : null));
    if (file != null)
      return new FileInputStream(file);
    else {
      // try to access as resource
      URL resourceURL = cl.getResource(name);
      InputStream is = null;
      if (resourceURL != null) {
	try { is = resourceURL.openStream(); }
	catch (IOException ioe) { is = null; }
      }
      if (is == null) {
	System.err.println(cl.getName() + ": warning: couldn't " +
			   "find \"" +
			   name + "\" in\n\t" +
			   file1 + "\n\t" + file2 + "\n" +
			   "or as the resource\n\t" +
			   resourceURL);
	throw new FileNotFoundException(name);
      }
      System.err.println("Reading from resource " + resourceURL);
      return is;
    }
  }

  /**
   * Gets the fallback defaults from resource, thowing exception if
   * resource unavailable (which is a very bad situation).
   */
  public final static InputStream getDefaultsResource()
    throws FileNotFoundException {
    // try to access as resource
    URL defaultsURL = Settings.class.getResource("default-settings.properties");
    InputStream is = null;
    if (defaultsURL != null) {
      try { is = defaultsURL.openStream(); }
      catch (IOException ioe) { is = null; }
    }
    if (is == null) {
      System.err.println(className + ": error: couldn't " +
			 "find the \"default-settings.properties\" resource");
      throw new FileNotFoundException("no default settings");
    }
    System.err.println("Reading default settings from resource\n\t" +
		       defaultsURL);
    return is;
  }

  /**
   * Gets a new <code>InputStream</code> for the file
   * <tt>$HOME/.db-parser/settings</tt> if it exists, returning
   * <code>null</code> otherwise.
   */
  private final static InputStream getSettingsStream()
    throws FileNotFoundException {
    File file = new File(Settings.settingsDir + File.separator + "settings");
    if (file.exists()) {
      System.err.println("Reading settings from\n\t" + file);
      return new FileInputStream(file);
    }
    else
      return null;
  }
}
