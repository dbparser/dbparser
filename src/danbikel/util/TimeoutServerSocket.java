package danbikel.util;

import java.net.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * A <code>ServerSocket</code> subclass that delivers <code>Socket</code>
 * objects via its implementation of {@link #accept} that have
 * had their timeout values set to the value specified at construction.
 */
public class TimeoutServerSocket extends ServerSocket {

  private int timeout;
  
  /**
   * Constructs a server socket on the specified port, that
   * delivers sockets with the specified timeout value via the
   * {@link #accept} method.
   */
  public TimeoutServerSocket(int timeout, int port) throws IOException {
    super(port);
    this.timeout = timeout;
  }

  /**
   * Creates a socket with the timeout value specified at construction,
   * then calls <code>ServerSocket.implAccept</code> to wait for a
   * connection.
   */
  public Socket accept() throws IOException {
    Socket s = new TimeoutSocket(timeout);
    implAccept(s);
    return s;
  }
}

