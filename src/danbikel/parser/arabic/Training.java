package danbikel.parser.arabic;

import danbikel.parser.Constants;
import danbikel.parser.HeadFinder;
import danbikel.parser.Language;
import danbikel.parser.Treebank;
import danbikel.parser.Settings;
import danbikel.parser.Nonterminal;
import danbikel.parser.Word;
import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

/**
 * Provides methods for language-specific processing of training parse trees.
 * Even though this subclass of {@link danbikel.parser.Training} is
 * in the default English language package, its primary purpose is simply
 * to fill in the {@link #argContexts}, {@link #semTagArgStopSet} and
 * {@link #nodesToPrune} data members using a metadata resource.  If this
 * capability is desired in another language package, this class may be
 * subclassed.
 * <p>
 * This class also re-defined the method
 * {@link danbikel.parser.lang.AbstractTraining#hasPossessiveChild(Sexp)}.
 */
public class Training extends danbikel.parser.lang.AbstractTraining {
  // constants
  private final static String className = Training.class.getName();
  private final static Symbol argContextsSym = Symbol.add("arg-contexts");
  private final static Symbol semTagArgStopListSym =
    Symbol.add("sem-tag-arg-stop-list");
  private final static Symbol nodesToPruneSym = Symbol.add("prune-nodes");
  private final static Symbol tagMapSym = Symbol.add("tag-map");
  private final static Symbol VP = Symbol.get("VP");
  private final static Symbol X = Symbol.get("X");
  private final static Symbol punc = Symbol.get("PUNC");
  private final static Symbol nonAlpha = Symbol.get("NON_ALPHABETIC");
  private final static Symbol nonAlphaPunc =
    Symbol.get("NON_ALPHABETIC_PUNCTUATION");
  private final static Symbol period = Symbol.get(".");
  private final static Symbol quotePeriod = Symbol.get("\".");
  private final static Symbol comma = Symbol.get(",");

  private final static boolean removeNounSuffix = false;
  private final static boolean removeDetPrefix  = false;
  private final static boolean removePerson     = true;
  private final static boolean removeNumber     = false;
  private final static boolean removeGender     = true;
  private final static boolean removeCase       = false;
  private final static boolean removeDefinite   = true;
  private final static boolean removePronoun    = true;
  private final static boolean removeMood       = true;

  protected final static String[] nounSuffixMarkers = {"+NSUFF"};

  protected final static String[] detPrefixMarkers = {"DET+"};

  protected final static String[] personMarkers = {
    "_1P", "_1S",
    "_2FS", "_2FP", "_2MS", "_2MP",
    "_3D", "_3FS", "_3FP", "_3MS", "_3MP",
    ":1P", ":1S",
    ":2FS", ":2FP", ":2MS", ":2MP",
    ":3D", ":3FS", ":3FP", ":3MS", ":3MP"
  };
  protected final static String[] numberMarkers = {"_SG", "_PL", "_DUAL","_DU"};
  protected final static String[] genderMarkers = {"_MASC", "_FEM"};
  protected final static String[] caseMarkers = {"_NOM", "_ACCGEN", "_ACC"};
  protected final static String[] definiteMarkers = {"_INDEF", "_DEF"};
  protected final static String[] pronounMarkers = {"_POSS", "_INDEF"};
  protected final static String[] moodMarkers = {"_MOOD:I", "_MOOD:SJ"};

  // the following two arrays must be coordinated for the contains() method
  // to work properly
  protected final static boolean[] remove = {
    removeNounSuffix,
    removeDetPrefix,
    removePerson,
    removeNumber,
    removeGender,
    removeCase,
    removeDefinite,
    removePronoun,
    removeMood
  };
  protected final static String[][] markers = {
    nounSuffixMarkers,
    detPrefixMarkers,
    personMarkers,
    numberMarkers,
    genderMarkers,
    caseMarkers,
    definiteMarkers,
    pronounMarkers,
    moodMarkers
  };

  /**
   * If regularizeVerbs is <code>true</code>, it indicates that part of speech
   * tags that contain any of the patterns in the {@link #verbPatterns} array
   * should be transformed simply into the pattern itself.  For example, the
   * tag <tt>IV2D+VERB_IMPERFECT+IVSUFF_SUBJ:D_MOOD:SJ</tt> would be
   * transformed into, simply, <tt>VERB_IMPERFECT</tt>.
   */
  protected final static boolean regularizeVerbs = true;
  /**
   * The match patterns used when {@link #regularizeVerbs} is
   * <code>true</code>.
   */
  protected final static String[] verbPatterns = {
    "PASSIVE_VERB", "VERB_IMPERFECT", "VERB_PASSIVE", "VERB_PERFECT"
  };


