package danbikel.parser.chinese;

import java.util.*;
import java.io.*;
import danbikel.parser.Constants;
import danbikel.parser.HeadFinder;
import danbikel.parser.Language;
import danbikel.parser.Treebank;
import danbikel.parser.Settings;
import danbikel.parser.Nonterminal;
import danbikel.lisp.*;

/**
 * Provides methods for language-specific processing of Chinese training parse
 * trees.  This class uses all the defaults provided by the superclass
 * {@link danbikel.parser.lang.AbstractTraining}, exccept that it overrides
 * {@link
 * danbikel.parser.lang.AbstractTraining#relabelSubjectlessSentences(Sexp)}.
 */
public class Training extends danbikel.parser.lang.AbstractTraining {
  // constants
  private final static String className = Training.class.getName();
  private final static Symbol argContextsSym = Symbol.add("arg-contexts");
  private final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  private final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  private final static Symbol wordsToPruneSym = Symbol.add("prune-words");
  private final static Symbol VP = Symbol.get("VP");

  // data members
  private Nonterminal nonterminal = new Nonterminal();

  /**
   * The default constructor, to be invoked by {@link danbikel.parser.Language}.
   * This constructor looks for a resource named by the property
   * <code>metadataPropertyPrefix + language</code>
   * where <code>metadataPropertyPrefix</code> is the value of
   * the constant {@link #metadataPropertyPrefix} and <code>language</code>
   * is the value of <code>Settings.get(Settings.language)</code>.
   * For example, the property for English is
   * <code>&quot;parser.training.metadata.english&quot;</code>.
   */
  public Training() throws FileNotFoundException, IOException {
    String language = Settings.get(Settings.language);
    String metadataResource = Settings.get(metadataPropertyPrefix + language);
    InputStream is = Settings.getFileOrResourceAsStream(this.getClass(),
							metadataResource);
    int bufSize = Constants.defaultFileBufsize;
    SexpTokenizer metadataTok =
      new SexpTokenizer(is, Language.encoding(), bufSize);
    readMetadata(metadataTok);
  }

  /**
   * We override {@link
   * danbikel.parser.lang.AbstractTraining#relabelSubjectlessSentences(Sexp)} so
   * that we can make the definition of a subjectless sentence
   * slightly more restrictive: a subjectless sentence not only must
   * have a null-element child that is marked with the subject
   * augmentation, but also its head must be a <tt>VP</tt> (this is
   * Mike Collins&rsquo; definition of a subjectless sentence).
   */
  public Sexp relabelSubjectlessSentences(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, make recursive call
      for (int i = 1; i < treeList.length(); i++)
	relabelSubjectlessSentences(treeList.get(i));

      Symbol parent = treeList.symbolAt(0);
      int headIdx = headFinder.findHead(treeList);
      if (headIdx == 0)
	System.err.println(tree);
      Symbol headChildLabel = treeList.getChildLabel(headIdx);
      Symbol sg = treebank.subjectlessSentenceLabel();
      /*
      if (treebank.isSentence(parent) &&
	  isCoordinatedPhrase(treeList, headIdx) &&
	  treebank.stripAugmentation(headChildLabel) == sg) {
	// this is a subjectless sentence, because it is an S that is
	// a coordinated phrase and whose head is a subjectless
	// sentence
	Nonterminal parsedParent = treebank.parseNonterminal(parent,
							     nonterminal);
	parsedParent.base = treebank.subjectlessSentenceLabel();
	treeList.set(0, parsedParent.toSymbol());
      }
      else */if (treebank.isSentence(parent) &&
	  treebank.getCanonical(headChildLabel) == VP) {
	for (int i = 1; i < treeListLen; i++) {
	  SexpList child = treeList.listAt(i);
	  Symbol childLabel = treeList.getChildLabel(i);
	  Nonterminal parsedChild =
	    treebank.parseNonterminal(childLabel, nonterminal);
	  Symbol subjectAugmentation = treebank.subjectAugmentation();

	  if (parsedChild.augmentations.contains(subjectAugmentation) &&
	      unaryProductionsToNull(child.get(1))) {
	    // we've got ourselves a subjectless sentence!
	    Nonterminal parsedParent = treebank.parseNonterminal(parent,
								 nonterminal);
	    parsedParent.base = treebank.subjectlessSentenceLabel();
	    treeList.set(0, parsedParent.toSymbol());
	    break;
	  }
	}
      }
    }
    return tree;
  }

  /**
   * A method to un-do the transformation provided by {@link
   * danbikel.parser.lang.AbstractTraining#repairBaseNPs(Sexp)} (for inclusion
   * in an overridden definition of
   * {@link danbikel.parser.lang.AbstractTraining#postProcess(Sexp)},
   * but currently unused by this class).
   *
   * @param tree the tree whose sentences that are right siblings of base NP
   *             nodes are to be re-inserted as rightmost children of their
   *             respective base NP nodes
   * @return the specified tree, having been modified in-place
   */
  protected Sexp unrepairBaseNPs(Sexp tree) {
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      // if we find a base NP followed by any type of S, unhook the S
      // from its parent and put as new final child of base NP subtree
      boolean thereAreAtLeastTwoChildren = treeList.length() > 2;
      if (thereAreAtLeastTwoChildren) {
	for (int i = 1; i < treeList.length() - 1; i++) {
	  Symbol currLabel = treeList.getChildLabel(i);
	  Symbol nextLabel = treeList.getChildLabel(i + 1);
	  if (currLabel == baseNP && isTypeOfSentence(nextLabel)) {
	    SexpList npbTree = treeList.listAt(i);
	    Sexp sentence = treeList.remove(i + 1); // unhook S from its parent
	    npbTree.add(sentence);
	    break;
	  }
	}
      }

      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	unrepairBaseNPs(treeList.get(i));
    }
    return tree;
  }


  /** Test driver for this class. */
  public static void main(String[] args) {
    String filename = null;
    boolean raisePunc = false, idArgs = false, subjectlessS = false;
    boolean stripAug = false, addBaseNPs = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-r"))
	  raisePunc = true;
	else if (args[i].equals("-i"))
	  idArgs = true;
	else if (args[i].equals("-s"))
	  subjectlessS = true;
	else if (args[i].equals("-a"))
	  stripAug = true;
	else if (args[i].equals("-n"))
	  addBaseNPs = true;
      }
      else
	filename = args[i];
    }

    if (filename == null) {
      System.err.println("usage: [-risan] <filename>\n" +
			 "where\n\t" +
			 "-r: raise punctuation\n\t" +
			 "-i: identify arguments\n\t" +
			 "-s: relabel subjectless sentences\n\t" +
			 "-a: strip augmentations\n\t" +
			 "-n: add/relabel base NPs");
      System.exit(1);
    }

    Training training = (Training)Language.training();
    training.printMetadata();

    try {
      SexpTokenizer tok = new SexpTokenizer(filename, Language.encoding(),
					    Constants.defaultFileBufsize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	if (raisePunc)
	  System.out.println(training.raisePunctuation(curr));
	if (idArgs)
	  System.out.println(training.identifyArguments(curr));
	if (subjectlessS)
	  System.out.println(training.relabelSubjectlessSentences(curr));
	if (stripAug)
	  System.out.println(training.stripAugmentations(curr));
	if (addBaseNPs)
	  System.out.println(training.addBaseNPs(curr));
      }
      System.out.println("\n\n");
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
