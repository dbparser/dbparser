package danbikel.parser;

public class LexPriorModelStructure1 extends ProbabilityStructure {
  public LexPriorModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 1; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    history.clear();
    history.add(headEvent.parent());
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    future.clear();
    HeadEvent headEvent = (HeadEvent)trainerEvent;
    future.add(headEvent.headWord().word());
    future.add(headEvent.headWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new LexPriorModelStructure1();
  }
}