  /**
   * The prefix of the property to get the resource required by the default
   * constructor.  The value of this constant is
   * <code>&quot;parser.training.metadata.&quot;</code>.
   */
  protected final static String metadataPropertyPrefix =
    "parser.training.metadata.";

  // data members
  private Nonterminal nonterminal = new Nonterminal();
  private danbikel.util.HashMap transformations = new danbikel.util.HashMap();

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

  public Sexp preProcess(Sexp tree) {
    transformTags(tree);
    prune(tree);
    addBaseNPs(tree);
    //repairBaseNPs(tree);
    //addGapInformation(tree);
    //relabelSubjectlessSentences(tree);
    removeNullElements(tree);
    raisePunctuation(tree);
    identifyArguments(tree);
    stripAugmentations(tree);
    return tree;
  }

  public SexpList preProcessTest(SexpList sentence,
				 SexpList originalWords, SexpList tags) {
    if (tags == null)
      return super.preProcessTest(sentence, originalWords, tags);
    SexpList processed = new SexpList(2);
    processed.add(sentence);
    int numWords = sentence.size();
    for (int i = 0; i < numWords; i++) {
      Symbol origWord = originalWords.symbolAt(i);

      SexpList currWordTags = tags.listAt(i);
      int numTags = currWordTags.size();

      for (int tagIdx = 0; tagIdx < numTags; tagIdx++) {

	Symbol origTag = currWordTags.symbolAt(tagIdx);
	Word word = danbikel.parser.Words.get(origWord, origTag);

	currWordTags.set(tagIdx, TagMap.transformTag(word));
      }

      tags.set(i, currWordTags);
    }
    processed.add(tags);
    return processed;
  }

  public boolean isValidTree(Sexp tree) {
    // we invalidate top-level "X" sentences, which are not annotated for
    // syntax because they are headers/headlines
    if (tree.isList()) {
      SexpList treeList = tree.list();
      if (treeList.get(0).isSymbol() && treeList.symbolAt(0) == X)
	return false;
    }
    return super.isValidTree(tree);
  }

  /**
   * Helper method used by {@link TagMap#transformTag(Word)}.
   */
  protected int contains(StringBuffer searchBuf, String[] searchPatterns,
			 IntCounter patternIdx) {
    int numPatterns = searchPatterns.length;
    int idx = -1;
    for (int i = 0; i < numPatterns; i++) {
      idx = searchBuf.indexOf(searchPatterns[i]);
      if (idx != -1) {
	patternIdx.set(i);
	return idx;
      }
    }
    patternIdx.set(-1);
    return -1;
  }

  protected Symbol transformTagOld(Word word) {
    // if the tag is a non-alphabetic punctuation and the word itself is
    // either a period or a comma, then the tag should be identical to the word
    if (word.tag() == nonAlpha || word.tag() == nonAlphaPunc) {
      if (word.word() == period ||
	  word.word() == quotePeriod ||
	  word.word() == comma) {
	return word.word();
      }
      else
	return word.tag();
    }
    else {
      // first, check cache of transformed tags
      Map.Entry cacheEntry = transformations.getEntry(word.tag());
      if (cacheEntry != null)
	return (Symbol)cacheEntry.getValue();

      StringBuffer tagBuf = new StringBuffer(word.tag().toString());
      int idx = -1;
      IntCounter patternIdx = new IntCounter(-1);

      if (regularizeVerbs &&
	  (idx = contains(tagBuf, verbPatterns, patternIdx)) != -1) {
	Symbol matchedPattern = Symbol.get(verbPatterns[patternIdx.get()]);
	transformations.put(word.tag(), matchedPattern);
	return matchedPattern;
      }

      for (int i = 0; i < remove.length; i++) {
	if (remove[i]) {
	  idx = contains(tagBuf, markers[i], patternIdx);
	  if (idx != -1) {
	    String matchedPattern = markers[i][patternIdx.get()];
	    tagBuf.delete(idx, idx + matchedPattern.length());
	  }
	}
      }

      Symbol transformedTag = Symbol.get(tagBuf.toString());

      transformations.put(word.tag(), transformedTag);

      return transformedTag;
    }
  }

  protected Sexp transformTags(Sexp tree) {
    if (Language.treebank().isPreterminal(tree) &&
	!Language.treebank().isNullElementPreterminal(tree)) {
      Word word = Language.treebank().makeWord(tree);
      Symbol newTag = TagMap.transformTag(word);
      tree.list().set(0, newTag);
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	transformTags(treeList.get(i));
    }
    return tree;
  }

  /**
   * We override this method so that it always returns <code>false</code>,
   * so that the default implementation of <code>addBaseNPs(Sexp)<code>
   * never considers an <tt>NP</tt> to be a possessive <tt>NP</tt>.  Thus,
   * the behavior of <code>addBaseNPs</code> is much simpler: all and only
   * <tt>NP</tt>s that do not dominate other NPs will be relabeled
   * <tt>NPB</tt>.
   *
   * @param tree the tree to be tested
   * @return <code>false</code>, regardless of the value of the specified tree`
   */
  protected boolean hasPossessiveChild(Sexp tree) {
    return false;
  }

