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

  public double lambdaFudge(int backOffLevel) { return 0.0; }
  public double lambdaFudgeTerm(int backOffLevel) { return 5.0; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;

    Sexp head =
      Language.training.removeGapAugmentation(headEvent.head());
    head = Language.training.removeArgAugmentation(head.symbol());
    Sexp parent =
      Language.training.removeGapAugmentation(headEvent.parent());
    parent = Language.training.removeArgAugmentation(parent.symbol());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(Subcat | H, P, w, t)
      history.add(head);
      history.add(parent);
      history.add(headEvent.headWord().word());
      history.add(headEvent.headWord().tag());
      break;
    case 1:
      // for p(Subcat | H, P, t)
      history.add(head);
      history.add(parent);
      history.add(headEvent.headWord().tag());
      break;
    case 2:
      // for p(Subcat | H, P)
      history.add(head);
      history.add(parent);
      break;
    }
    return history;
  }
}
