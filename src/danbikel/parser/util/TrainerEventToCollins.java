package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

public class TrainerEventToCollins {
  public static Symbol topSym = Language.training().topSym();
  public static Symbol startSym = Language.training().startSym();
  public static Symbol stopSym = Language.training().stopSym();
  public static Symbol baseNP = Language.treebank().baseNPLabel();
  public static Symbol npArg = Symbol.add("NP-A");
  public static Symbol sbarArg = Symbol.add("SBAR-A");
  public static Symbol sArg = Symbol.add("S-A");
  public static Symbol vpArg = Symbol.add("VP-A");
  public static Symbol miscArg =
    Symbol.add(stopSym.toString() +
	       Language.treebank().canonicalAugDelimiter() +
	       Language.training().argAugmentation());



  public static String subcatToCollins(Subcat subcat, boolean withGap) {
    int nps = 0, sbars = 0, ss = 0, vps = 0, miscs = 0;
    Iterator it = subcat.iterator();
    while (it.hasNext()) {
      Symbol requirement = (Symbol)it.next();
      if (requirement == npArg)
	nps++;
      else if (requirement == sArg)
	ss++;
      else if (requirement == sbarArg)
	sbars++;
      else if (requirement == vpArg)
	vps++;
      else if (requirement == miscArg)
	miscs++;
    }
    StringBuffer sb = new StringBuffer(withGap ? 6 : 5);
    sb.append(nps).append(ss).append(sbars).append(vps).append(miscs);
    if (withGap)
      sb.append("0");
    return sb.toString();
  }

  public static String modEventToCollins(ModifierEvent modEvent) {
    StringBuffer sb = new StringBuffer(80);
    sb.append("2 ");
    Word modHeadWord = modEvent.modHeadWord();
    boolean modIsStop = modHeadWord.tag() == stopSym;
    if (modIsStop) {
      sb.append("#STOP# #STOP# ");
    }
    else {
      sb.append(modHeadWord.word()).append(" ");
      sb.append(modHeadWord.tag()).append(" ");
    }

    Symbol prevMod = modEvent.previousMods().symbolAt(0);
    boolean prevModIsStart = false, prevModIsPunc = false, prevModIsCC = false;
    if (prevMod == startSym)
      prevModIsStart = true;
    else if (Language.treebank().isPunctuation(prevMod))
      prevModIsPunc = true;
    else if (Language.treebank().isConjunction(prevMod))
      prevModIsCC = true;

    boolean parentIsBaseNP = modEvent.parent() == baseNP;

    Word headWord = modEvent.headWord();
    if (parentIsBaseNP && !prevModIsStart)
      headWord = modEvent.previousWords().getWord(0);

    sb.append(headWord.word()).append(" ");
    sb.append(headWord.tag()).append(" ");
    String modifier = modIsStop ? "#STOP#" : modEvent.modifier().toString();
    sb.append(modifier).append(" ");
    sb.append(modEvent.parent()).append(" ");

    Symbol head = modEvent.head();
    if (parentIsBaseNP && !prevModIsStart)
      head = modEvent.previousMods().symbolAt(0);

    sb.append(head).append(" ");
    sb.append(subcatToCollins(modEvent.subcat(), true)).append(" ");

    // append distance triple
    sb.append(modEvent.side() == Constants.LEFT ? "1" : "0");
    // mike doesn't consider CC's or punctuation to be words when calculating
    // head-adjacency, so we can either approximate it by detecting whether
    // previous mod is either start or punc or CC, or just know that our
    // head-adjacency will always be different
    /*
    boolean adjacent = parentIsBaseNP ||
                       prevModIsStart || prevModIsPunc || prevModIsCC;
    */
    boolean adjacent = parentIsBaseNP || prevModIsStart;
    sb.append(adjacent ? "1" : "0");
    boolean verbIntervening =
      parentIsBaseNP ? false : modEvent.verbIntervening();
    sb.append(verbIntervening ? "1" : "0");

    // don't even try to spit out coordination and punctuation information

    return sb.toString();
  }

  public static String headEventToCollins(HeadEvent headEvent) {
    StringBuffer sb = new StringBuffer(80);
    sb.append("3 ");
    Word headWord = headEvent.headWord();
    sb.append(headWord.word()).append(" ").append(headWord.tag()).append(" ");
    Symbol parent = headEvent.parent();
    sb.append(parent == topSym ? "TOP" :
              headEvent.parent().toString()).append(" ");
    sb.append(headEvent.head()).append(" ");
    sb.append(subcatToCollins(headEvent.leftSubcat(), false)).append(" ");
    sb.append(subcatToCollins(headEvent.rightSubcat(), false));
    return sb.toString();
  }

  public static String trainerEventToCollins(TrainerEvent event) {
    String collinsStr = null;
    if (event instanceof HeadEvent)
      collinsStr = headEventToCollins((HeadEvent)event);
    else if (event instanceof ModifierEvent)
      collinsStr = modEventToCollins((ModifierEvent)event);
    return collinsStr;
  }
}
