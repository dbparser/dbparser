package danbikel.parser;

import danbikel.lisp.*;

class Collins {
  private static Symbol startSym = Language.training().startSym();
  private static Symbol conjSym = Symbol.add("CC");
  private static Symbol puncSym = Symbol.add(",");
  private static Symbol miscSym = Language.training().stopSym();

  public static Symbol mapPrevMod(Symbol prevMod) {
    if (prevMod == startSym)
      return startSym;
    else if (Language.treebank.isConjunction(prevMod))
      return conjSym;
    else if (Language.treebank.isPunctuation(prevMod))
      return puncSym;
    else
      return miscSym;
  }  
}
