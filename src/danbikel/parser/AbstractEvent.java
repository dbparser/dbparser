package danbikel.parser;

/**
 * A convenience class that simply implements the <code>equals</code>
 * method, as specified by the contract in {@link
 * Event#equals(Object)}.  For efficiency, subclasses are encouraged
 * to override this method, using its result only when the two objects
 * being compared are both instances of <code>Event</code> but are not
 * of identical run-time types (this is the strategy employed by
 * <code>SexpEvent</code>).
 */
abstract public class AbstractEvent implements Event {
  AbstractEvent() { }

  public boolean genericEquals(Object obj) {
    if (!(obj instanceof Event))
      return false;
    // check same number of types and same total number of components
    Event otherEvent = (Event)obj;
    if (otherEvent.numTypes() != numTypes() ||
	otherEvent.numComponents() != numComponents())
      return false;

    //System.err.println("warning: we're deep in genericEquals!");

    // check that every type of this object is supported by other object
    // (and hence, since they have the same number of types, the converse
    // will be true)
    int numTypes = numTypes();
    for (int i = 0; i < numTypes; i++)
      if (otherEvent.typeIndex(getClass(i)) == -1)
	return false;

    // they share the same types, so check each list
    for (int typeIdx = 0; typeIdx < numTypes; typeIdx++) {
      int otherTypeIdx = otherEvent.typeIndex(getClass(typeIdx));
      // first, check length of same-typed lists
      if (numComponents(typeIdx) != otherEvent.numComponents(otherTypeIdx))
	return false;
      // now, check components of our equal-length lists of the current type:
      // lists must be pairwise equal
      int numComponents = numComponents(typeIdx);
      for (int componentIdx = 0; componentIdx < numComponents; componentIdx++) {
	Object objFromThis = this.get(typeIdx, componentIdx);
	Object objFromOther = otherEvent.get(otherTypeIdx, componentIdx);
	try {
	  if (!objFromThis.equals(objFromOther))
	    return false;
	}
	catch (ClassCastException cce) {
	  return false;
	}
      }
    }
    return true;
  }
}