  /**
   * For arabic, we do <i>not</i> want to transform preterminals
   * (parts of speech) to their canonical forms, so this method is overridden.
   *
   * @param tree the tree for which nonterminals, but not parts of speech,
   * are to be transformed into their canonical forms
   */
  protected void canonicalizeNonterminals(Sexp tree) {
    if (Language.treebank().isPreterminal(tree)) {
      return;
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      Symbol currLabel = treeList.symbolAt(0);
      treeList.set(0, Language.treebank().getCanonical(currLabel));
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	canonicalizeNonterminals(treeList.get(i));
    }
  }

  /**
   * Reads metadata to fill in {@link #argContexts} and
   * {@link #semTagArgStopSet}.  Does no format
   * checking on the S-expressions of the metadata resource.
   *
   * @param metadataTok tokenizer for stream of S-expressions containing
   * metadata for this class
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
      else if (dataType == tagMapSym) {
	for (int i = 1; i < metadataLen; i++) {
	  SexpList mapping = metadata.get(i).list();
	  TagMap.add(mapping.symbolAt(0), mapping.symbolAt(1));
	}
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
      System.err.println("parent: " + parent + "\t" +
			 "children: " + argContexts.get(parent));
    }
    Iterator argStopSetIt = semTagArgStopSet.iterator();
    System.err.print("(");
    if (argStopSetIt.hasNext())
      System.err.print(argStopSetIt.next());
    while (argStopSetIt.hasNext()) {
      System.err.print(' ');
      System.err.print(argStopSetIt.next());
    }
    System.err.println(")");
  }

  public void postProcess(Sexp tree) {
    //unrepairBaseNPs(tree);
    super.postProcess(tree);
  }

  private final static String[] usageMsg = {
    "usage: [-tpnria] [-combine] <filename>\n" +
    "where\n\t" +
    "-t: transform tags\n\t" +
    "-p: prune trees\n\t" +
    "-b: add/relabel base NPs\n\t" +
    "-n: remove null elements\n\t" +
    "-r: raise punctuation\n\t" +
    "-i: identify arguments\n\t" +
    "-a: strip augmentations\n\t" +
    "-combine: by default, the tree will be printed to " +
    "System.out after each\n\t\ttransformation; " +
    "if this flag is present, all transformations will "+
    "be\n\t\tapplied and only the final, fully-" +
    "transformed tree will be printed"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++) {
      System.err.println(usageMsg[i]);
    }
    System.exit(1);
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    String filename = null;
    boolean transformTags = false;
    boolean prune = false;
    boolean addBaseNPs = false;
    boolean removeNullElements = false;
    boolean raisePunc = false;
    boolean idArgs = false;
    boolean stripAug = false;
    boolean combineTransformations = false;

    for (int i = 0; i < args.length; i++) {
      // for each arg that begins with a '-', examine each of its letters
      // and set booleans accordingly, except for "-combine" flag
      if (args[i].charAt(0) == '-') {
	if (args[i].equals("-combine"))
	  combineTransformations = true;
	else {
	  for (int charIdx = 1; charIdx < args[i].length(); charIdx++) {
	    char curr = args[i].charAt(charIdx);
	    switch (curr) {
	    case 't':
	      transformTags = true;
	      break;
	    case 'p':
	      prune = true;
	      break;
	    case 'b':
	      addBaseNPs = true;
	      break;
	    case 'n':
	      removeNullElements = true;
	      break;
	    case 'r':
	      raisePunc = true;
	      break;
	    case 'i':
	      idArgs = true;
	      break;
	    case 'a':
	      stripAug = true;
	      break;
	    default:
	      System.err.println("illegal flag: " + curr);
	      usage();
	    }
	  }
	}
      }
      else
	filename = args[i];
    }

    if (filename == null) {
      usage();
    }

    Training training = (Training)Language.training();
    //training.printMetadata();

    try {
      SexpTokenizer tok = new SexpTokenizer(filename, Language.encoding(),
					    Constants.defaultFileBufsize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	if (transformTags) {
	  curr = training.transformTags(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (prune) {
	  curr = training.prune(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (addBaseNPs) {
	  curr =training.addBaseNPs(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (removeNullElements) {
	  curr = training.removeNullElements(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (raisePunc) {
	  curr = training.raisePunctuation(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (idArgs) {
	  curr =training.identifyArguments(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (stripAug) {
	  curr = training.stripAugmentations(curr);
	  if (!combineTransformations)
	    System.out.println(curr);
	}
	if (combineTransformations)
	  System.out.println(curr);
      }
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
