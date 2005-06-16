package danbikel.parser;

import danbikel.lisp.Symbol;

/**
 * Specifies a single method to map a symbol representing a nonterminal to
 * another symbol, typically an equivalence class.
 *
 * @see NTMapper
 * @see Settings#prevModMapperClass
 */
public interface NonterminalMapper {
  /**
   * Maps the specified nonterminal label to some other symbol (typically
   * an equivalence class).
   * @param label the nonterminal label to be mapped
   * @return a mapped version of the specified nonterminal label (typically
   * an equivalence class)
   */
  public Symbol map(Symbol label);
}
