package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A Word object is a structured representation of a word.  It includes:
 * <ul>
 * <li> the word itself
 * <li> the word's part of speech
 * <li> the synset information for the word
 *   <ul>
 *   <li> the WordNet lemma
 *   <li> the WordNet part of speech, one of {noun, verb, adjective, adverb}
 *   <li> the synset integer
 *   </ul>
 * </ul>
 */
public class Word implements Serializable, Cloneable, SexpConvertible {

  // constants
  private final static String className = Word.class.getName();

  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  private Symbol word;
  private Symbol tag;
  private Symbol lemma;
  private Symbol wnPos;
  private Symbol synset;
  private int index = -1;
  /** A private handle onto the original word if the stored word gets
      modified (e.g., upcased). */
  private Symbol originalWord;


  /**
   * Creates a new Word object with the specified word and part of speech
   * and WordNet information.
   *
   * @param word the word itself (all lowercase)
   * @param tag its part-of-speech tag
   * @param lemma the WordNet lemma of this word
   * @param wnPos the WordNet part of speech of this word
   * @param synset the synset of this word
   */
  public Word(Symbol word, Symbol tag,
	      Symbol lemma, Symbol wnPos, Symbol synset) {
    this(word, tag);
    this.wnPos = wnPos;
    this.lemma = lemma;
    this.synset = synset;
  }

  /**
   * Creates a new Word object with the specified word and part of speech,
   * with all WordNet fields set to <code>null</code>.
   *
   * @param word the word itself (all lowercase).
   * @param tag its part-of-speech tag.
   */
  public Word(Symbol word, Symbol tag) {
    this.word = word;
    this.tag = tag;
  }

  public Word(Sexp s) {
    if (s.isList() == false)
      throw new IllegalArgumentException(className +
					 ": S-expression passed to " +
					 "constructor is not a list");
    SexpList sexp = s.list();
    if (sexp.length() != 2 && sexp.length() != 3 &&
	sexp.length() != 5 && sexp.length() != 6)
      throw new IllegalArgumentException(className +
					 ": illegal Sexp length: " +
					 sexp.length());
    for (int i = 0; i < sexp.length(); i++)
      if (sexp.get(i).isSymbol() == false)
	throw new IllegalArgumentException(className +
					   ": non-Symbol element to Sexp");
    word = sexp.get(0).symbol();
    tag = sexp.get(1).symbol();

    if (sexp.length() > 3) {
      lemma = sexp.get(2).symbol();
      wnPos = sexp.get(3).symbol();
      synset = sexp.get(4).symbol();
      if (synset instanceof StringSymbol) {
	try {
	  synset = Symbol.add(Integer.parseInt(synset.toString()));
	} catch (NumberFormatException nfe) { System.err.println(nfe); }
      }
    }

    if (sexp.length() == 3 || sexp.length() == 6) {
      try {
	index = Integer.parseInt(sexp.last().symbol().toString());
      }
      catch (NumberFormatException nfe) { System.err.println(nfe); }
    }
  }

  /**
   * Returns the word itself.
   * @return the word itself
   */
  public Symbol word() { return word; }

  /**
   * Sets the word field.
   *
   * @param word the word itself (lowercase)
   */
  public void setWord(Symbol word) { this.word = word; }

  /**
   * Returns the tag field.
   * @return the part-of-speech tag of this word
   */
  public Symbol tag() { return tag; }

  /**
   * Sets the tag field.
   *
   * @param tag the part-of-speech tag.
   */
  public void setTag(Symbol tag) { this.tag = tag; }

  /**
   * Returns the WordNet part of speech for this word.
   * @return the WordNet part of speech for this word
   */
  public Symbol wnPos() { return wnPos; }

  /**
   * Sets the WordNet part of speech for this word.
   * @param wnPos a WordNet part of speech
   */
  public void setWNpos(Symbol wnPos) { this.wnPos = wnPos; }

  /**
   * Returns the synset for this word.
   * @return the synset for this word
   */
  public Symbol synset() { return synset; }

  /**
   * Sets the synset for this word.
   *
   * @param synset the synset of this word in WordNet (should normally be
   * an instance of {@link IntSymbol})
   */
  public void setSynset(Symbol synset) { this.synset = synset; }


  /**
   * Returns the WordNet lemma for this word.
   * @return the WordNet lemma for this word
   */
  public Symbol lemma() { return lemma; }

  /**
   * Sets the WordNet lemma for this word.
   *
   * @param lemma the WordNet lemma of this word
   */
  public void setLemma(Symbol lemma) { this.lemma = lemma; }

  /**
   * Gets the 0-based index of this word in its original sentence.
   * @return 0-based index of this word in its original sentence, or -1
   * if the index was never set for this object
   */
  public int index() { return index; }
  /**
   * Sets the index of this word in its original sentence.
   *
   * @param index the index of this word in its original sentence
   */
  public void setIndex(int index) { this.index = index; }


  /**
   * Gets the value of the handle onto the original word, or <code>null</code>
   * if this handle was never set.
   */
  public Symbol originalWord() { return originalWord; }

  /**
   * Sets the value of the handle onto the original word.
   *
   * @param originalWord the original word
   */
  public void setOriginalWord(Symbol originalWord) {
    this.originalWord = originalWord;
  }

  /**
   * Returns a hash value for this object.
   *
   * @return the hash value for this object.
   */
  public int hashCode() {
    int code = word.hashCode();
    if (tag != null) {
      code = (code << 2) ^ tag.hashCode();
    }
    if (wnPos != null) {
      code = (code << 2) ^ wnPos.hashCode();
    }
    if (synset != null) {
      code = (code << 2) ^ synset.hashCode();
    }
    return code;
  }

  /**
   * Determines whether two Word objects are equal.
   *
   * @param obj2 the Word object to compare with.
   */
  public boolean equals(Object obj2) {
    if (obj2 instanceof Word) {
      Word w2 = (Word) obj2;
      return (word == w2.word) &&
	(tag == w2.tag) &&
	(wnPos == w2.wnPos) &&
	(synset == w2.synset);
    }
    return false;
  }

  /**
   * Converts this Word object to a string (in S-expression format).
   *
   * @return the string representation.
   */
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("(");
    b.append(word);
    b.append(" ");
    b.append(tag);

    if (wnPos != null) {
      b.append(" ").append(lemma).append(" ").append(wnPos);
      b.append(" ").append(synset);
    }
    if (index >= 0) {
      b.append(" ").append(index);
    }
    b.append(")");
    return b.toString();
  }

  public Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException cnse) {
      System.err.println(cnse);
      return null;
    }
  }

  /**
   * Returns a clone of this object.  A shallow copy is all that is needed,
   * as all data members are basic types or references to unique objects
   * (symbols).
   */
  public Word copy() {
    return (Word)this.clone();
  }

  public Sexp toSexp() {
    boolean wn = wnPos != null;
    int len = (wn ? 5 : 2);
    SexpList list = new SexpList(len);
    list.add(tag()).add(word());
    if (wn) {
      // change word to have underscore-delimited WN info
    }
    return list;
  }
}
