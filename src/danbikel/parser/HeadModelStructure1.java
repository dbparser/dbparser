package danbikel.parser;

import danbikel.lisp.*;


public class HeadModelStructure1 extends ProbabilityStructure {
  public HeadModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;

    Sexp noGapParent =
      Language.training.removeGapAugmentation(headEvent.parent());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(H | P, w, t)
      history.add(noGapParent);
      history.add(headEvent.headWord().word());
      history.add(headEvent.headWord().tag());
      break;
    case 1:
      // for p(H | P, t)
      history.add(noGapParent);
      history.add(headEvent.headWord().tag());
      break;
    case 2:
      // for p(H | P)
      history.add(noGapParent);
    }
    return history;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((HeadEvent)trainerEvent).head());
    return future;
  }

  public ProbabilityStructure copy() {
    return new HeadModelStructure1();
  }
}
