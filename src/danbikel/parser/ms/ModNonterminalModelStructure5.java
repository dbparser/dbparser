package danbikel.parser.ms;

import danbikel.parser.*;
/**
 * This class provides a probability structure identical to its superclass,
 * except that {@link #getFuture(TrainerEvent,int)} has been overridden so
 * that the future only consists of an unlexicalized modifying nonterminal
 * label.  Furthermore, this probability structure overrides the
 * {@link #jointModel} method to return an array containing a
 * {@link TagModelStructure1} instance.
 *
 * @see #jointModel
 * @see TagModelStructure1
 */
public class ModNonterminalModelStructure5
  extends ModNonterminalModelStructure4 {

  private ProbabilityStructure[] jointStructures = {new TagModelStructure1()};

  public ModNonterminalModelStructure5() {
    super();
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(modEvent.modifier());
    return future;
  }

  public Model newModel() { return new JointModel(this); }

  public ProbabilityStructure[] jointModel() {
    return jointStructures;
  }

  public ProbabilityStructure copy() {
    return new ModNonterminalModelStructure5();
  }
}