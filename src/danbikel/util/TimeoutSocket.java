package danbikel.util;

import java.io.*;
import java.net.*;

class TimeoutSocket extends Socket {

  /* 
   * Constructor for class TimeoutSocket.
   */
  public TimeoutSocket(int timeout) throws IOException {
    super();
    setSoTimeout(timeout);
  }
  
  /* 
   * Constructor for class TimeoutSocket. 
   */
  public TimeoutSocket(String host, int port, int timeout) throws IOException {
    super(host, port);
    setSoTimeout(timeout);
  }
}
