package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.util.Random;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

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

  // private constants
  private final static boolean debug = false;
  private final static String className = Parser.class.getName();

  // public constants
  public final static String outputFilenameSuffix = ".parsed";

  // data members
  private DecoderServerRemote server;
  private SexpList sent;
  private Decoder decoder;

  public Parser(String derivedDataFilename)
    throws RemoteException, IOException, ClassNotFoundException {
    server = new DecoderServer(derivedDataFilename);
    decoder = new Decoder(0, server);
  }

  public Parser(DecoderServerRemote server)
    throws RemoteException, IOException, ClassNotFoundException {
    this.server = server;
    decoder = new Decoder(0, server);
  }

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
      System.err.println(className + ": server returned prob. of " + prob);
    return sent;
  }

  public Sexp parse(SexpList sent) throws RemoteException {
    return decoder.parse(sent);
  }

  protected Object process(Object obj) throws RemoteException {
    if (decoder == null) {
      decoder = new Decoder(id, server);
    }
    sent = (SexpList)obj;
    return parse(sent);
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

  // main stuff
  private static String switchboardName = Switchboard.defaultBindingName;
  private static String derivedDataFilename = null;
  private static String inputFilename = null;
  private static String outputFilename = null;

  private static final String[] usageMsg = {
    "usage: [-internal-server <derived data file>] ",
    "\t[ [-sa <sentence input file> | --stand-alone <sentence input file> ",
    "\t  [-out <parse output file>] ] |",
    "\t  [switchboard binding name] ]"
  };

  private static final void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  private static final boolean processArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
        // process switch
        if (args[i].equals("-sa") || args[i].equals("--stand-alone")) {
          if (i + 1 == args.length) {
            System.err.println("error: " + args[i] + " requires a filename");
            usage();
            return false;
          }
          inputFilename = args[++i];
        }
        else if (args[i].equals("-internal-server")) {
          if (i + 1 == args.length) {
            System.err.println("error: " + args[i] + " requires a filename");
            usage();
            return false;
          }
          derivedDataFilename = args[++i];
        }
        else if (args[i].equals("-out")) {
          if (i + 1 == args.length) {
            System.err.println("err:" + args[i] + " requires a filename");
            usage();
            return false;
          }
          outputFilename = args[++i];
        }
        else {
          System.err.println("unrecognized command-line switch: " + args[i]);
          usage();
          return false;
        }
      }
      else
        switchboardName = args[i];
    }

    if (inputFilename != null && derivedDataFilename == null) {
      System.err.println("error: must use -internal-server with -sa");
      usage();
      return false;
    }

    if (inputFilename != null && outputFilename == null) {
      outputFilename = inputFilename + ".parsed";
    }

    return true;
  }

  /**
   * Contacts the switchboard, registers this parsing client and
   * gets sentences from the switchboard, parses them and returns them,
   * until the switchboard indicates there are no more sentences to
   * process.  Multiple such clients may be created.
   */
  public static void main(String[] args) {
    if (!processArgs(args))
      return;
    Parser parser = null;
    if (inputFilename != null) {
      try {
        File inFile = new File(inputFilename);
        if (!inFile.exists()) {
          System.err.println(className + ": error: file \"" + inputFilename +
                             "\" does not exist");
          return;
        }
        parser = new Parser(derivedDataFilename);
        int bufSize = Constants.defaultFileBufsize;
        OutputStreamWriter osw =
          new OutputStreamWriter(new FileOutputStream(outputFilename),
                                 Language.encoding());
        BufferedWriter out = new BufferedWriter(osw, bufSize);
        Sexp sent = null;
        SexpTokenizer tok =
          new SexpTokenizer(inputFilename, Language.encoding(),
                            Constants.defaultFileBufsize);
        Time totalTime = new Time();
        Time time = new Time();
        for (int i = 1; ((sent = Sexp.read(tok)) != null); i++) {
          System.err.println("processing sentence No. " + i + ": " + sent);
          time.reset();
          Sexp parsedSent = parser.parse(sent.list());
          System.err.println("elapsed time: " + time);
          out.write(String.valueOf(parsedSent));
          out.write("\n");
        }
        System.err.println("\ntotal elapsed time: " + totalTime);
        System.err.println("\nHave a nice day!");
        out.flush();
      }
      catch (RemoteException re) {
        System.err.println(re);
      }
      catch (IOException ioe) {
        System.err.println(ioe);
      }
      catch (ClassNotFoundException cnfe) {
        System.err.println(cnfe);
      }
    }
    else {
      //Create and install a security manager
      if (System.getSecurityManager() == null)
        System.setSecurityManager(new RMISecurityManager());
      // define fallback-default values for the following three
      // fault-tolerance settings
      int defaultRetries = 1, defaultRetrySleep = 1000;
      boolean defaultFailover = true;
      try {
        parser = new Parser(Parser.getTimeout());
        parser.register(switchboardName);
        Settings.setSettings(parser.switchboard.getSettings());
        if (derivedDataFilename != null)
          parser.server = new DecoderServer(derivedDataFilename);
        else
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
      catch (IOException ioe) {
        System.err.println(ioe);
      }
      catch (ClassNotFoundException cnfe) {
        System.err.println(cnfe);
      }
    }
    if (debug)
      System.err.println(className + ": main ending!");
  }
}
