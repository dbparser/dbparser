package danbikel.parser;

public class NonterminalPriorModelStructure1 extends ProbabilityStructure {
  public NonterminalPriorModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    PriorEvent priorEvent = (PriorEvent)trainerEvent;

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(label | w,t)
      history.add(priorEvent.headWord().word());
      history.add(priorEvent.headWord().tag());
      break;
    case 1:
      // for p(label | t)
      history.add(priorEvent.headWord().tag());
      break;
    }
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((PriorEvent)trainerEvent).label());
    return future;
  }

  public ProbabilityStructure copy() {
    return new NonterminalPriorModelStructure1();
  }
}
