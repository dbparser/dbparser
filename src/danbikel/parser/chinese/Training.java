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
 * Provides methods for language-specific processing of training parse trees.
 * Even though this subclass of {@link danbikel.parser.Training} is
 * in the default English language package, its primary purpose is simply
 * to fill in the {@link danbikel.parser.Training#argContexts},
 * {@link danbikel.parser.Training#semTagArgStopSet} and
 * {@link danbikel.parser.Training#nodesToPrune} data members using
 * a metadata resource.  If this capability is desired in another language
 * package, this class may be subclassed.
 */
public class Training extends danbikel.parser.Training {
  // constants
  private final static String className = Training.class.getName();
  private final static Symbol argContextsSym = Symbol.add("arg-contexts");
  private final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  private final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  private final static Symbol VP = Symbol.get("VP");

  /**
   * The prefix of the property to get the resource required by the default
   * constructor.  The value of this constant is
   * <code>&quot;parser.training.metadata.&quot;</code>.
   */
  protected final static String metadataPropertyPrefix =
    "parser.training.metadata.";

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
   * Reads metadata to fill in {@link danbikel.parser.Training#argContexts} and
   * {@link danbikel.parser.Training#semTagArgStopSet}.  Does no format
   * checking on the S-expressions of the metadata resource.
   *
   * @param metadata S-expressions containing metadata for this class
   */
  protected void readMetadata(SexpTokenizer metadataTok) throws IOException {
    Sexp metadataSexp = null;
    while ((metadataSexp = Sexp.read(metadataTok)) != null) {
      SexpList metadata = metadataSexp.list();
      int metadataLen = metadata.length();
      Symbol dataType = metadata.first().symbol();
      if (dataType == argContextsSym) {
	for (int i = 1; i < metadataLen; i++) {
	  SexpList context = metadata.get(i).list();
	  argContexts.put(context.get(0), context.get(1));
	}
      }
      else if (dataType == semTagArgStopListSym) {
	SexpList semTagArgStopList = metadata.get(1).list();
	for (int i = 0; i < semTagArgStopList.length(); i++)
	  semTagArgStopSet.add(semTagArgStopList.get(i));
      }
      else if (dataType == nodesToPruneSym) {
	SexpList nodesToPruneList = metadata.get(1).list();
	for (int i = 0; i < nodesToPruneList.length(); i++)
	  nodesToPrune.add(nodesToPruneList.get(i));
      }
      else {
	// unrecognized data type
      }
    }
  }

  /** Debugging method to print the metadata used by this class. */
  public void printMetadata() {
    Iterator argContextsIt = argContexts.keySet().iterator();
    while (argContextsIt.hasNext()) {
      Sexp parent = (Sexp)argContextsIt.next();
      System.out.println("parent: " + parent + "\t" +
			 "children: " + argContexts.get(parent));
    }
    Iterator argStopSetIt = semTagArgStopSet.iterator();
    System.out.print("(");
    if (argStopSetIt.hasNext())
      System.out.print(argStopSetIt.next());
    while (argStopSetIt.hasNext()) {
      System.out.print(' ');
      System.out.print(argStopSetIt.next());
    }
    System.out.println(")");
  }

  /**
   * We override {@link
   * danbikel.parser.Training#relabelSubjectlessSentences(Sexp)} so
   * that we can make the definition of a subjectless sentence
   * slightly more restrictive: a subjectless sentence not only must
   * have a null-element child that is marked with the subject
   * augmentation, but also its head must be a <tt>VP</tt> (this is
   * Mike Collins' definition of a subjectless sentence).
   */
  public Sexp relabelSubjectlessSentences(Sexp tree) {
    if (tree.isSymbol())
      return tree;
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

  protected Sexp unrepairBaseNPs(Sexp tree) {
    if (tree.isSymbol())
      return tree;
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

  protected void postProcess(Sexp tree) {
    //unrepairBaseNPs(tree);
    super.postProcess(tree);
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
