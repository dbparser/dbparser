package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * Methods used for the construction of prior states in the Markov
 * process of creating modifier nonterminals.  Currently, prior state
 * is stored in a previous modifier list (a <code>SexpList</code>
 * object) and a previous modifier head-word list (a
 * <code>WordList</code> object), so the methods here are used to
 * "shift" a new modifier to the head of these lists, losing the last
 * element of the list (which was the least-recently-generated
 * previous modifier).  In the decoder, these previous modifier lists
 * are constructed, so there are two &quot;skip&quot; methods that
 * indicate whether to skip over certain previously-generated
 * modifiers in the construction of these lists.
 * <p>
 * <b>Implementation note</b>: In the future, the Markov process of
 * generating modifiers will be implemented in a cleaner fashion, by
 * introducing a special <code>State</code> object, which implementors
 * of this interface will manipulate.  In other words, this interface
 * will serve to specify a <i>transition function</i>, allowing
 * greater flexibility in the experimentation with different notions
 * of history in the Markov process.
 */
public interface Shift {
  public void shift(TrainerEvent event, SexpList list,
		    Sexp prevMod, Sexp currMod);
  public void shift(TrainerEvent event, WordList wordList,
		    Word prevWord, Word currWord);

  public boolean skip(Item item, Sexp prevMod, Sexp currMod);
  public boolean skip(Item item, Word prevWord, Word currWord);
}
