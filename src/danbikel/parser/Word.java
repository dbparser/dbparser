package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A Word object is a structured representation of a word.  It includes:
 * <ul>
 * <li> the word itself
 * <li> the word's part of speech
 * <li> an optional representation of the word's features
 * </ul>
 */
public class Word implements Serializable, Cloneable, SexpConvertible {

  // constants
  private final static String className = Word.class.getName();

  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  protected Symbol word;
  protected Symbol tag;
  protected Symbol features;

  /**
   * Creates a new Word object with the specified word and part of speech.
   *
   * @param word the word itself (all lowercase).
   * @param tag its part-of-speech tag.
   */
  public Word(Symbol word, Symbol tag) {
    this(word, tag, null);
  }

  public Word(Symbol word, Symbol tag, Symbol features) {
    this.word = word;
    this.tag = tag;
    this.features = features;
  }

  public Word(Sexp s) {
    if (s.isList() == false)
      throw new IllegalArgumentException(className +
					 ": S-expression passed to " +
					 "constructor is not a list");
    SexpList sexp = s.list();
    int sexpLen = sexp.length();
    if (!(sexpLen >= 2 && sexpLen <= 3))
      throw new IllegalArgumentException(className +
					 ": illegal Sexp length: " + sexpLen);

    if (!sexp.isAllSymbols())
	throw new IllegalArgumentException(className +
					   ": non-Symbol element to Sexp");

    word = sexp.symbolAt(0);
    tag = sexp.symbolAt(1);
    features = (sexpLen == 3 ? sexp.symbolAt(2) : null);
  }

  /**
   * Returns the word itself of this <code>Word</code> object.
   */
  public Symbol word() { return word; }

  /**
   * Sets the word itself of this <code>Word</code> object.
   *
   * @param word the word itself
   */
  public void setWord(Symbol word) { this.word = word; }

  /**
   * Returns the part-of-speech tag of this word.
   */
  public Symbol tag() { return tag; }

  /**
   * Sets the part-of-speech tag for this word.
   *
   * @param tag the part-of-speech tag
   */
  public void setTag(Symbol tag) { this.tag = tag; }

  /**
   * Returns the features of this word, or <code>null</code> if no features
   * have been set for this word.
   */
  public Symbol features() { return features; }

  /**
   * Sets the features for this word.
   */
  public void setFeatures(Symbol features) { this.features = features; }

  /**
   * Sets all three data members for this word.
   *
   * @return this Word object
   */
  public Word set(Symbol word, Symbol tag, Symbol features) {
    setWord(word);
    setTag(tag);
    setFeatures(features);
    return this;
  }

  /**
   * Returns a hash value for this object.
   *
   * @return the hash value for this object.
   */
  public int hashCode() {
    int code = word.hashCode();
    code = (code << 2) ^ tag.hashCode();
    return code;
  }

  /**
   * Determines whether two Word objects are equal.
   *
   * @param obj2 the Word object to compare with.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj instanceof Word) {
      Word other = (Word)obj;
      return (word == other.word &&
	      tag == other.tag &&
	      features == other.features);
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
    if (features != null)
      b.append(" ").append(features);
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
    return new SexpList(2).add(word()).add(tag());
  }
}
