package danbikel.parser;

public class NonterminalPriorModelStructure1 extends ProbabilityStructure {
  public NonterminalPriorModelStructure1() {
    super();
  }

  public int maxEventComponents() { return 3; }
  public int numLevels() { return 2; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;

    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(label | w,t)
      history.add(headEvent.headWord().word());
      history.add(headEvent.headWord().tag());
      break;
    case 1:
      // for p(label | t)
      history.add(headEvent.headWord().tag());
      break;
    }
    return history;
  }
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    future.clear();
    future.add(((HeadEvent)trainerEvent).head());
    return future;
  }

  public ProbabilityStructure copy() {
    return new NonterminalPriorModelStructure1();
  }
}
