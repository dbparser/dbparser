package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the generation model
 * for modifying nonterminals/part-of-speech tags (the modifying nonterminals
 * are partially lexicalized with the parts of speech of their respective
 * head words).
 * <p>
 * <b>It is a horrendous bug that all of these <code>ProbabilityStructure</code>
 * classes do not copy various lists from the <code>TrainerEvent</code> objects
 * before removing gap augmentations from their elements.</b>
 * <p>
 */
public class ModNonterminalModelStructure2 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  public ModNonterminalModelStructure2() {
    super();
  }

  public int maxEventComponents() { return 7; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;

    MutableEvent hist = historiesWithSubcats[backOffLevel];

    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    Symbol prevModIsStartSym =
      Constants.booleanToSym(modEvent.previousMods().get(0) == startSym);
    switch (backOffLevel) {
    case 0:
      // for p(M(t)_i | P, H, w, t, verbIntervening, M_i-1 == +START+, subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, prevModIsStartSym);
      hist.add(1, modEvent.subcat());
      break;
    case 1:
      // for p(M(t)_i | P, H, t, verbIntervening, M_i-1 == +START+, subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, prevModIsStartSym);
      hist.add(1, modEvent.subcat());
      break;
    case 2:
      // for p(M(t)_i | P, H, verbIntervening, M_i-1 == +START+, subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, verbInterveningSym);
      hist.add(0, prevModIsStartSym);
      hist.add(1, modEvent.subcat());
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(modEvent.modifier());
    future.add(modEvent.modHeadWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new ModNonterminalModelStructure1();
  }
}
