package danbikel.switchboard;

import danbikel.util.proxy.Reconnect;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

/**
 * An RMI invocation handler that gets a new server for switchboard clients
 * in the event of a method invocation failure.
 */
public class Failover extends Reconnect {
  // constants
  private final static boolean debug = true;

  // data members
  private SwitchboardRemote switchboard;
  private Client client;

  public Failover(Object server, Client client, SwitchboardRemote switchboard) {
    super(server, null);
    this.client = client;
    this.switchboard = switchboard;
  }

  public static synchronized Object proxyFor(Object server,
					     Client client,
					     SwitchboardRemote switchboard) {
    Class serverClass = server.getClass();
    return Proxy.newProxyInstance(serverClass.getClassLoader(),
				  serverClass.getInterfaces(),
				  new Failover(server, client, switchboard));
  }

  protected void validate() throws RemoteException {
    int clientId = client.id();

    if (stub == null) {

      if (debug)
	System.err.println(getClass().getName() +
			   ": trying to get a new server for client No. " +
			   clientId);

      stub = switchboard.getServer(clientId);

      if (stub != null)
	if (debug)
	  System.err.println(getClass().getName() +
			     ": got a new server for client No. " + clientId);
    }
    if (stub == null)
      throw new RemoteException(client.getClass().getName() +
				": switchboard did not return a server for " +
				"client No. " + clientId);
  }
}
