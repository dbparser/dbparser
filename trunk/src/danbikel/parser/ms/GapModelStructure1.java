package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for either side of the head child.
 */
public class GapModelStructure1 extends ProbabilityStructure {
  public GapModelStructure1() { super(); }

  public int maxEventComponents() { return 4; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    GapEvent gapEvent = (GapEvent)trainerEvent;

    Sexp noGapHead =
      Language.training().removeGapAugmentation(gapEvent.head());
    Sexp noGapParent =
      Language.training().removeGapAugmentation(gapEvent.parent());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(Gap | H, P, w, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(gapEvent.headWord().word());
      history.add(gapEvent.headWord().tag());
      break;
    case 1:
      // for p(Gap | H, P, t)
      history.add(noGapHead);
      history.add(noGapParent);
      history.add(gapEvent.headWord().tag());
      break;
    case 2:
      // for p(Gap | H, P)
      history.add(noGapHead);
      history.add(noGapParent);
      break;
    }
    return history;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((GapEvent)trainerEvent).direction());
    return future;
  }

  public ProbabilityStructure copy() {
    return new GapModelStructure1();
  }
}
