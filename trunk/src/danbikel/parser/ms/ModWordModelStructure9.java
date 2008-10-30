package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

public class ModWordModelStructure9 extends ProbabilityStructure {

  public ModWordModelStructure9() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent hist = histories[backOffLevel];
    hist.clear();

    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      break;
    case 1:
      // for p(w_i | t_i)
      hist.add(modEvent.modHeadWord().tag());
      break;
    }

    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    Word modHead = ((ModifierEvent)trainerEvent).modHeadWord();
    Symbol word =
      modHead.features() != null ? modHead.features() : modHead.word();
    future.add(word);
    return future;
  }

  public ProbabilityStructure copy() {
    ProbabilityStructure psCopy = new ModWordModelStructure9();
    psCopy.setAdditionalData(this.additionalData);
    return psCopy;
  }
}
