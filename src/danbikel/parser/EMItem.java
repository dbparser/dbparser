package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

public class EMItem extends CKYItem.MappedPrevModBaseNPAware {
  protected static class AntecedentPair {
    EMItem first;
    EMItem second;
    // for each pair of antecedents, there is one or more events that generated
    // this consequent, stored in this singly-linked list
    SLNode events;

    AntecedentPair(EMItem first, EMItem second, TrainerEvent event) {
      this(first, second, new SLNode(event, null));
    }
    AntecedentPair(EMItem first, EMItem second, SLNode events) {
      this.first = first;
      this.second = second;
      this.events = events;
    }

    EMItem first() { return first; }
    EMItem second() { return second; }
    SLNode events() { return events; }
  }

  // additional data members
  /** A list of antecedent pairs. */
  protected SLNode antecendentPairs;
}
