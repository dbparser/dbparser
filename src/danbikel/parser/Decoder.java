package  danbikel.parser;

import  danbikel.lisp.*;
import  java.io.Serializable;
import  java.util.*;
import  java.rmi.*;


/**
 * Provides the methods necessary to perform CKY parsing on input sentences.
 */
public class Decoder implements Serializable {
  // data members
  protected DecoderServerRemote server;
  protected CKYChart chart;
  protected Map posMap;
  protected CountsTable nonterminals;
  protected Map leftSubcatMap;
  protected Map rightSubcatMap;
  // these next two data members are also kept in CKYChart, but we keep
  // them here as well, for debugging purposes
  protected int cellLimit = -1;
  protected double pruneFact = 0.0;
  protected SexpList originalSentence = new SexpList();
  protected Subcat emptySubcat = Subcats.get();
  protected boolean downcaseWords =
    Boolean.valueOf(Settings.get(Settings.downcaseWords)).booleanValue();

  /**
   * Constructs a new decoder that will use the specified
   * <code>DecoderServer</code> to get all information and probabilities
   * required for decoding (parsing).
   *
   * @param  server the <code>DecoderServerRemote</code> implementor
   * (either local or remote) that provides this decoder object with
   * information and probabilities required for decoding (parsing)
   */
  public Decoder(DecoderServerRemote server) {
    this.server = server;
    try {
      this.posMap = server.posMap();
      this.nonterminals = server.nonterminals();
      this.leftSubcatMap = server.leftSubcatMap();
      this.rightSubcatMap = server.rightSubcatMap();
    } catch (RemoteException re) {
      System.err.println(re);
    }
    String useCellLimitStr = Settings.get(Settings.decoderUseCellLimit);
    boolean useCellLimit = Boolean.valueOf(useCellLimitStr).booleanValue();
    if (useCellLimit)
      cellLimit = Integer.parseInt(Settings.get(Settings.decoderCellLimit));
    String usePruneFactStr = Settings.get(Settings.decoderUsePruneFactor);
    boolean usePruneFact = Boolean.valueOf(usePruneFactStr).booleanValue();
    if (usePruneFact) {
      pruneFact = Double.parseDouble(Settings.get(Settings.decoderPruneFactor));
    }
    chart = new CKYChart(cellLimit, pruneFact);
  }

  /**
   * Initializes the chart for parsing the specified sentence.  Specifically,
   * this method will add a chart item for each possible part of speech for
   * each word.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   */
  protected void initialize(SexpList sentence) throws RemoteException {
    initialize(sentence, null);
  }

  /**
   * Initializes the chart for parsing the specified sentence, using the
   * specified coordinated list of part-of-speech tags when assigning parts
   * of speech to unknown words.
   *
   * @param sentence the sentence to parse, which must be a list containing
   * only symbols as its elements
   * @param tags a list of part-of-speech tags of the same length as
   * <code>sentence</code> that will be used when seeding the chart with
   * the parts of speech for unknown words; if the value of this argument is
   * <code>null</code>, then all possible parts of speech will be used
   * for unknown words
   */
  protected void initialize(SexpList sentence, SexpList tags)
    throws RemoteException {
    // preserve original sentence
    originalSentence.clear();
    originalSentence.addAll(sentence);

    SexpList singletonTagList = new SexpList(1);

    int sentLen = sentence.length();

    if (downcaseWords) {
      for (int i = 0; i < sentLen; i++) {
        Symbol downcasedWord =
          Symbol.add(sentence.symbolAt(i).toString().toLowerCase());
        sentence.set(i, downcasedWord);
      }
    }
    sentence = server.convertUnknownWords(sentence);

    for (int i = 0; i < sentLen; i++) {
      boolean wordIsUnknown = originalSentence.get(i) != sentence.get(i);
      SexpList tagSet = ((wordIsUnknown && tags != null) ?
                         new SexpList(1).add(tags.get(i)) :
                         (SexpList)posMap.get(sentence.get(i)));
      int numTags = tagSet.length();
      for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {
        Symbol tag = tagSet.symbolAt(tagIdx);
        Word headWord = new Word(sentence.symbolAt(i), tag);
        CKYItem item = new CKYItem(tag,
                                   headWord,
                                   emptySubcat,
                                   emptySubcat,
                                   null,
                                   null,
                                   null,
                                   SexpList.emptyList,
                                   i, i,
                                   false, true,
                                   0.0);
        chart.add(i, i, item);
        // addUnariesAndStops(i, i);
      }
    }
  }
}