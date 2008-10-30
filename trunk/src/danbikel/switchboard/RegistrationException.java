package danbikel.switchboard;

import java.rmi.*;

/**
 * An exception raised when a switchboard user cannot be registered
 * properly with the switchboard.
 *
 * @see SwitchboardRemote
 * @see Switchboard
 */
public class RegistrationException extends RemoteException {
  /** Constructs a new <code>RegistrationException</code>. */
  public RegistrationException() { super(); }
  /** Constructs a new <code>RegistrationException</code> with the specified
      message. */
  public RegistrationException(String msg) { super(msg); }
  /** Constructs a new <code>RegistrationException</code> with the specified
      message and nested exception. */
  public RegistrationException(String msg, Throwable ex) { super(msg, ex); }
}
