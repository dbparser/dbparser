package danbikel.parser;

import java.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Provides methods for language-specific preprocessing of training parse
 * trees.  The primary method to be invoked from this class is
 * {@link #preProcess(Sexp)}.
 * <p>
 * <b>Concurrency note</b>: As training is typically a sequential
 * process, with very few noted exceptions, <i>none of the default
 * implementations of the methods of this abstract base class is
 * thread-safe</i>.  If thread-safe guarantees are desired, the
 * methods of this class should be overridden.
 *
 * @see #preProcess(Sexp)
 */
public abstract class Training implements Serializable {

  // constants

  private static final String className = Training.class.getName();

  /**
   * Property to be checked in {@link Settings} by {@link
   * #addGapInformation(Sexp)}.  The value of this constant is the string
   * <code>&quot;parser.training.addgapinfo&quot;</code>.
   */
  protected static final String addGapInfoProperty =
    "parser.training.addgapinfo";

  /**
   * Converts the string value of the property {@link #addGapInfoProperty}
   * to a boolean, using <code>Boolean.valueOf</code>.
   */
  protected static final boolean addGapInfo =
    Boolean.valueOf(Settings.get(addGapInfoProperty)).booleanValue();

  // data members

  private Nonterminal nonterminal = new Nonterminal();
  private Nonterminal addGapData = new Nonterminal();

  private Treebank treebank = Language.treebank;
  private HeadFinder headFinder = Language.headFinder;

  /**
   * The symbol that will be used to identify nonterminals whose subtrees
   * contain a gap (a trace).  This method is used by {@link
   * #stripAugmentations(Sexp)}, so that gap augmentations that are added by
   * {@link #addGapInformation(Sexp)} do not get removed.  The default value is
   * the symbol returned by <code>Symbol.add(&quot;g&quot;)</code>.  If this
   * default value conflicts with an augmentation already used in a particular
   * Treebank, this value should be reassigned in the constructor of a
   * subclass.
   */
  protected Symbol gapAugmentation = Symbol.add("g");

  /**
   * The string consisting of the canonical augmentation delimiter
   * concatenated with the gap augmentation, to be used in
   * identifying nonterminals that contain gap augmentations.
   *
   * @see Treebank#canonicalAugDelimiter
   * @see #gapAugmentation
   */
  protected String delimAndGapStr =
    treebank.canonicalAugDelimiter() + gapAugmentation.toString();
  /**
   * The length of {@link #delimAndGapStr}, cached here for efficiency
   * and convenience.
   */
  protected int delimAndGapStrLen = delimAndGapStr.length();


  /**
   * The symbol that will be used to identify argument nonterminals.  This
   * method is used by {@link #stripAugmentations(Sexp)}, so that argument
   * augmentations that are added by {@link #identifyArguments(Sexp)} do not
   * get removed.  The default value is the symbol returned by
   * <code>Symbol.add(&quot;A&quot;)</code>.  If this default value conflicts
   * with an augmentation already used in a particular Treebank, this value
   * should be reassigned in the constructor of a subclass.
   */
  protected Symbol argAugmentation = Symbol.add("A");

  /**
   * The string composed of the canonical augmentation delimiter prepended
   * to the argument augmentation, for efficient lookup of whether or not
   * a nonterminal label contains an argument augmentation.
   *
   * @see #isArgumentFast(Symbol)
   */
  protected String canonicalDelimPlusArgAug;

  /**
   * The symbol that gets assigned as the part of speech for null
   * preterminals that represent traces that have undergone WH-movement, as
   * relabeled by the default implementation of {@link
   * #addGapInformation(Sexp)}.  The default value is the return value of
   * <code>Symbol.add(&quot;*TRACE*&quot;)</code>.  If this maps to an actual
   * part of speech tag or nonterminal label in a particular Treebank, this
   * data member should be reassigned in the constructor of a subclass.
   */
  protected Symbol traceTag = Symbol.add("*TRACE*");

  /** Data member returned by the accessor method of the same name. */
  private Symbol startSym = Symbol.add("+START+");
  /** Data member returned by the accessor method of the same name. */
  private Symbol stopSym = Symbol.add("+STOP+");
  /** Data member returned by the accessor method of the same name. */
  private Word startWord = new Word(startSym, startSym);
  /** Data member returned by the accessor method of the same name. */
  private Word stopWord = new Word(stopSym, stopSym);
  /** Data member returned by the accessor method of the same name. */
  private Symbol topSym = Symbol.add("+TOP+");
  /** Data member returned by the accessor method of the same name. */
  private Word topWord = new Word(topSym, topSym);

  /**
   * A Symbol created from the first character of {@link
   * Treebank#augmentationDelimiters}.
   */
  protected final Symbol canonicalAugDelimSym;

  /**
   * Data member to store the set of nodes to prune for the default
   * implementation of {@link #prune(Sexp)}.  The set should only contain
   * objects of type <code>Symbol</code>, and the elements of this set
   * should be added in the constructor of a subclass.
   */
  protected Set nodesToPrune;

  /**
   * The set of preterminals (<code>Sexp</code> objects) that have been pruned
   * away.
   */
  protected Set prunedPreterms = new HashSet();

  /**
   * Data member used to store the map required by the default implementation
   * of the method {@link #identifyArguments(Sexp)}.  This data member maps
   * parent nonterminals to lists of children nonterminals, to indicate that
   * the children are candidates for being labeled as arguments in the presence
   * of that parent.  A children list may also be a list of the form
   * <pre>
   * (head &lt;offset&gt;)
   * </pre>
   * indicating to match a node <code>&lt;offset&gt;</code> away from the head
   * child of the parent that was mapped to this children list.  The keys and
   * values of this map should be added in the constructor of a subclass.
   * The keys of this map must be of type {@link Symbol}, and the values of
   * this map must be of type {@link SexpList}.
   *
   * @see #identifyArguments(Sexp)
   */
  protected Map argContexts;
  /**
   * Data member used to store the set required by the method {@link
   * #identifyArguments(Sexp)}.  The set contains semantic tags (which is
   * English Treebank parlance) that prohibit a candidate argument child from
   * being relabeled as an argument.  The objects in this set must all be of
   * type <code>Symbol</code>.  The members of this set should be added in the
   * constructor of a subclass.
   *
   * @see #identifyArguments(Sexp)
   */
  protected Set semTagArgStopSet;
  /**
   * The symbol that is a possible mapping in {@link #argContexts} to indicate
   * to choose a child relative to the head as an argument.  For example, an
   * argument context might be <code>PP</code> mapping to <code>(head
   * 1))</code>, meaning that the child that is 1 position to the right of the
   * head child of a PP should be relabeled as an argument.  The value of this
   * data member is the symbol returned by
   * <code>Symbol.add(&quot;head&quot;)</code>.  In the unlikely event that
   * this value conflicts with a nonterminal in a particular Treebank, this
   * data member should be reassigned in the constructor of a subclass.
   *
   * @see #identifyArguments(Sexp)
   */
  protected Symbol headSym = Symbol.add("head");

  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate
   * to choose a child relative to the left side of the head as an argument.
   * For example, an argument context might be <code>VP</code> mapping to
   * <code>(head-left left MD VBD)</code>, meaning that the children to the left
   * of the head child should be searched from left to right, and the first
   * child found that is a member of the set <tt>{MD, VBD}</tt> should be
   * considered a possible argument of the head.
   */
  protected Symbol headPreSym = Symbol.add("head-pre");
  /**
   * The symbol that is a possible mapping {@link #argContexts} to indicate
   * to choose a child relative to the right side of the head as an argument.
   * For example, an argument context might be <code>PP</code> mapping to
   * <code>(head-right left PP NP WHNP ADJP)</code>, meaning that the children
   * to the right of the head child should be searched from left to right, and
   * the first child found that is a member of the set
   * <tt>{PP, NP, WHNP, ADJP}</tt> should be considered a possible argument
   * of the head.
   */
  protected Symbol headPostSym = Symbol.add("head-post");

  /**
   * The value of {@link Treebank#baseNPLabel}, cached for efficiency and
   * convenience.
   */
  protected final Symbol baseNP = treebank.baseNPLabel();
  /**
   * The value of {@link Treebank#NPLabel}, cached for efficiency and
   * convenience.
   */
  protected final Symbol NP = treebank.NPLabel();

  // data members used by raisePunctuation
  private SexpList addToRaise = new SexpList();
  private SexpList raise = new SexpList();
  /**
   * The set of preterminals (<code>Sexp</code> objects) that were "raised
   * away" by {@link #raisePunctuation(Sexp)} because they appeared either at
   * the beginning or the end of a sentence.
   */
  protected Set prunedPunctuation = new HashSet();

  // data member used by hasGap
  private ArrayList hasGapIndexStack = new ArrayList();

  // data member used by isArgumentFast
  //private Map isArgMap = Collections.synchronizedMap(new HashMap());

  /**
   * Default constructor for this abstract base class; sets {@link
   * #argContexts} to a new <code>Map</code> object, sets {@link
   * #semTagArgStopSet} to a new <code>Set</code> object and initializes {@link
   * #canonicalAugDelimSym}.  Subclass constructors are responsible for filling
   * in the data for {@link #argContexts} and {@link #semTagArgStopSet}.
   */
  protected Training() {
    argContexts = new HashMap();
    semTagArgStopSet = new HashSet();
    nodesToPrune = new HashSet();
    canonicalAugDelimSym =
      Symbol.add(new String(new char[] {treebank.canonicalAugDelimiter()}));
    canonicalDelimPlusArgAug =
      canonicalAugDelimSym.toString() + argAugmentation.toString();
  }

  /**
   * The method to call before counting events in a training parse tree.
   * This default implementation executes the following methods of this class
   * in order:
   * <ol>
   * <li> {@link #prune(Sexp)}
   * <li> {@link #addBaseNPs(Sexp)}
   * <li> {@link #addGapInformation(Sexp)}
   * <li> {@link #relabelSubjectlessSentences(Sexp)}
   * <li> {@link #removeNullElements(Sexp)}
   * <li> {@link #raisePunctuation(Sexp)}
   * <li> {@link #identifyArguments(Sexp)}
   * <li> {@link #stripAugmentations(Sexp)}
   * </ol>
   * While every attempt has been made to make the default implementations of
   * these preprocessing methods independent of one another, the order above is
   * not entirely arbitrary.  In particular:
   * <ul>
   * <li>{@link #addGapInformation(Sexp)} should be run after methods that
   * introduce new nodes, which in this case is {@link #addBaseNPs(Sexp)}, as
   * these new nodes may need to be used to thread the gap feature
   * <li>{@link #relabelSubjectlessSentences(Sexp)} should be run after
   * {@link #addGapInformation(Sexp)} because only those sentences whose
   * empty subjects are <i>not</i> the result of WH-movement should be
   * relabeled
   * <li>{@link #removeNullElements(Sexp)} should be run after any
   * methods that depend on the presence of null elements, such as
   *   <ul>
   *   <li>{@link #relabelSubjectlessSentences(Sexp)} because a sentence cannot
   *   be determined to be subjectless unless a null element is present as
   *   a child of a subject-marked node
   *   <br>and<br>
   *   <li>{@link #addGapInformation(Sexp)} because the determination of
   *   the location of a trace requires the presence of indexed null elements
   *   </ul>
   * <li>{@link #raisePunctuation(Sexp)} should be run after
   * {@link #removeNullElements(Sexp)} because a null element that is a
   * leftmost or rightmost child can block detection of a punctuation element
   * that needs to be raised after removal of the null element (if a punctuation
   * element is the next-to-leftmost or next-to-rightmost child of an interior
   * node)
   * <li>{@link #stripAugmentations(Sexp)} should be run after all methods
   * that may depend upon the presence of nonterminal augmentations: {@link
   * #identifyArguments(Sexp)}, {@link #relabelSubjectlessSentences(Sexp)} and
   * {@link #addGapInformation(Sexp)}
   * </ul>
   *
   * @param tree the parse tree to pre-process
   * @return <code>tree</code> having been pre-processed
   */
  public Sexp preProcess(Sexp tree) {
    prune(tree);
    addBaseNPs(tree);
    addGapInformation(tree);
    relabelSubjectlessSentences(tree);
    removeNullElements(tree);
    raisePunctuation(tree);
    identifyArguments(tree);
    stripAugmentations(tree);
    return tree;
  }

  /**
   * Returns <code>true</code> if <code>tree</code> is a preterminal (the base
   * case) or is a list with the first element of type <code>Symbol</code> (the
   * node label) and subsequent elements are valid trees (the recursive case).
   * If a language package requires a different definition of training parse
   * tree validity, this method should be overridden.  However, changing the
   * definition of tree validity should be done with care, as the default
   * implementations of the tree-processing methods in this class require trees
   * that correspond to the definition of validity implemented by this method.
   *
   * @param tree the parse tree to check for validity
   * @see Treebank#isPreterminal(Sexp)
   */
  public static boolean isValidTree(Sexp tree) {
    if (tree.isSymbol())
      return false;
    if (Language.treebank.isPreterminal(tree))
      return true;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      if (treeList.first().isSymbol() == false)
	return false;
      int treeListLen = treeList.length();
      if (treeListLen == 1)
	return false;
      for (int i = 1; i < treeListLen; i++)
	if (isValidTree(treeList.get(i)) == false)
	  return false;
    }
    return true;
  }

  /**
   * Returns <code>true</code> if the specified label is a node to prune.
   */
  /*
  public boolean isNodeToPrune(Symbol label) {
    return nodesToPrune.contains(label);
  }
  */

  /**
   * Returns the set of pruned preterminals (<code>Sexp</code> objects).
   *
   * @see #prune(Sexp)
   */
  public Set getPrunedPreterms() { return prunedPreterms; }

  /**
   * Prunes away subtrees that have a root that is an element of
   * <code>nodesToPrune</code>.
   * <p>
   * <b>Side effect</b>: An internal set of pruned preterminals will
   * be updated.  This set may be accessed via {@link #getPrunedPreterms()}.
   * <p>
   * <b>Bugs</b>: Cannot prune away entire tree if the root label of the
   * specified tree is in <code>nodesToPrune</code>.
   * <p>
   *
   * @param tree the parse tree to prune
   * @return <code>tree</code> having been pruned
   *
   * @see #nodesToPrune
   */
  public Sexp prune(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 1; i < treeList.length(); i++)
	if (nodesToPrune.contains(treeList.getChildLabel(i))) {
          collectPreterms(prunedPreterms, treeList.get(i));
	  treeList.remove(i--);
	}
	else
	  prune(treeList.get(i));
    }
    return tree;
  }

  private final void collectPreterms(Set preterms, Sexp tree) {
    if (treebank.isPreterminal(tree)) {
      preterms.add(tree);
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
        collectPreterms(preterms, treeList.get(i));
    }
  }

  /**
   * Augments labels of nonterminals that are arguments.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if argument identification is not desired for a particular
   * language package.
   *
   * @param tree the parse tree to modify
   * @return a reference to the modified <code>tree</code> object
   * @see Treebank#canonicalAugDelimiter
   */
  public Sexp identifyArguments(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      // first, make recursive call
      for (int childIdx = 1; childIdx < treeListLen; childIdx++)
	identifyArguments(treeList.get(childIdx));

      Symbol parent =
	treebank.getCanonical(tree.list().first().symbol());
      SexpList candidateChildren = (SexpList)argContexts.get(parent);

      if (candidateChildren != null) {
        int headIdx = headFinder.findHead(treeList);
        Symbol headChild = (headIdx == 0 ? null :
                            treeList.get(headIdx).list().first().symbol());

	if (isCoordinatedPhrase(tree, headIdx))
	  return tree;

	// either the candidate list has the form (head <int>) or
        // (head-left <list>) or (head-right <list>) or it's a list of actual
        // nonterminal labels
	if (candidateChildren.first().symbol() == headSym ||
            candidateChildren.first().symbol() == headPreSym ||
            candidateChildren.first().symbol() == headPostSym) {
          int argIdx = -1;
          Symbol child = null;
          boolean foundArg = false;

          if (candidateChildren.first().symbol() == headSym) {
            String offsetStr = candidateChildren.get(1).toString();
            int headOffset = Integer.parseInt(offsetStr);
            // IF there is a head and IF that head is not equal to parent (i.e.,
            // if it's not a situation like (PP (PP ...) (CC and) (PP ...)) ) and
            // if the headIdx plus the headOffset is still valid, then we've got
            // an argument
            argIdx = headIdx + headOffset;
            if (headIdx > 0 &&
                treebank.getCanonical(headChild) != parent &&
                argIdx > 0 && argIdx < treeListLen) {
              child = treeList.getChildLabel(argIdx);

              // if the arg is actually a conjunction or punctuation,
              // if possible, find the first child to the right of argIdx that
              // is not a preterminal
              if (treebank.isConjunction(child) || treebank.isPunctuation(child)) {
                for (int i = argIdx + 1; i < treeListLen; i++) {
                  if (!treebank.isPreterminal(treeList.get(i))) {
                    argIdx = i;
                    child = treeList.getChildLabel(argIdx);
                    break;
                  }
                }
              }
              foundArg = true;
            }
          }
          else {
            SexpList searchInstruction = candidateChildren;
            int searchInstructionLen = searchInstruction.length();
            boolean leftSide =
              searchInstruction.symbolAt(0) == headPreSym;
            boolean leftToRight =
              searchInstruction.symbolAt(1) == Constants.firstSym;
	    boolean negativeSearch =
	      searchInstruction.symbolAt(2) == Constants.notSym;
	    int searchSetStartIdx = negativeSearch ? 3 : 2;	      
            if (headIdx > 0 && treebank.getCanonical(headChild) != parent) {
	    //if (headIdx > 0) {
              int increment = leftToRight ? 1 : -1;
              int startIdx = -1, endIdx = -1;
              if (leftSide) {
                startIdx = leftToRight ?  1            :  headIdx - 1;
                endIdx   = leftToRight ?  headIdx - 1  :  1;
              }
              else {
                startIdx = leftToRight ? headIdx + 1      :  treeListLen - 1;
                endIdx   = leftToRight ? treeListLen - 1  :  headIdx + 1;
              }
              // start looking one after (or before) the head index for
              // first occurrence of a symbol in the search set, which is
              // comprised of the symbols at indices 2..searchInstructionLen
              // in the list searchInstruction
              SEARCH:
              for (int i = startIdx;
                   leftToRight ? i <= endIdx : i >= endIdx; i += increment) {
		Symbol currChild = treeList.getChildLabel(i);
		Symbol noAugChild = treebank.stripAugmentation(currChild);
                for (int j = searchSetStartIdx; j < searchInstructionLen; j++) {
		  Symbol searchSym = searchInstruction.symbolAt(j);
                  //if (noAugChild == searchSym) {
		  if (currChild == searchSym) {
		    if (negativeSearch)
		      continue SEARCH;
		    else {
		      argIdx = i;
		      child = currChild;
		      foundArg = true;
		      break SEARCH;
		    }
                  }
                }
		// if no match for any nt in search set and this is
		// negative search, we've found what we're looking for
		if (negativeSearch) {
		  argIdx = i;
		  child = currChild;
		  foundArg = true;
		  break SEARCH;
		}
              }
            }
          }

          if (foundArg) {
            Nonterminal parsedChild =
              treebank.parseNonterminal(child, nonterminal);
            treebank.addAugmentation(parsedChild, argAugmentation);
            treeList.setChildLabel(argIdx, parsedChild.toSymbol());
          }
	}
	else {
	  // the candidate list is a list of actual nonterminal labels
	  if (treebank.getCanonical(headChild) != parent) {
	  //if (true) {
	    for (int childIdx = 1; childIdx < treeListLen; childIdx++) {
              if (childIdx == headIdx)
                continue;
	      Symbol child = treeList.getChildLabel(childIdx);
	      int candidateChildIdx =
		candidateChildren.indexOf(treebank.getCanonical(child));
	      if (candidateChildIdx != -1) {
		Nonterminal parsedChild =
		  treebank.parseNonterminal(child, nonterminal);
		SexpList augmentations = parsedChild.augmentations;
		int augLen = augmentations.length();
		boolean isArg = true;
		for (int i = 0; i < augLen; i++) {
		  if (semTagArgStopSet.contains(augmentations.get(i))) {
		    isArg = false;
		    break;
		  }
		}
		if (isArg) {
		  treebank.addAugmentation(parsedChild, argAugmentation);
		  treeList.setChildLabel(childIdx, parsedChild.toSymbol());
		}
	      }
	    }
	  }
	}
      }
    }
    return tree;
  }

  /**
   * The symbol that is used to mark argument (required) nonterminals by
   * {@link #identifyArguments(Sexp)}.
   */
  public Symbol argAugmentation() { return argAugmentation; }

  /**
   * Returns <code>true</code> if and only if <code>label</code> has an
   * argument augmentation as added by {@link #identifyArguments(Sexp)}.
   */
  public boolean isArgument(Symbol label) {
    Nonterminal parsedLabel = treebank.parseNonterminal(label, nonterminal);
    return parsedLabel.augmentations.contains(argAugmentation);
  }

  /**
   * Returns <code>true</code> if and only if the specified nonterminal
   * label has an argument augmentation preceded by the canonical
   * augmentaion delimiter.  Unlike {@link #isArgument(Symbol)}, this
   * method is thread-safe.  Also, it is more efficient than
   * {@link #isArgument(Symbol)}, as it does not actually parse the
   * specified nonterminal label.
   * <p>
   * <b>Bugs</b>: This method will return a false positive if
   * {@link #canonicalDelimPlusArgAug} is contained within any of the
   * print-names of the symbols in the array
   * {@link Treebank#nonterminalExceptionSet}.  This case is unlikely
   * to occur, but is possible.
   */
  public boolean isArgumentFast(Symbol label) {
    /*
    Boolean isArgBool = (Boolean)isArgMap.get(label);
    if (isArgBool == null) {
      boolean isArg =
        label.toString().indexOf(canonicalDelimPlusArgAug, 1) != -1;
      isArgBool = isArg ? Boolean.TRUE : Boolean.FALSE;
      isArgMap.put(label, isArgBool);
    }
    return isArgBool == Boolean.TRUE;
    */
    return label.toString().indexOf(canonicalDelimPlusArgAug, 1) != -1;
  }

  /**
   * Augments nonterminals to include gap information for WHNP's that have
   * moved and leave traces (gaps), as in the GPSG framework.  This method is
   * optional, and may simply return <code>tree</code> untouched if gap
   * information is desired for a particular language package.  The default
   * implementation of this method checks the setting of the property {@link
   * #addGapInfoProperty} in the {@link Settings} object: if this property is
   * <code>false</code> (as determined by
   * <code>Boolean.valueOf(String)</code>), then <code>tree</code> is returned
   * untouched; otherwise, this method simply calls {@link #hasGap(Sexp, Sexp,
   * ArrayList)}.
   *
   * @param tree the parse tree to which to add gapping
   * @return the same <code>tree</code> that was passed in, with certain
   * nodes modified to include gap information
   * @see #hasGap(Sexp, Sexp, ArrayList)
   */
  public Sexp addGapInformation(Sexp tree) {
    if (!addGapInfo)
      return tree;
    hasGapIndexStack.clear();
    hasGap(tree, tree, hasGapIndexStack);
    return tree;
  }


  /**
   * Returns -1 if <code>tree</code> has no gap (trace), or the index of the
   * trace otherwise.  If <code>tree</code> is a null preterminal with an
   * indexed terminal (a trace) that matches the index at the top of
   * <code>indexStack</code>, then that index is popped off the stack, the
   * preterminal label is changed to be {@link #traceTag}, and the index of the
   * trace is returned.  If a child of <code>tree</code> has a gap but another
   * child is a WHNP that is coindexed, then the gap is &quot;filled&quot;, and
   * this method returns -1; otherwise, this method augments the label of
   * <code>tree</code> with {@link #gapAugmentation} and returns the gap index
   * of the child.
   * <p>
   * Put informally, this method does a depth-first search of <code>tree</code>,
   * pushing the indices of any indexed WHNP nodes onto <code>indexStack</code>
   * and popping off those indices when the corresponding null element is found
   * someplace deeper in the tree.  The stack is necessary to allow for
   * the nesting of gaps in a tree.
   * <p>
   * <b>Algorithm</b>:
   * <pre>
   * <font color=red>// base case</font>
   * <b>if</b> tree is a null-element preterminal with an index that matches top of
   *    indexStack
   * <b>then</b>
   *   modify preterminal to be traceTag;
   *   <b>return</b> pop(indexStack);
   * <b>endif</b>
   *
   * <b>int</b> numWHNPChildren = 0;
   * <b>Sexp</b> whnpChild = <b>null</b>;
   * <b>foreach</b> child <b>of</b> tree <b>do</b>
   *   <b>if</b> child is a WHNP with an index augmentation <b>then</b>
   *     <b>if</b> numWHNPChildren == 0 <b>then</b>
   *       whnpChild = child;
   *     <b>endif</b>
   *     numWHNPChildren++;
   *   <b>endif</b>
   * <b>end</b>
   *
   * <b>if</b> numWHNPChildren &gt; 0 <b>then</b>
   *   push(index of whnpChild, indexStack);
   * <b>endif</b>
   *
   * <b>int</b> numTracesToBeLinked = 0, traceIndex = -1;
   * <b>foreach</b> child <b>of</b> tree <b>do</b>
   *   <b>int</b> gapIndex = hasGap(child, root, indexStack); <font color=red>// recursive call</font>
   *   <b>if</b> gapIndex != -1 <b>then</b>
   *     <b>if</b> numTracesToBeLinked == 0 <b>then</b>
   *       traceIndex = gapIndex;
   *     <b>endif</b>
   *     numTracesToBeLinked++;
   *   <b>endif</b>
   * <b>end</b>
   *
   * <b>if</b> numTracesToBeLinked &gt; 0 <b>then</b>
   *   add gap augmentation to the current parent (the root of <b>tree</b>);
   *   <b>if</b> numWHNPChildren &gt; 0 <b>and</b> index of whnpChild == traceIndex <b>then</b>
   *     <font color=red>// a trace from a child subtree has been hooked up with the current WHNP child</font>
   *     <b>return</b> -1;
   *   <b>else</b>
   *     <b>return</b> traceIndex;
   *   <b>endif</b>
   * <b>else</b>
   *   <b>if</b> numWHNPChildren &gt; 0 <b>then</b>
   *     <b>print</b> warning that a moved WHNP node doesn't have a coindexed trace
   *       in any of its parent's other child subtrees;
   *   <b>endif</b>
   *   <b>return</b> -1;
   * <b>endif</b>
   * </pre>
   * A warning will also be issued if there are crossing WHNP-trace
   * dependencies.
   * <p>
   * This method is called by the default implementation of {@link
   * #addGapInformation}.
   * <p>
   *
   * @param tree the tree to gapify
   * @param root always the root of the tree we're gapifying, for error and
   * warning reporting
   * @param indexStack a stack of <code>Integer</code> objects (where the top
   * of the stack is the highest-indexed object), representing the pending
   * requests to find traces to match with coindexed WHNP's discovered higher
   * up in the tree (earlier in the DFS)
   * @return -1 if <code>tree</code> has no gap, or the index of the trace
   * otherwise
   *
   * @see #gapAugmentation
   * @see #traceTag
   * @see #addGapInformation(Sexp)
   * @see Treebank#isWHNP Treebank.isWHNP(Symbol)
   * @see Treebank#isNullElementPreterminal(Sexp)
   * @see Treebank#getTraceIndex(Sexp, Nonterminal)
   */
  protected int hasGap(Sexp tree, Sexp root, ArrayList indexStack) {
    // System.out.println(tree);

    if (tree.isSymbol())
      return -1;
    if (treebank.isPreterminal(tree)) {
      if (treebank.isNullElementPreterminal(tree) &&
	  indexStack.size() > 0) {
	int traceIdx = treebank.getTraceIndex(tree, nonterminal);
	Integer indexFound = new Integer(traceIdx);
	Integer lookingFor = (Integer)indexStack.get(indexStack.size() - 1);
	if (traceIdx != -1) {
	  if (indexFound.equals(lookingFor)) {
	    indexStack.remove(indexStack.size() - 1);
	    tree.list().set(0, traceTag);
	    tree.list().set(1, traceTag); // set word to be trace as well
	    return indexFound.intValue();
	  }
	  else if (indexStack.contains(indexFound)) {
	    System.err.println(className + ": warning: crossing " +
			       "WH-movement for tree\n\t" + root);
	  }
	}
      }
      // if either a non-null element preterminal, or the indexStack was empty,
      // or we found a trace whose index didn't match what we were looking for
      return -1;
    }
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol parent = treeList.get(0).symbol();

      int numWHNPChildren = 0;
      int whnpIdx = -1;
      for (int i = 1; i < treeListLen; i++) {
	Symbol child = treeList.getChildLabel(i);
	Sexp firstChildOfChild = treeList.get(i).list().get(1);
	if (treebank.isWHNP(child) &&
	    !treebank.isNullElementPreterminal(firstChildOfChild)) {
	  Nonterminal parsedChild =
	    treebank.parseNonterminal(child, nonterminal);
	  if (parsedChild.index != -1) {
	    if (numWHNPChildren == 0)
	      whnpIdx = parsedChild.index; // only grab leftmost WHNP index
	    numWHNPChildren++;
	  }
	}
      }

      if (numWHNPChildren > 1)
	System.err.println(className + ": warning: multiple WHNP nodes have " +
			   "moved to become children of the same parent (" +
			   parent + ") for tree\n\t" + root);

      if (numWHNPChildren > 0)
	indexStack.add(new Integer(whnpIdx));

      int numTracesToBeLinked = 0;
      int traceIdx = -1, currGapIdx = -1;

      // recursive calls to all children
      for (int i = 1; i < treeListLen; i++) {
	currGapIdx = hasGap(treeList.get(i), root, indexStack);
	if (currGapIdx != -1) {
	  if (numTracesToBeLinked == 0)
	    traceIdx = currGapIdx;
	  numTracesToBeLinked++;
	}
      }

      // don't need to issue warning if numTracesToBeLinked > 1, since
      // we check for crossing WHNP movement in base case above

      if (numTracesToBeLinked > 0) {
	Nonterminal parsedParent =
	  treebank.parseNonterminal(parent, nonterminal);
	treebank.addAugmentation(parsedParent, gapAugmentation);
	treeList.set(0, parsedParent.toSymbol());

	if (numWHNPChildren > 0 && whnpIdx == traceIdx)
	  return -1;
	else
	  return traceIdx;
      }
      else {
	if (numWHNPChildren > 0)
	  System.err.println(className + ": warning: a moved WHNP node with " +
			     "parent " + parent + " and index " + whnpIdx +
			     " doesn't have co-indexed trace in any of its "+
			     "parent's other children in tree\n\t" + root);
	return -1;
      }
    }
    return -1; // should never reach this point
  }

  /**
   * Returns <code>true</code> if and only if <code>label</code> has a
   * gap augmentation as added by {@link #addGapInformation(Sexp)}.
   */
  public boolean hasGap(Symbol label) {
    Nonterminal parsedLabel = treebank.parseNonterminal(label, nonterminal);
    return parsedLabel.augmentations.contains(gapAugmentation);
  }

  /**
   * The symbol that will be used to identify nonterminals whose subtrees
   * contain a gap (a trace).  This method is used by {@link
   * #stripAugmentations(Sexp)}, so that gap augmentations that are added by
   * {@link #addGapInformation(Sexp)} do not get removed.  The default value is
   * the symbol returned by <code>Symbol.add(&quot;g&quot;)</code>.  If this
   * default value conflicts with an augmentation already used in a particular
   * Treebank, the value of the data member {@link #gapAugmentation} should be
   * reassigned in the constructor of a subclass.
   */
  public Symbol gapAugmentation() { return gapAugmentation; }

  /**
   * The symbol that gets reassigned as the part of speech for null
   * preterminals that represent traces that have undergone WH-movement, as
   * relabeled by the default implementation of {@link
   * #addGapInformation(Sexp)}.  The default value is the return value of
   * <code>Symbol.add(&quot;*TRACE*&quot;)</code>.  If this maps to an actual
   * part of speech tag or nonterminal label in a particular Treebank, the
   * data member {@link #traceTag} should be reassigned in the constructor
   * of a subclass.
   */
  public Symbol traceTag() { return traceTag; }

  /**
   * Relabels sentences that have no subjects with the nonterminal label
   * returned by {@link Treebank#subjectlessSentenceLabel}.  This method is
   * optional, and may be overridden to simply return <code>tree</code>
   * untouched if subjectless sentence relabeling is not desired for a
   * particular language package.
   * <p>
   * The default implementation here assumes that a subjectless sentence is a
   * node for which {@link Treebank#isSentence(Symbol)} returns
   * <code>true</code> and has a child with an augmentation for which {@link
   * Treebank#subjectAugmentation} returns <code>true</code>, and that this
   * child represents a subtree that is a series of unary productions, ending in
   * a subtree for which {@link Treebank#isNullElementPreterminal(Sexp)}
   * returns <code>true</code>.  Informally, this method looks for sentence
   * nodes that have a child marked as a subject, where that child has a null
   * element as its first (and presumably only) child.  For example, in the
   * English Treebank, this would mean one of the following contexts:
   * <pre>
   * (S (PREMOD ...) (NP-SBJ (-NONE- *T*)) ... )
   * </pre>
   * or
   * <pre>
   * (S (PREMOD ...) (NP-SBJ (NPB (-NONE- *T*))) ... )
   * </pre>
   * where <tt>(PREMOD ...)</tt> represents zero or more premodifying phrases
   * and where <tt>NPB</tt> represents a node inserted by a method such as
   * {@link #addBaseNPs(Sexp)}.  Note that the subtree rooted by <tt>NPB</tt>
   * satisfies the condition of being a subtree that is the result of a
   * series of unary productions (one of them, in this case) ending
   * in a null element preterminal.  (This seemingly over-complicated condition
   * is necessary for this method to run properly after <code>tree</code>
   * has been processed by {@link #addBaseNPs(Sexp)}.)
   * <p>
   * If a subclass of this class in a language package requires more
   * extensive or different checking for the &quot;subjectlessness&quot; of a
   * sentence, this method should be overridden.
   * <p>
   *
   * @param tree the parse tree in which to relabel subjectless sentences
   * @return the same <code>tree</code> that was passed in, with
   * subjectless sentence nodes relabeled
   * @see Treebank#isSentence(Symbol)
   * @see Treebank#subjectAugmentation
   * @see Treebank#isNullElementPreterminal(Sexp)
   * @see Treebank#subjectlessSentenceLabel
   */
  public Sexp relabelSubjectlessSentences(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol parent = treeList.symbolAt(0);
      if (treebank.isSentence(parent)) {
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
      for (int i = 1; i < treeList.length(); i++)
	relabelSubjectlessSentences(treeList.get(i));
    }
    return tree;
  }

  private final boolean unaryProductionsToNull(Sexp tree) {
    if (treebank.isNullElementPreterminal(tree))
      return true;
    else if (tree.isList()) {
      // walk down tree: as soon as a node has more than one child (i.e.,
      // is a list of length > 2) return false; otherwise, continue walking
      // until we hit a preterminal, at which point exit loop and test
      // if null element
      Sexp curr = tree;
      for ( ; !(treebank.isPreterminal(curr)); curr = curr.list().get(1))
	if (curr.list().length() != 2)
	  return false;
      return treebank.isNullElementPreterminal(curr);
    }
    else
      return false;
  }

  /**
   * Strips any augmentations off all of the nonterminal labels of
   * <code>tree</code>.  The set of nonterminal labels does <i>not</i> include
   * preterminals, which are typically parts of speech.  If a particular
   * language's Treebank augments preterminals, this method should be
   * overridden in a language package's subclass. The only augmentations that
   * will not be removed are those that are added by {@link
   * #identifyArguments(Sexp)}, so as to preserve the transformations of that
   * method.  This method should only be called subsequent to the invocations
   * of methods that require augmentations, such as {@link
   * #relabelSubjectlessSentences(Sexp)}.
   *
   * @param tree the tree all of the nonterminals of which are to be stripped
   * of all augmentations except those added by <code>identifyArguments</code>
   * @return a reference to <code>tree</code>
   */
  public Sexp stripAugmentations(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (Language.treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      Symbol label = tree.list().first().symbol();
      Language.treebank.parseNonterminal(label, nonterminal);
      boolean isArg = nonterminal.augmentations.contains(argAugmentation);
      boolean hasGap = nonterminal.augmentations.contains(gapAugmentation);
      nonterminal.augmentations.clear();
      if (isArg)
	treebank.addAugmentation(nonterminal, argAugmentation);
      if (hasGap)
	treebank.addAugmentation(nonterminal, gapAugmentation);
      nonterminal.index = -1; // effectively strips off index
      tree.list().set(0, nonterminal.toSymbol());
      int treeListLen = tree.list().length();
      for (int i = 1; i < treeListLen; i++)
	stripAugmentations(tree.list().get(i));
    }
    return tree;
  }

  /**
   * Raises punctuation to the highest possible point in a parse tree,
   * resulting in a tree where no punctuation is the first or last child of a
   * non-leaf node.  One consequence is that all punctuation is removed from
   * the beginning and end of the sentence.  The punctuation affected is
   * defined by the implementation of the method {@link
   * Treebank#isPuncToRaise(Sexp)}.
   * <p>
   * <b>Side effect</b>: All preterminals removed from the beginning and end
   * of the sentence are stored in an internal set, which can be accessed
   * via {@link #getPrunedPunctuation()}.
   * <p>
   * Example of punctuation raising:
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old)
   *      (, ,))
   *    (VP joined (NP (NPB the board))) (. .))
   * </pre>
   * becomes
   * <pre>
   * (S (NP
   *      (NPB Pierre Vinken)
   *      (, ,)
   *      (ADJP 61 years old))
   *    (, ,)
   *    (VP joined (NP (NPB the board))))
   * </pre>
   * This method appropriately deals with the case of having multiple
   * punctuation elements to be raised on the left or right side of the list of
   * children for a nonterminal.  For example, in English, if this method
   * were passed the tree
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky) (. .) (. .) (. .))
   *   (VP (VB was) (ADJP (JJ stupid)))
   *   (. .) (. .) (. .))
   * </pre>
   * the result would be
   * <pre>
   * (S
   *   (NP (DT The) (NN dog) (, ,) (NNP Barky))
   *   (. .) (. .) (. .)
   *   (VP (VB was) (ADJP (JJ stupid))))
   * </pre>
   * <p>
   * <b>Bugs</b>: In the pathological case where all the children of a node
   * are punctuation to raise, this method simply emits a warning to
   * <code>System.err</code> and does not attempt to raise them (which would
   * cause an interior node to become a leaf).
   * <p>
   * @param tree the parse tree to destructively modify by raising punctuation
   * @return a reference to the modified <code>tree</code> object
   */
  public Sexp raisePunctuation(Sexp tree) {
    /*
    leftRaise.clear();
    rightRaise.clear();
    raisePunctuation(tree, leftRaise, rightRaise);
    */
    raise.clear();
    raisePunctuation(tree, raise, Constants.LEFT);
    for (int i = 0; i < raise.length(); i++)
      prunedPunctuation.add(raise.get(i));
    raise.clear();
    raisePunctuation(tree, raise, Constants.RIGHT);
    for (int i = 0; i < raise.length(); i++)
      prunedPunctuation.add(raise.get(i));
    return tree;
  }

  private void raisePunctuation(Sexp tree, SexpList raise, boolean direction) {
    if (tree.isSymbol() || Language.treebank.isPreterminal(tree))
      return;
    if (allChildrenPuncToRaise(tree)) {
      System.err.println(Training.class.getName() +
			 ": warning: all children are punctuation to raise\n" +
			 "\t" + tree);
      return;
    }

    // if tree is a list with at least two children (i.e., of length 3)
    if (tree.isList()) {
      SexpList treeList = tree.list();

      boolean leftToRight = direction == Constants.LEFT;
      int startIdx = (leftToRight ? 1 : treeList.length() - 1);
      int endIdx = (leftToRight ? treeList.length() - 1 : 1);
      int increment = (leftToRight ? 1 : -1);
      for (int i = startIdx;
	   (leftToRight && i <= endIdx) || (!leftToRight && i >= endIdx);
	   i += increment) {
	// we're occasionally removing items from tree, so recalculate these
	startIdx = (leftToRight ? 1 : treeList.length() - 1);
	endIdx = (leftToRight ? treeList.length() - 1 : 1);
	Sexp currChild = treeList.get(i);

	raisePunctuation(currChild, raise, direction);

	// if it's the last child that we're visiting
	if (i == endIdx) {
	  // while there's ending punctuation, remove it and add it
	  // to the raise queue (in queue order, hence the call to
	  // reverse, since we're grabbing nodes from the outside in)
	  addToRaise.clear();
	  while (treeList.length() > 1 &&
		 Language.treebank.isPuncToRaise(treeList.get(endIdx))) {
	    addToRaise.add(treeList.remove(endIdx));
	    endIdx = (leftToRight ? treeList.length() - 1 : 1);
	  }
	  // set i to the new last element index (not strictly necessary)
	  if (raise.addAll(addToRaise.reverse()))
	    i = endIdx;
	}
	// if it's not the last child we're visiting at this level and
	// there are raise requests enqueued, we can oblige by
	// inserting the punctuation just after the current node
	else if (raise.length() != 0) {
	  // since items naturally shift rightward when we use the add method,
	  // to add "after" current node in a right-to-left traversal requires
	  // keeping add index the same (i.e., an increment of zero)
	  int insertIncrement = (leftToRight ? increment : 0);
	  i += insertIncrement;
	  for (int raiseIdx = 0; raiseIdx < raise.length(); raiseIdx++) {
	    treeList.add(i, raise.get(raiseIdx));
	    if (leftToRight)
	      i += insertIncrement;
	  }
          endIdx = (leftToRight ? treeList.length() - 1 : 1);
	  i -= insertIncrement; // offset increment for enclosing for loop
	  raise.clear();
	}
      }
    }
  }

  private static boolean allChildrenPuncToRaise(Sexp tree) {
    int treeLen = tree.list().length();
    for (int i = 1; i < treeLen; i++)
      if (Language.treebank.isPuncToRaise(tree.list().get(i)) == false)
	return false;
    return true;
  }

  /**
   * Returns the set of preterminals (<code>Sexp</code> objects) that were
   * punctuation elements that were "raised away" because they were either at
   * the beginning or end of a sentence.
   *
   * @see #raisePunctuation(Sexp)
   */
  public Set getPrunedPunctuation() { return prunedPunctuation; }

  /**
   * Adds and/or relabels base NPs, which are defined in this default
   * implementation to be NPs that do not dominate other non-possessive NPs,
   * where a possessive NP is defined to be an NP that itself dominates
   * a possessive preterminal, as determined by the implementation of the
   * method {@link Treebank#isPossessivePreterminal(Sexp)}.  If an NP
   * is relabeled as a base NP but is not dominated by another NP, then
   * a new NP is interposed, for the sake of consistency.  For example,
   * if the specified tree is the English Treebank tree
   * <pre>
   * (S (NP-SBJ (DT The) (NN dog)) (VP (VBD sat)))
   * </pre>
   * then this method will transform it to be
   * <pre>
   * (S (NP-SBJ (NPB (DT The) (NN dog))) (VP (VBD sat)))
   * </pre>
   * Note that the <tt>SBJ</tt> augmentation is transferred to the
   * enclosing NP.
   *
   * @param tree the parse tree in which to add and/or relabel base NPs
   * @return a reference to the modified version of <code>tree</code>
   *
   * @see #hasPossessiveChild(Sexp)
   * @see Treebank#isNP(Symbol)
   * @see Treebank#baseNPLabel
   * @see Treebank#NPLabel
   */
  public Sexp addBaseNPs(Sexp tree) {
    return addBaseNPs(null, -1, tree);
  }

  private Sexp addBaseNPs(Sexp grandparent, int parentIdx, Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      if (treebank.isNP(tree.list().first().symbol())) {
	Symbol parent = treeList.first().symbol();
	boolean parentIsBaseNP = true;
	for (int i = 1; i < treeListLen; i++) {
	  // if a child is an NP that is NOT a possessive NP (i.e., the child
	  // does not itself have a child that is a possessive preterminal)
	  // then parent is NOT a baseNP
	  if (treebank.isNP(treeList.getChildLabel(i)) &&
	      hasPossessiveChild(treeList.get(i)) == false) {
	    parentIsBaseNP = false;
	    break;
	  }
	}
	if (parentIsBaseNP) {
	  Nonterminal parsedParent =
	    treebank.parseNonterminal(parent, nonterminal);
	  if (parsedParent.augmentations.length() == 0 &&
	      parsedParent.index == -1)
	    treeList.set(0, baseNP);
	  else {
	    parsedParent.base = baseNP;
	    treeList.set(0, parsedParent.toSymbol());
	  }
	  // if the grandparent is not an NP, we need to add a normal NP level
	  // transferring any augmentations to the new parent from the current
	  if (needToAddNormalNPLevel(grandparent, parentIdx, tree)) {
	    if (grandparent != null) {
	      SexpList newParent = new SexpList(2);
	      treeList.set(0, baseNP);
	      parsedParent.base = NP;
	      newParent.add(parsedParent.toSymbol()).add(tree);
	      grandparent.list().set(parentIdx, newParent);
	    }
	    else {
	      // make parent back into NP
	      parsedParent.base = NP;
	      treeList.set(0, parsedParent.toSymbol());
	      // now, take all parent's children and add them as
	      // children of a new node that will be the new base NP
	      SexpList baseNPNode = new SexpList(treeListLen);
	      baseNPNode.add(baseNP);
	      for (int j = 1; j < treeListLen; j++)
		baseNPNode.add(treeList.get(j));
	      for (int j = treeListLen - 1; j >= 1; j--)
		treeList.remove(j);
	      // finally, add baseNPNode as sole child of current parent
	      treeList.add(baseNPNode);
	    }
	  }
	}
      }
      for (int i = 1; i < treeListLen; i++)
	addBaseNPs(tree, i, treeList.get(i));
    }
    return tree;
  }

  /**
   * Returns <code>true</code> if a unary NP needs to be added above the
   * specified base NP.
   *
   * @param grandparent the parent of the &quot;parent&quot; that is a
   * base NP
   * @param parentIdx the index of the child of <code>grandparent</code>
   * that is the base NP (that is,
   * <pre>grandparent.list().get(parentIdx) == tree</pre>
   * @param tree the base NP, whose parent is <code>grandparent</code>
   */
  protected boolean needToAddNormalNPLevel(Sexp grandparent,
                                           int parentIdx, Sexp tree) {
    if (grandparent == null)
      return true;

    SexpList grandparentList = grandparent.list();
    if (!treebank.isNP(grandparentList.symbolAt(0)))
      return true;
    int headIdx = headFinder.findHead(grandparent);
    return (isCoordinatedPhrase(grandparent, headIdx) ||
	    (headIdx != parentIdx && grandparentList.symbolAt(0) != baseNP));
  }

  /**
   * Returns <code>true</code> if a non-head child of the specified
   * tree is a conjunction, and that conjunction is either post-head
   * but non-final, or immediately pre-head but non-initial (where
   * &quot;immediately pre-head&quot; means &quot;at the first index
   * less than <code>headIdx</code> that is not punctuation, as determined
   * by {@link Treebank#isPunctuation(Symbol)}).  A child is a
   * conjunction if its label is one for which
   * {@link Treebank#isConjunction(Symbol)} returns <code>true</code>.
   *
   * @param tree the (sub)tree to test
   * @param headIdx the index of the head child of the specified tree */
  protected boolean isCoordinatedPhrase(Sexp tree, int headIdx) {
    SexpList treeList = tree.list();
    int treeListLen = treeList.length();

    int conjIdx = -1;
    // first, search everything post-head except final child,
    // since conj must not be final for tree to be a true coordinated phrase
    int lastChildIdx = treeListLen - 1;
    for (int i = headIdx + 1; i < lastChildIdx; i++) {
      if (treebank.isConjunction(treeList.getChildLabel(i))) {
	conjIdx = i;
	break;
      }
    }
    // if first search didn't succeed, search immediately pre-head
    if (conjIdx == -1) {
      int i = headIdx - 1;
      // skip all punctuation immediately preceding head
      while (i > 1 && treebank.isPunctuation(treeList.getChildLabel(i)))
	i--;
      // conj must not be initial for tree to be a coordinated phrase
      if (i > 1 && treebank.isConjunction(treeList.getChildLabel(i)))
	conjIdx = i;
    }

    return conjIdx != -1;	    
  }


  /**
   * Returns <code>true</code> if <code>tree</code> contains a child for which
   * {@link Treebank#isPossessivePreterminal(Sexp)} returns
   * <code>true</code>, <code>false</code> otherwise.  This is a helper method
   * used by the default implementation of {@link #addBaseNPs(Sexp)}.
   * Possessive children are often more even-tempered than possessive parents.
   *
   * @param tree the parse subtree to check for possessive preterminal
   * children
   */
  protected boolean hasPossessiveChild(Sexp tree) {
    if (tree.isSymbol())
      return false;
    if (treebank.isPreterminal(tree))
      return false;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	if (treebank.isPossessivePreterminal(treeList.get(i)))
	  return true;
    }
    return false;
  }

  /**
   * Removes all null elements, that is, those nodes of <code>tree</code> for
   * which {@link Treebank#isNullElementPreterminal(Sexp)} returns
   * <code>true</code>.  Additionally, if the removal of a null element leaves
   * an interior node that is childless, then this interior node is removed as
   * well.  For example, if we have the following sentence in English
   * <pre> (S (NP-SBJ (-NONE- *T*)) (VP ...)) </pre>
   * it will be transformed to be
   * <pre> (S (VP ...)) </pre>
   * <b>N.B.</b>: This method should only be invoked <i>after</i> preprocessing
   * with {@link #relabelSubjectlessSentences(Sexp)} and {@link
   * #addGapInformation(Sexp)}, as these methods (and possibly others, if
   * overridden) rely on the presence of null elements.
   *
   * @see Treebank#isNullElementPreterminal(Sexp)
   */
  public static Sexp removeNullElements(Sexp tree) {
    if (tree.isSymbol())
      return tree;
    if (Language.treebank.isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 0; i < treeList.length(); i++) {
	if (Language.treebank.isNullElementPreterminal(treeList.get(i))) {
	  treeList.remove(i--); // postdecrement i to offset for loop increment
	}
	else {
	  removeNullElements(treeList.get(i));
	  // if removing null preterminals from the current child yields
	  // a childless child, then remove the current child
	  if (treeList.get(i).isList() &&
	      treeList.get(i).list().length() == 1)
	    treeList.remove(i--);
	}
      }
    }
    return tree;
  }

  // some data accessor methods

  /**
   * Returns the symbol to indicate hidden nonterminals that precede the first
   * in a sequence of modifier nonterminals.  The default value is the return
   * value of <code>Symbol.add(&quot;+START+&quot;)</code>; if this value
   * conflicts with an actual nonterminal in a particular Treebank, then this
   * method should be overridden.
   *
   * @see Trainer
   */
  public Symbol startSym() { return startSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the start symbol.
   *
   * @see #startSym
   * @see Trainer
   */
  public Word startWord() { return startWord; }
  /**
   * Returns the symbol to indicate a hidden nonterminal that follows the last
   * in a sequence of modifier nonterminals.  The default value is the return
   * value of <code>Symbol.add(&quot;+STOP+&quot;)</code>; if this value
   * conflicts with an actual nonterminal in a particular Treebank, then this
   * method should be overridden.
   * <p>
   * This symbol may also be used as a special value that is guaranteed not
   * to conflict with any nonterminal in a given language's treebank.
   * <p>
   *
   * @see Trainer
   */
  public Symbol stopSym() { return stopSym; }
  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the stop symbol.
   *
   * @see #stopSym
   * @see Trainer
   */
  public Word stopWord() { return stopWord; }
  /**
   * Returns the symbol to indicate the hidden root of all parse trees.  The
   * default value is the return value of
   * <code>Symbol.add(&quot;+TOP+&quot;)</code>; if this value conflicts with
   * an actual nonterminal in a particular Treebank, then this method should be
   * overridden.
   *
   * @see Trainer
   */
  public Symbol topSym() { return topSym; }

  /**
   * Returns the <code>Word</code> object that represents the hidden "head
   * word" of the hidden root of all parse trees.
   */
  public Word topWord() { return topWord; }


  /**
   * Returns the symbol used in the {@link #argContexts} map to identify
   * an offset from the head child.
   */
  public Symbol headSym() { return headSym; }

  public Symbol headPreSym() { return headPreSym; }

  public Symbol headPostSym() { return headPostSym; }

  /**
   * Returns an unmodifiable view of the {@link #argContexts} map.
   */
  public Map argContexts() { return Collections.unmodifiableMap(argContexts); }

  // a couple of utility methods for removing gap augmentations very efficiently

  /**
   * If the specified S-expression is a list, this method modifies the
   * list to contain only symbols without gap augmentations;
   * otherwise, this method removes the gap augmentation (if one exists)
   * in the specified symbol and returns that new symbol.  Note that
   * the presence of gap augmentations is determined by matching for
   * {@link #delimAndGapStr}, which means that symbols consisting solely
   * of the gap augmentation itself ({@link #gapAugmentation}) will
   * be unaffected.
   *
   * @param sexp a symbol or list of symbols from which to remvoe any
   * gap augmentations
   * @return a symbol or list of symbols with no gap augmentations
   */
  public Sexp removeGapAugmentation(Sexp sexp) {
    if (!addGapInfo)
      return sexp;
    if (sexp.isSymbol())
      return symRemoveGapAugmentation(sexp.symbol());
    else
      return listRemoveGapAugmentation(sexp.list());
  }

  private Symbol symRemoveGapAugmentation(Symbol label) {
    int gapAugIdx = label.toString().indexOf(delimAndGapStr);
    if (gapAugIdx != -1) {
      String labelStr = label.toString();
      /*
      StringBuffer sb = new StringBuffer(labelStr.length() + 1);
      sb.append(labelStr);
      sb.delete(gapAugIdx, gapAugIdx + delimAndGapStrLen);
      */
      StringBuffer sb =
	new StringBuffer(labelStr.length() - delimAndGapStrLen).
	  append(labelStr.substring(0, gapAugIdx)).
	  append(labelStr.substring(gapAugIdx + delimAndGapStrLen));
      return Symbol.add(sb.toString());
    }
    else
      return label;
  }

  private SexpList listRemoveGapAugmentation(SexpList list) {
    int listLen = list.length();
    for (int i = 0; i < listLen; i++)
      list.set(i, symRemoveGapAugmentation(list.symbolAt(i)));
    return list;
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: <filename>");
      System.exit(1);
    }
    String filename = args[0];
    try {
      SexpTokenizer tok = new SexpTokenizer(filename, Language.encoding(),
					    Constants.defaultFileBufsize);
      Training training = (Training)Language.training();
      Sexp curr = null;
      for (int treeNum = 1; (curr = Sexp.read(tok)) != null; treeNum++) {
	if (curr.isList()) {
          // automatically determine whethe to strip outer parens
	  Sexp tree = curr.list().get(0);
          if (tree.isSymbol())
            tree = curr;
	  if (training.isValidTree(tree))
	    System.out.println(training.preProcess(tree));
	  else
	    System.err.println("tree No. " + treeNum + " invalid: " + tree);
	}
	else
	  System.err.println("S-expression No. " + treeNum + ": not list: " +
			     curr);
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
