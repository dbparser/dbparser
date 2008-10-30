package danbikel.switchboard;

import java.rmi.*;

/**
 * An exception raised when a switchboard method with a client ID parameter
 * is called with an invalid client ID.
 *
 * @see SwitchboardRemote
 * @see Switchboard
 */
public class UnrecognizedClientException extends RemoteException {
  /** Constructs a new <code>UnrecognizedClientException</code>. */
  public UnrecognizedClientException() { super(); }
  /** Constructs a new <code>UnrecognizedClientException</code> with the
      specified message. */
  public UnrecognizedClientException(String msg) { super(msg); }
  /** Constructs a new <code>UnrecognizedClientException</code> with the
      specified message and nested exception. */
  public UnrecognizedClientException(String msg, Throwable ex) {
    super(msg, ex);
  }
}
