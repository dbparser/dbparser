package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

public class AnalyzeDisns {

  /**
   * Returns the entropy of the specified distribution.
   */
  public static double entropy(double[] disn) {
    double entropy = 0.0;
    for (int i = disn.length - 1; i >= 0; i--) {
      entropy -= disn[i] * (Math.log(disn[i])/Math.log(2));
    }
    return entropy;
  }

  public static void analyzeModWordDisn(ModelCollection mc, String eventStr)
    throws IOException {

    System.err.println("analyzing dis'n for " + eventStr);

    Model mwm = mc.modWordModel();
    Set allWords = new HashSet();
    allWords.addAll(mc.vocabCounter().keySet());
    allWords.addAll(mc.wordFeatureCounter().keySet());

    System.err.println("num words (incl. word feature vectors): " +
		       allWords.size());

    ProbabilityStructure structure = mwm.getProbStructure();
    int numLevels = structure.numLevels();
    TrainerEvent event = new ModifierEvent(Sexp.read(eventStr));
    Word modWord = event.modHeadWord();

    double total = 0.0;
    double[] tmpProbs = new double[allWords.size()];
    /*
    double[][] tmpLevelProbs = new double[numLevels][];
    for (int i = 0; i < numLevels; i++) {
      tmpLevelProbs[i] = new double[allWords.size()];
    }
    */
    int probIdx = 0;
    Iterator it = allWords.iterator();
    while (it.hasNext()) {
      modWord.setWord((Symbol)it.next());
      double prob = mwm.estimateProb(structure, event);
      int levelForProb = numLevels;
      for (int i = 0; i < numLevels; i++){
	if (structure.estimates[i] > 0.0) {
	  levelForProb = i;
	  break;
	}
      }
      if (prob > 0.0) {
	Symbol word = event.modHeadWord().word();
	// wordFreq should *really* be the count(w,t) observed in training,
	// *NOT* the simple c(w)
	double wordFreq = mc.vocabCounter().count(word);
	System.out.println(wordFreq + "\t" + prob + "\t" + levelForProb +
			   "\t" + word);
	tmpProbs[probIdx++] = prob;
	total += prob;
      }
    }

    double probs[] = new double[probIdx];
    System.arraycopy(tmpProbs, 0, probs, 0, probIdx);

    System.err.println("total prob. mass is " + total);
      
    System.err.println("entropy is " + entropy(probs) + " bits");

    /*
      Arrays.sort(probs);
      for (int i = probs.length - 1; i >= 0; i--)
      System.out.println(probs[i]);
    */
  }

  public static void outputHistories(Model model) {
    Iterator histIt = model.counts[0].history().entrySet().iterator();
    while (histIt.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)histIt.next();
      System.out.println(entry.getDoubleValue(CountsTrio.hist) + "\t" +
			 entry.getKey());
    }
  }

  public static void main(String[] args) {
    String mcFilename = args[0];
    try {
      ModelCollection mc = Trainer.loadModelCollection(mcFilename);
      Model mwm = mc.modWordModel();

      // output all histories
      //outputHistories(mwm);
      String modEventStr = args.length > 1 ? args[1] : 
	"((foo VB) (to TO) VP-A (+START+) " +
	"((+START+ +START+)) VP TO (VP-A) false right)";
      
      analyzeModWordDisn(mc, modEventStr);
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}
