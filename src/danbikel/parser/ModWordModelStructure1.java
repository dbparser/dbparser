package danbikel.parser;

import danbikel.lisp.*;

public class ModWordModelStructure1 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();

  public ModWordModelStructure1() { super(); }

  public int maxEventComponents() { return 9; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent hist = historyWithSubcat;
    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, (M_i-1,...,M_i-k),
      //             subcat)
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(modEvent.headWord().word());
      hist.add(modEvent.headWord().tag());
      hist.add(verbInterveningSym);
      hist.add(Language.training.removeGapAugmentation(modEvent.previousMods()));
      hist.add(modEvent.subcat());
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, M_i-1 == +START+, subcat)
      Symbol prevModIsStartSym =
	Constants.booleanToSym(modEvent.previousMods().list().get(0) ==
			       startSym);
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(modEvent.headWord().tag());
      hist.add(verbInterveningSym);
      hist.add(prevModIsStartSym);
      hist.add(modEvent.subcat());
      break;
    case 2:
      // for p(w_i | t_i)
      history.clear();
      history.add(modEvent.modHeadWord().tag());
      hist = history;
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    future.clear();
    future.add(((ModifierEvent)trainerEvent).modHeadWord().word());
    return future;
  }

  public ProbabilityStructure copy() {
    return new ModWordModelStructure1();
  }
}
