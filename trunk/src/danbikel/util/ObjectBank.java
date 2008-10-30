package danbikel.util;

import java.util.*;

/**
 * A generic object bank: the bank monotonically grows until all of its
 * objects are reclaimed.
 */

public class ObjectBank {
  // data members
  private Class type;
  private Object[] bank;
  private int head;
  private int size;


  public ObjectBank(Class type) {
    this(type, 0);
  }
  public ObjectBank(Class type, int initialSize) {
    if (initialSize < 0)
      throw new IllegalArgumentException();
    this.type = type;
    bank = new Object[initialSize];
    for (int i = 0; i < bank.length; i++)
      bank[i] = newInstance();
    size = initialSize;
    head = 0;
  }

  public Object get() {
    if (head < size)
      return bank[head++];
    else {
      ensureCapacity(++size);
      Object newObj = newInstance();
      bank[head++] = newObj;
      return newObj;
    }
  }

  public void reclaimAll() {
    head = 0;
  }

  private void ensureCapacity(int newSize) {
    if (bank.length >= newSize)
      return;
    int newArrSize = bank.length * 2;
    if (newArrSize < newSize)
      newArrSize = newSize;
    Object[] newArr = new Object[newArrSize];
    System.arraycopy(bank, 0, newArr, 0, bank.length);
    bank = newArr;
  }

  private final Object newInstance() {
    try {
      return type.newInstance();
    }
    catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public int size() { return size; }
  public int numInUse() { return head; }
  public int capacity() { return bank.length; }
}