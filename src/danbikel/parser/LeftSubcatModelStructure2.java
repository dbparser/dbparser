package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the left side of the head child.
 */
public class LeftSubcatModelStructure2 extends SubcatModelStructure2 {
  public LeftSubcatModelStructure2() {
    super();
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).leftSubcat();
  }

  public ProbabilityStructure copy() {
    return new LeftSubcatModelStructure2();
  }
}
