package danbikel.parser;

public class TopLexModelStructure1 extends ProbabilityStructure {
  public TopLexModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(w | t, H, +TOP+)
      history.add(headEvent.headWord().tag());
      history.add(headEvent.head());
      history.add(headEvent.parent());
      break;
    case 1:
      // for p(w | t, H)
      history.add(headEvent.headWord().tag());
      history.add(Language.treebank.getCanonical(headEvent.head()));
      break;
    }
    return history;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    // for p(w | ...)
    future.add(headEvent.headWord().word());
    return future;
  }

  public ProbabilityStructure copy() {
    return new TopLexModelStructure1();
  }
}
