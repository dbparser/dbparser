package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

public class BrokenLexPriorModelStructure extends ProbabilityStructure {
  public BrokenLexPriorModelStructure() {
    super();
  }

  public int maxEventComponents() { return 2; }
  public int numLevels() { return 1; }

  /*
  public double lambdaFudge(int backOffLevel) { return 0.0; }
  public double lambdaFudgeTerm(int backOffLevel) { return 1.0; }
  */

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
    Word headWord = priorEvent.headWord();
    Symbol word =
      headWord.features() != null ? headWord.features() : headWord.word();
    future.add(word);
    future.add(headWord.tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new BrokenLexPriorModelStructure();
  }
}
