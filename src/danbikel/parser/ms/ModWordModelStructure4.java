package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

public class ModWordModelStructure4 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  private Word startWord = Language.training().startWord();
  private Symbol baseNP = Language.treebank().baseNPLabel();
  private Symbol topSym = Language.training().topSym();

  public ModWordModelStructure4() {
    super();
  }

  public int maxEventComponents() { return 10; }
  public int numLevels() { return 5; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;

    if (modEvent.parent() == baseNP)
      return getBaseNPHistory(modEvent, backOffLevel);

    Symbol side = Constants.sideToSym(modEvent.side());

    MutableEvent hist = historiesWithSubcats[backOffLevel];
    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    Symbol mappedPrevModSym =
      Collins.mapPrevMod(modEvent.previousMods().symbolAt(0));
    Symbol parent =
      Language.training().removeArgAugmentation(modEvent.parent());

    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, map(M_i-1),  subcat,
      //             side)
      hist.add(0, Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, w, t, side)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(0, Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, side);
      break;
    case 2:
      // for p(w_i | P, w, side)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(0, parent);
      hist.add(0, modEvent.headWord().word());
      hist.add(0, side);
      break;
    case 3:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, map(M_i-1), subcat, side)
      hist.add(0, Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training().removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 4:
      // for p(w_i | t_i)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(modEvent.modHeadWord().tag());
      break;
    }
    return hist;
  }

  private Event getBaseNPHistory(ModifierEvent modEvent, int backOffLevel) {
    MutableEvent hist = histories[backOffLevel];

    Symbol side = Constants.sideToSym(modEvent.side());

    Symbol prevModLabel =
      (modEvent.previousMods().get(0) == startSym ?
       modEvent.head() : modEvent.previousMods().symbolAt(0));
    Word prevModWord =
      (modEvent.previousWords().getWord(0).equals(startWord) ?
       modEvent.headWord() : modEvent.previousWords().getWord(0));
    hist.clear();
    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, M(w,t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      /*
    case 1:
      // for p(w_i | M(t)_i, P, M(t)_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      */
    case 1:
      // for p(w_i | M(t)_i, P, M_i-1, side)
      hist.add(Language.training().removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training().removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(side);
      break;
    case 2:
    case 3:
    case 4:
      // for p(w_i | t_i)
      hist.add(modEvent.modHeadWord().tag());
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    Word modHead = ((ModifierEvent)trainerEvent).modHeadWord();
    Symbol word =
      modHead.features() != null ? modHead.features() : modHead.word();
    future.add(word);
    return future;
  }

  public boolean doCleanup() { return true; }

  /**
   * In order to gather statistics for words that appear as the head of
   * the entire sentence when estimating p(w | t), the trainer "fakes" a
   * modifier event, as though the root node of the observed tree was seen
   * to modify the magical +TOP+ node.  For back-off levels 0 and 1, we
   * will never use the derived counts whose history contexts contain +TOP+.
   * This method allows for the removal of these "unnecessary" counts,
   * which will never be used when decoding.
   */
  public boolean removeHistory(int backOffLevel, Event history) {
    // this method assumes the parent component of histories for
    // back-off levels 0, 1 and 3 will be at index 2, and at index 0 for
    // back-off level 2.  IF THIS CHANGES, this method will need to change
    // accordingly.
    switch (backOffLevel) {
    case 0:
      return history.get(0, 2) == topSym;
    case 1:
      return history.get(0, 2) == topSym;
    case 2:
      return history.get(0, 0) == topSym;
    case 3:
      return history.numComponents() >= 3 && history.get(0, 2) == topSym;
    case 4:
      return false;
    }
    return false;
  }

  public ProbabilityStructure copy() {
    ProbabilityStructure psCopy = new ModWordModelStructure4();
    psCopy.setAdditionalData(this.additionalData);
    return psCopy;
  }
}
