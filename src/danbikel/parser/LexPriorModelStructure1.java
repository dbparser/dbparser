package danbikel.parser;

public class LexPriorModelStructure1 extends ProbabilityStructure {
  public LexPriorModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 1; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    PriorEvent priorEvent = (PriorEvent)trainerEvent;
    MutableEvent history = histories[backOffLevel];
    history.clear();
    history.add(priorEvent.history());
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    PriorEvent priorEvent = (PriorEvent)trainerEvent;
    future.add(priorEvent.headWord().word());
    future.add(priorEvent.headWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new LexPriorModelStructure1();
  }
}
