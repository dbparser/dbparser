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
  protected Symbol word;
  protected Symbol tag;

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
    if (sexp.length() != 2)
      throw new IllegalArgumentException(className +
					 ": illegal Sexp length: " +
					 sexp.length());

    if (sexp.isAllSymbols() == false)
	throw new IllegalArgumentException(className +
					   ": non-Symbol element to Sexp");

    word = sexp.get(0).symbol();
    tag = sexp.get(1).symbol();
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
      return word == other.word && tag == other.tag;
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
    return new SexpList(2).add(tag()).add(word());
  }
}
