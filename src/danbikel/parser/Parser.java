package danbikel.parser;

import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.util.Random;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;

/**
 * A parsing client.  This class parses sentences by implementing the
 * {@link AbstractClient#process(Object)} method of its {@link
 * AbstractClient superclass}.  All top-level probabilities are
 * computed by a {@link DecoderServer} object, which is either local
 * or is a stub whose methods are invoked via RMI.  The actual
 * parsing is done in the {@link Decoder} class.
 */
public class Parser
  extends AbstractClient implements ParserRemote {

  // constants
  private final static boolean debug = false;

  // data members
  private DecoderServerRemote server;
  private SexpList sent;

  public Parser(int timeout) throws RemoteException {
    super(timeout);
  }
  public Parser(int timeout, int port) throws RemoteException {
    super(timeout, port);
  }
  public Parser(int port,
		RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
  }

  protected void getServer() throws RemoteException {
    super.getServer();
    server = (DecoderServerRemote)super.server;
  }

  protected void tolerateFaults(int retries,
				int sleepTime,
				boolean failover) {
    super.tolerateFaults(retries, sleepTime, failover);
    server = (DecoderServerRemote)super.server;
  }


  private SexpList test(SexpList sent) throws RemoteException {
    double prob = server.testProb();
    if (debug)
      System.err.println("server returned prob. of " + prob);
    return sent;
  }

  protected Object process(Object obj) throws RemoteException {
    sent = (SexpList)obj;
    return test(sent);
  }

  /**
   * Prints the sentence currently being parsed to <code>System.err</code>
   * as an emergency backup (in case processing took a long time and
   * it is highly undesirable to lose the work).
   */
  protected void switchboardFailure() {
    System.err.println(sent);
  }

  /**
   * Obtains the timeout from <code>Settings</code>.
   *
   * @see Settings#sbUserTimeout
   */
  protected static int getTimeout() {
    String timeoutStr = Settings.get(Settings.sbUserTimeout);
    return (timeoutStr != null ? Integer.parseInt(timeoutStr) :
	    defaultTimeout);
  }

  private static int getRetries(int defaultValue) {
    return getIntProperty(Settings.serverMaxRetries, defaultValue);
  }

  private static int getRetrySleep(int defaultValue) {
    return getIntProperty(Settings.serverRetrySleep, defaultValue);
  }

  private static boolean getFailover(boolean defaultValue) {
    String failoverStr = Settings.get(Settings.serverFailover);
    return ((failoverStr == null) ? defaultValue :
	    Boolean.valueOf(failoverStr).booleanValue());
  }

  private static int getIntProperty(String property, int defaultValue) {
    String propStr = Settings.get(property);
    return (propStr == null) ? defaultValue : Integer.parseInt(propStr);
  }

  /**
   * Contacts the switchboard, registers this parsing client and
   * gets sentences from the switchboard, parses them and returns them,
   * until the switchboard indicates there are no more sentences to
   * process.  Multiple such clients may be created.
   */
  public static void main(String[] args) {
    String switchboardName = Switchboard.defaultBindingName;
    if (args.length > 1)
      switchboardName = args[0];
    //Create and install a security manager
    if (System.getSecurityManager() == null)
      System.setSecurityManager(new RMISecurityManager());
    Parser parser = null;
    // define fallback-default values for the following three
    // fault-tolerance settings
    int defaultRetries = 1, defaultRetrySleep = 1000;
    boolean defaultFailover = true;
    try {
      parser = new Parser(Parser.getTimeout());
      parser.register(switchboardName);
      Settings.setSettings(parser.switchboard.getSettings());
      parser.getFaultTolerantServer(getRetries(defaultRetries),
				    getRetrySleep(defaultRetrySleep),
				    getFailover(defaultFailover));
      parser.processObjectsThenDie();
    }
    catch (RemoteException re) {
      System.err.println(re);
      if (parser != null) {
	try { parser.die(true); }
	catch (RemoteException re2) {
	  System.err.println("couldn't die! (" + re + ")");
	}
      }
    }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
    if (debug)
      System.err.println("main ending!");
  }
}
