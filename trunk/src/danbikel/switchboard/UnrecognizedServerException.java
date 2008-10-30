package danbikel.switchboard;

import java.rmi.*;

/**
 * An exception raised when a switchboard method that has a server ID
 * parameter is called with an invalid server ID.  This exception
 * is thrown, for example, by the {@link Switchboard#getServer(int,int)}
 * method if a client requests an invalid server.
 *
 * @see SwitchboardRemote
 * @see Switchboard
 */
public class UnrecognizedServerException extends RemoteException {
  /** Constructs a new <code>UnrecognizedServerException</code>. */
  public UnrecognizedServerException() { super(); }
  /** Constructs a new <code>UnrecognizedServerException</code> with the
      specified message. */
  public UnrecognizedServerException(String msg) { super(msg); }
  /** Constructs a new <code>UnrecognizedServerException</code> with the
      specified message and nested exception. */
  public UnrecognizedServerException(String msg, Throwable ex) {
    super(msg, ex);
  }
}
