package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;

public class EMItem extends CKYItem.MappedPrevModBaseNPAware {
  public static class AntecedentPair implements Serializable {
    EMItem first;
    EMItem second;
    // for each pair of antecedents, there is one or more events that generated
    // this consequent; the events and their associated probabilities
    // are stored in the following co-indexed arrays
    transient TrainerEvent[] events;
    double[] probs;

    // the next pair in this singly-linked list
    AntecedentPair next;

    AntecedentPair(EMItem first, EMItem second,
		   TrainerEvent event, double prob, AntecedentPair next) {
      this(first, second, new TrainerEvent[]{event}, new double[]{prob}, next);
    }
    AntecedentPair(EMItem first, EMItem second,
		   TrainerEvent[] events, double[] probs,
		   AntecedentPair next) {
      this.first = first;
      this.second = second;
      this.events = events;
      this.probs = probs;
      this.next = next;
    }

    EMItem first() { return first; }
    EMItem second() { return second; }
    AntecedentPair next() { return next; }
    TrainerEvent[] events() { return events; }
    double[] probs() { return probs; }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      for (AntecedentPair curr = this; curr != null; curr = curr.next) {
        buf.append("(@");
        buf.append(System.identityHashCode(curr.first));
        if (curr.second != null) {
          buf.append(",@");
          buf.append(System.identityHashCode(curr.second));
        }
        buf.append(")");
        if (curr.next != null)
          buf.append(", ");
      }
      buf.append("]");
      return buf.toString();
    }
  }

  // additional data members
  /** A list of antecedent pairs. */
  protected AntecedentPair antecedentPairs;
  protected int unaryLevel;


  /**
   * This method simply throws an UnsupportedOperationException,
   * as the log probabilities of the superclass are not used by this class.
   */
  public void set(Symbol label,
                  Word headWord,
                  Subcat leftSubcat,
                  Subcat rightSubcat,
                  CKYItem headChild,
                  SLNode leftChildren,
                  SLNode rightChildren,
                  SexpList leftPrevMods,
                  SexpList rightPrevMods,
                  int start,
                  int end,
                  boolean leftVerb,
                  boolean rightVerb,
                  boolean stop,
                  double logTreeProb,
                  double logPrior,
                  double logProb) {
    throw new UnsupportedOperationException();
  }

  public void set(Symbol label,
                  Word headWord,
                  Subcat leftSubcat,
                  Subcat rightSubcat,
                  CKYItem headChild,
                  SLNode leftChildren,
                  SLNode rightChildren,
                  SexpList leftPrevMods,
                  SexpList rightPrevMods,
                  int start,
                  int end,
                  boolean leftVerb,
                  boolean rightVerb,
                  boolean stop,
                  int unaryLevel,
                  double insideProb) {
    super.set(label, headWord, leftSubcat, rightSubcat, headChild,
              leftChildren, rightChildren, leftPrevMods, rightPrevMods,
              start, end, leftVerb, rightVerb, stop, 0.0, 0.0, 0.0);
    setInsideProb(insideProb);
    setOutsideProb(0.0);
    antecedentPairs = null;
    this.unaryLevel = unaryLevel;
  }

  public CKYItem setDataFrom(CKYItem other) {
    super.setDataFrom(other);
    antecedentPairs = null;
    unaryLevel = ((EMItem)other).unaryLevel;
    setOutsideProb(0.0);
    return this;
  }

  // We employ the logProb, logPrior and logTreeProb data members for different
  // purposes in this class.  We accomplish this by overriding their
  // accessor/mutator methods so that they each throw an
  // UnsupportedOperationException and by adding new methods with appropriate
  // names that use the old data members for storage.
  public double logProb() {
    throw new UnsupportedOperationException();
  }
  public void setLogProb(double logProb) {
    throw new UnsupportedOperationException();
  }
  public double logPrior() {
    throw new UnsupportedOperationException();
  }
  public void setLogPrior(double logPrior) {
    throw new UnsupportedOperationException();
  }
  public double logTreeProb() {
    throw new UnsupportedOperationException();
  }
  public void setLogTreeProb(double logTreeProb) {
    throw new UnsupportedOperationException();
  }

  /*
  public double prob() { return logProb; }
  public void setProb(double prob) { logProb = prob; }
  */
  public double outsideProb() { return logPrior; }
  public void setOutsideProb(double outsideProb) {
    logPrior = outsideProb;
  }
  public void increaseOutsideProb(double amount) {
    logPrior += amount;
  }

  public double insideProb() { return logTreeProb; }
  public void setInsideProb(double insideProb) {
    logTreeProb = insideProb;
  }
  public void increaseInsideProb(double amount) {
    logTreeProb += amount;
  }

  public AntecedentPair antecedentPairs() { return antecedentPairs; }
  public void setAntecedentPairs(AntecedentPair pair) {
    this.antecedentPairs = pair;
  }

  public int unaryLevel() { return unaryLevel; }
  public void setUnaryLevel(int unaryLevel) { this.unaryLevel = unaryLevel; }

  public boolean equals(Object obj) {
    return super.equals(obj) && unaryLevel == ((EMItem)obj).unaryLevel();
  }
  public String toString() {
    return toSexp().toString() + "\t; head=" + headWord +
      "; lc=" + leftSubcat.toSexp() + "; rc=" + rightSubcat.toSexp() +
      "; leftPrev=" + leftPrevMods + "; rightPrev=" + rightPrevMods +
      "; lv=" + shortBool(leftVerb) + "; rv=" + shortBool(rightVerb) +
      "; hasVerb=" + shortContainsVerb(containsVerb) +
      "; stop=" + shortBool(stop) +
      "; inside=" + insideProb() +
      "; outside=" + outsideProb() +
      "; antecedentPairs=" + String.valueOf(antecedentPairs) +
      "; unaryLevel=" + unaryLevel +
      " (@" + System.identityHashCode(this) + ")";
  }
}
