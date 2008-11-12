package danbikel.parser;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * Default implementation of the {@link Runtime} interface, providing all
 * necessary instances for this parsing engine.
 */
public class RuntimeImpl implements Runtime {
  // constants
  private final static String className = RuntimeImpl.class.getName();

  // data members
  private Settings settings;
  private Language language;
  private WordFactory words;
  private SubcatFactory subcats;
  private NonterminalMapper ntMapper;
  private Shift shifter;

  // constructors
  public RuntimeImpl() {
    settings = new Settings();
    init();
  }

  public RuntimeImpl(String settingsFile) throws IOException {
    this(new File(settingsFile));
  }

  public RuntimeImpl(File settingsFile) throws IOException {
    settings = new Settings(settingsFile);
    init();
  }

  public RuntimeImpl(InputStream is) throws IOException {
    settings = new Settings(is);
    init();
  }

  // helper method for constructors
  private void init() {
    language = new danbikel.parser.Language(settings);
    words = getWordFactory();
    subcats = getSubcatFactory();
    ntMapper = getNTMapper();
    shifter = getShifter();
  }

  // four helper methods for init method
  private WordFactory getWordFactory() {
    WordFactory factory;
    String wordFactStr = settings.get(Settings.wordFactoryClass);
    if (wordFactStr != null) {
      try {
	factory = (WordFactory) Class.forName(wordFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + wordFactStr + ":\n\t" + e +
			   "\n\tusing DefaultWordFactory instead");
	factory = new DefaultWordFactory();
      }
    } else {
      System.err.println(className + ": error: the property " +
			 Settings.wordFactoryClass + " was not set;\n\t" +
			 "using DefaultWordFactory");
      factory = new DefaultWordFactory();
    }
    return factory;
  }

  private SubcatFactory getSubcatFactory() {
    SubcatFactory factory;
    String subcatFactStr = settings.get(Settings.subcatFactoryClass);
    if (subcatFactStr != null) {
      try {
	factory = (SubcatFactory) Class.forName(subcatFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + subcatFactStr + ":\n\t" + e +
			   "\n\tusing SubcatBagFactory instead");
	factory = new SubcatBagFactory();
      }
    } else {
      System.err.println(className + ": error: the property " +
			 Settings.subcatFactoryClass + " was not set;\n\t" +
			 "using SubcatBagFactory");
      factory = new SubcatBagFactory();
    }
    return factory;
  }

  private NonterminalMapper getNTMapper() {
    String fallbackDefaultClassname = IdentityNTMapper.class.getName();
    NonterminalMapper mapper;
    String mapperClassStr = settings.get(Settings.prevModMapperClass);
    if (mapperClassStr != null) {
      try {
	mapper =
	  (NonterminalMapper) Class.forName(mapperClassStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + mapperClassStr + ":\n\t" + e +
			   "\n\tusing " + fallbackDefaultClassname +
			   " instead");
	mapper = new IdentityNTMapper();
      }
    } else {
      System.err.println(className + ": error: the property " +
			 Settings.prevModMapperClass + " was not set;\n\t" +
			 "using " + fallbackDefaultClassname);
      mapper = new IdentityNTMapper();
    }
    return mapper;
  }

  private Shift getShifter() {
    Shift shifter;
    String shifterStr = settings.get(Settings.shifterClass);
    if (shifterStr != null) {
      try {
	shifter = (Shift) Class.forName(shifterStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + shifterStr + ":\n\t" + e +
			   "\n\tusing DefaultShifter instead");
	shifter = new DefaultShifter();
      }
    } else {
      System.err.println(className + ": error: the property " +
			 Settings.shifterClass + " was not set;\n\t" +
			 "using DefaultShifter");
      shifter = new DefaultShifter();
    }
    return shifter;
  }

  public Settings settings() {
    return settings;
  }

  public Language language() {
    return language;
  }

  public WordFactory words() {
    return words;
  }

  public SubcatFactory subcats() {
    return subcats;
  }

  public NonterminalMapper ntMapper() {
    return ntMapper;
  }

  public Shift shifter() {
    return shifter;
  }
}
