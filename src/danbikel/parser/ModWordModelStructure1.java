package danbikel.parser;

import danbikel.lisp.*;

public class ModWordModelStructure1 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();

  public ModWordModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 9; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent hist = historiesWithSubcats[backOffLevel];
    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, (M_i-1,...,M_i-k),
      //             subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.previousMods()));
      hist.add(1, modEvent.subcat());
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, M_i-1 == +START+, subcat)
      Symbol prevModIsStartSym =
	Constants.booleanToSym(modEvent.previousMods().get(0) == startSym);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, prevModIsStartSym);
      hist.add(1, modEvent.subcat());
      break;
    case 2:
      // for p(w_i | t_i)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(modEvent.modHeadWord().tag());
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((ModifierEvent)trainerEvent).modHeadWord().word());
    return future;
  }

  public ProbabilityStructure copy() {
    return new ModWordModelStructure1();
  }
}
