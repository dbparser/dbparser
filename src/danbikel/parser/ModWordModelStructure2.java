package danbikel.parser;

import danbikel.lisp.*;

public class ModWordModelStructure2 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  private Word startWord = Language.training().startWord();
  private Symbol baseNP = Language.treebank().baseNPLabel();

  public ModWordModelStructure2() {
    super();
  }

  public int maxEventComponents() { return 10; }
  public int numLevels() { return 3; }

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
      Language.training.removeArgAugmentation(modEvent.parent());

    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, map(M_i-1),  subcat,
      //             side)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, map(M_i-1), subcat, side)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, parent);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 2:
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
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      /*
    case 1:
      // for p(w_i | M(t)_i, P, M(t)_i-1, side)
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
      */
    case 1:
      // for p(w_i | M(t)_i, P, M_i-1, side)
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(side);
      break;
    case 2:
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
   * The trainer passes <code>ModifierEvent</code> objects
   * representing modifiers on both sides of the head, even though
   * there are separate models for each side.  This allows the final
   * back-off level, p(w | t), to use information gathered from both
   * sides.  However, it means that after all counts have been
   * derived, there are twice as many entries in the counts tables for
   * the back-off levels 0 and 1, since the model for, say, the left
   * side will have stored counts for the right side as well (the
   * counts are stored separately by explicitly including a "side"
   * field in the events being counted).  This method allows for the
   * removal of these "unnecessary" counts, which will never be used
   * when decoding.
   */
  public boolean removeHistory(int backOffLevel, Event history) {
    // this method assumes the "side" field will be the last element
    // of the events in which it is included (at levels 0 and 1)
    // N.B.: Right now, both levels 0 and 1 of both the regular and the
    // baseNP models have a side field as their final element; if this
    // changes, however, it will be necessary to check the parent
    // field to see if the specified history is that for a baseNP
    // and do a special baseNP case
    Symbol side = (Symbol)additionalData;
    switch (backOffLevel) {
    case 0:
      return history.get(0, history.numComponents(0) - 1) != side;
    case 1:
      return history.get(0, history.numComponents(0) - 1) != side;
    case 2:
      return false;
    }
    return false;
  }

  public ProbabilityStructure copy() {
    ProbabilityStructure psCopy = new ModWordModelStructure2();
    psCopy.setAdditionalData(this.additionalData);
    return psCopy;
  }
}
