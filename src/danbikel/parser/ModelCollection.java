package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * Provides access to all <code>Model</code> objects and maps
 * necessary for parsing.  By bundling all of this information
 * together, all of the objects necessary for parsing can be stored
 * and retrieved simply by serializing and de-serializing this object
 * to a Java object file.
 */
public class ModelCollection implements Serializable {
  // data members
  private Model lexPriorModel;
  private Model nonterminalPriorModel;
  private Model topNonterminalModel;
  private Model topLexModel;
  private Model headModel;
  private Model gapModel;
  private Model leftSubcatModel;
  private Model rightSubcatModel;
  private Model leftModNonterminalModel;
  private Model rightModNonterminalModel;
  private Model leftModWordModel;
  private Model rightModWordModel;
  private CountsTable vocabCounter;
  private CountsTable wordFeatureCounter;
  private CountsTable nonterminals;
  private Map posMap;
  private Map leftSubcatMap;
  private Map rightSubcatMap;

  // derived data
  // maps from integers to nonterminals and nonterminals to integers
  private Map nonterminalMap;
  private Symbol[] nonterminalArr;

  /**
   * Constructs a new <code>ModelCollection</code> that initially contains
   * no data.
   */
  public ModelCollection() {}

  /**
   * Sets all the data members of this object.
   *
   * @param lexPriorModel the model for prior probabilities of
   * lexical elements (for the estimation of the joint event that is a
   * fully lexicalized nonterminal)
   * @param nonterminalPriorModel the model for prior probabilities of
   * nonterminals given the lexical components (for the estimation of the
   * joint event that is a fully lexicalized nonterminal)
   * @param topNonterminalModel the head-generation model for heads whose
   * parents are {@link Training#topSym()}
   * @param topLexModel the head-word generation model for heads of entire
   * sentences
   * @param headModel the head-generation model
   * @param gapModel the gap-generation model
   * @param leftSubcatModel the left subcat-generation model
   * @param rightSubcatmodel the right subcat-generation mode,l
   * @param leftModNonterminalModel the left modifying nonterminal-generation
   * model
   * @param rightModNonterminalModel the right modifying nonterminal-generation
   * model
   * @param leftModWordModel the left modifying word-generation model
   * @param vocabCounter a table of counts of all "known" words of the
   * training data
   * @param wordFeatureCounter a table of counts of all word features ("unknown"
   * words) of the training data
   * @param nonterminals a table of counts of all nonterminals occurring in
   * the training data
   * @param rightModWordModel the right modifying word-generation model
   * @param posMap a mapping from lexical items to all of their possible parts
   * of speech
   * @param leftSubcatMap a mapping from left subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   * @param leftSubcatMap a mapping from right subcat-prediction conditioning
   * contexts (typically parent and head nonterminal labels) to all possible
   * subcat frames
   */
  public void set(Model lexPriorModel,
		  Model nonterminalPriorModel,
		  Model topNonterminalModel,
		  Model topLexModel,
		  Model headModel,
		  Model gapModel,
		  Model leftSubcatModel,
		  Model rightSubcatModel,
		  Model leftModNonterminalModel,
		  Model rightModNonterminalModel,
		  Model leftModWordModel,
		  Model rightModWordModel,
		  CountsTable vocabCounter,
		  CountsTable wordFeatureCounter,
		  CountsTable nonterminals,
		  Map posMap,
		  Map leftSubcatMap,
		  Map rightSubcatMap) {
    this.lexPriorModel = lexPriorModel;
    this.nonterminalPriorModel = nonterminalPriorModel;
    this.topNonterminalModel = topNonterminalModel;
    this.topLexModel = topLexModel;
    this.headModel = headModel;
    this.gapModel = gapModel;
    this.leftSubcatModel = leftSubcatModel;
    this.rightSubcatModel = rightSubcatModel;
    this.leftModNonterminalModel = leftModNonterminalModel;
    this.rightModNonterminalModel = rightModNonterminalModel;
    this.leftModWordModel = leftModWordModel;
    this.rightModWordModel = rightModWordModel;
    this.vocabCounter = vocabCounter;
    this.wordFeatureCounter = wordFeatureCounter;
    this.nonterminals = nonterminals;
    this.posMap = posMap;
    this.leftSubcatMap = leftSubcatMap;
    this.rightSubcatMap = rightSubcatMap;

    createNonterminalMap();
  }

  private void createNonterminalMap() {
    nonterminalMap = new HashMap(nonterminals.size());
    nonterminalArr = new Symbol[nonterminals.size()];
    Iterator nts = nonterminals.keySet().iterator();
    for (int uid = 0; nts.hasNext(); uid++) {
      Symbol nonterminal = (Symbol)nts.next();
      nonterminalArr[uid] = nonterminal;
      nonterminalMap.put(nonterminal, new Integer(uid));
    }
  }

  public int numNonterminals() { return nonterminalArr.length; }
  public Map getNonterminalMap() { return nonterminalMap; }
  public Symbol[] getNonterminalArr() { return nonterminalArr; }

  // accessors
  public Model lexPriorModel() { return lexPriorModel; }
  public Model nonterminalPriorModel() { return nonterminalPriorModel; }
  public Model topNonterminalModel() { return topNonterminalModel; }
  public Model topLexModel() { return topLexModel; }
  public Model headModel() { return headModel; }
  public Model gapModel() { return gapModel; }
  public Model leftSubcatModel() { return leftSubcatModel; }
  public Model rightSubcatModel() { return rightSubcatModel; }
  public Model leftModNonterminalModel() { return leftModNonterminalModel; }
  public Model rightModNonterminalModel() { return rightModNonterminalModel; }
  public Model leftModWordModel() { return leftModWordModel; }
  public Model rightModWordModel() { return rightModWordModel; }
  public CountsTable vocabCounter() { return vocabCounter; }
  public CountsTable wordFeatureCounter() { return wordFeatureCounter; }
  public CountsTable nonterminals() { return nonterminals; }
  public Map posMap() { return posMap; }
  public Map leftSubcatMap() { return leftSubcatMap; }
  public Map rightSubcatMap() { return rightSubcatMap; }
}
