package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for either side of the head child.
 */
abstract public class SubcatModelStructure1 extends ProbabilityStructure {
  protected SubcatModelStructure1() { super(); }

  public int maxEventComponents() { return 4; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;

    Sexp noGapHead =
      Language.training.removeGapAugmentation(headEvent.head());
    Sexp noGapParent =
      Language.training.removeGapAugmentation(headEvent.parent());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(Subcat | H, P, w, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(headEvent.headWord().word());
      history.add(headEvent.headWord().tag());
      break;
    case 1:
      // for p(Subcat | H, P, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(headEvent.headWord().tag());
      break;
    case 2:
      // for p(Subcat | H, P)
      history.add(noGapHead);
      history.add(noGapParent);
      break;
    }
    return history;
  }
}
