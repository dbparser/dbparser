package danbikel.switchboard;

import java.rmi.*;

/**
 * A semantic marker for those switchboard users that are servers;
 * also, specifies server-specific methods for the switchboard to use.
 * Servers are assigned <code>Client</code> objects by the
 * switchboard.
 * <p>
 * <b>A note on fault tolerance</b>: In order to ensure the
 * fault-tolerance of clients, implementors should ensure that they
 * use socket factories that set the <tt>SO_TIMEOUT</tt> values of
 * their TCP/IP sockets to some integer greater than 0, by providing
 * custom socket factories that provide at least the functionality of
 * {@link danbikel.util.TimeoutSocketFactory TimeoutSocketFactory}.
 * Subclasses that use sockets other than TCP/IP sockets should have
 * similar non-infinite timeouts.
 *
 * @see Client
 * @see SwitchboardRemote
 * @see AbstractServer
 */
public interface Server extends SwitchboardUser {
  public static final int acceptUnlimitedClients = 0;

  /**
   * The maximum number of clients this server is willing to accept,
   * or {@link #acceptUnlimitedClients} if this server is willing to accept a
   * virtually unlimited number of clients (a large maximum value may
   * be used by the switchboard).  It is an error for this method to
   * return any number other than a non-zero positive integer or
   * the special value.
   * <p>
   * Note that a server may change the return value of this method over time;
   * this method is guaranteed to be called and its value cached every time a
   * client is assigned to this server.
   *
   * @see SwitchboardRemote#register(Server)
   */
  public int maxClients() throws RemoteException;

  /**
   * Returns whether this server is only willing to accept clients that
   * request it.  This is the method by which a client can "arrange" to
   * be hooked up to a specific server, such as when it is desirable to
   * have a single server per client.
   * <p>
   * Note that a server may change the return value of this method over time;
   * this method is guaranteed to be called and its value cached every time a
   * client is assigned to this server.
   *
   * @see SwitchboardRemote#register(Server)
   * @see SwitchboardRemote#getServer(int,int)
   */
  public boolean acceptClientsOnlyByRequest() throws RemoteException;

  /** Returns the ID number of this server. */
  public int id() throws RemoteException;
}
