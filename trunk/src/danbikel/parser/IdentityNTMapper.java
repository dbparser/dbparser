package danbikel.parser;

import danbikel.lisp.Symbol;

/**
 * Provides the identity mapping function.
 */
public class IdentityNTMapper implements NonterminalMapper {
  /**
   *  Constructs a new instance of this identity mapper.
   */
  IdentityNTMapper() {}
  
  /**
   * Returns the specified label unchanged (the identity mapping function).
   * @param label the label to be mapped
   * @return the specified label unchanged (the identity mapping function).
   */
  public Symbol map(Symbol label) {
    return label;
  }
}
