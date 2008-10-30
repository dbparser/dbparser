package danbikel.parser;

import danbikel.lisp.*;

import java.util.Map;

/**
 * A class that provides a static method for mapping nonterminals, {@link
 * #map(Symbol)}.  This class uses an internal {@link NonterminalMapper}
 * instance, the actual type of which is determined by the value of {@link
 * Settings#prevModMapperClass}.  If {@link Settings#prevModMapperClass} was not
 * set (which should never happen, since that setting is part of the default
 * settings provided with this package), then an instance of {@link
 * IdentityNTMapper} is used.
 */
public class NTMapper {
  // static class, so no instances may be constructed
  private NTMapper() {}

  private static final String className = NTMapper.class.getName();
  private static final String fallbackDefaultClassname =
    IdentityNTMapper.class.getName();

  private static NonterminalMapper mapper = getMapper();

  private static NonterminalMapper getMapper() {
    NonterminalMapper mapper;
    String mapperClassStr = Settings.get(Settings.prevModMapperClass);
    if (mapperClassStr != null) {
      try {
	mapper = (NonterminalMapper)Class.forName(mapperClassStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + mapperClassStr + ":\n\t" + e +
			   "\n\tusing " + fallbackDefaultClassname + " instead");
	mapper = new IdentityNTMapper();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.prevModMapperClass + " was not set;\n\t" +
			 "using " + fallbackDefaultClassname);
      mapper = new IdentityNTMapper();
    }
    return mapper;
  }

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.prevModMapperClass)) {
	  mapper = getMapper();
	}
      }
    };
    Settings.register(NTMapper.class, change, null);
  }

  /**
   * Maps the specified nonterminal to another symbol, via an internal instance
   * of {@link NonterminalMapper} whose concrete type is determined by
   * the value of the {@link Settings#prevModMapperClass} setting.
   *
   * @param nonterminal the nonterminal to be mapped
   * @return a mapping of the specified nonterminal
   */
  public static Symbol map(Symbol nonterminal) {
    return mapper.map(nonterminal);
  }
}
