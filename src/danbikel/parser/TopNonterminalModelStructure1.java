package danbikel.parser;

public class TopNonterminalModelStructure1 extends ProbabilityStructure {
  public TopNonterminalModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 1; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    if (backOffLevel != 0)
      throw new IllegalArgumentException();
    MutableEvent history = histories[backOffLevel];
    history.clear();
    // for p(H,t | +TOP+)
    history.add(trainerEvent.parent());
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    // for p(H,t | ...)
    future.add(((HeadEvent)trainerEvent).head());
    future.add(((HeadEvent)trainerEvent).headWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new TopNonterminalModelStructure1();
  }
}
