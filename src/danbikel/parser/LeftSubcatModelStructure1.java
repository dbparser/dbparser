package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the left side of the head child.
 */
public class LeftSubcatModelStructure1 extends SubcatModelStructure1 {
  public LeftSubcatModelStructure1() {
    super();
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).leftSubcat();
  }

  public ProbabilityStructure copy() {
    return new LeftSubcatModelStructure1();
  }
}
