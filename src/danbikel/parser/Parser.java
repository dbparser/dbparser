package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.util.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

/**
 * A parsing client.  This class parses sentences by implementing the
 * {@link AbstractClient#process(Object)} method of its {@link
 * AbstractClient superclass}.  All top-level probabilities are
 * computed by a <code>DecoderServer</code> object, which is either local
 * or is a stub whose methods are invoked via RMI.  The actual
 * parsing is implemented in the <code>Decoder</code> class.
 *
 * @see AbstractClient
 * @see DecoderServer
 * @see Decoder
 */
public class Parser
  extends AbstractClient implements ParserRemote, Runnable {

  // private constants
  private final static boolean debug = false;
  private final static String className = Parser.class.getName();

  // public constants
  public final static String outputFilenameSuffix = ".parsed";

  // data members
  private DecoderServerRemote server;
  private SexpList sent;
  private Decoder decoder;
  private boolean localServer = false;

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
    // the following check is necessary, as this method will be called
    // by reRegister, which is called when Switchboard failure is
    // detected by AbstractClient.processObjects
    if (localServer)
      return;

    super.getServer();
    server = (DecoderServerRemote)super.server;
  }

  protected void tolerateFaults(int retries,
				int sleepTime,
				boolean failover) {
    // the following check is necessary, as this method will be called
    // by reRegister, which is called when Switchboard failure is
    // detected by AbstractClient.processObjects
    if (localServer)
      return;

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
    if (sentContainsWordsAndTags(sent))
      return decoder.parse(getWords(sent), getTagLists(sent));
    else if (sent.isAllSymbols())
      return decoder.parse(sent);
    else {
      System.err.println(className + ": error: sentence \"" + sent +
                         "\" has a bad format:\n\tmust either be all symbols " +
                         "or a list of lists of the form (<word> (<tag>*))");
      return null;
    }
  }

  private boolean sentContainsWordsAndTags(SexpList sent) {
    int size = sent.size();
    for (int i = 0; i < size; i++) {
      if (!wordTagList(sent.get(i)))
        return false;
    }
    return true;
  }

  private boolean wordTagList(Sexp sexp) {
    if (sexp.isSymbol())
      return false;
    SexpList list = sexp.list();
    // this is a word-tag list is the first element is a symbol (the word)
    // and the second element is a list containing all symbols (the list
    // of possible tags)
    return (list.size() == 2 && list.get(0).isSymbol() &&
            list.get(1).isList() && list.get(1).list().isAllSymbols());
  }

  private SexpList getWords(SexpList sent) {
    int size = sent.size();
    SexpList wordList = new SexpList(size);
    for (int i = 0; i < size; i++)
      wordList.add(sent.get(i).list().get(0));
    return wordList;
  }

  private SexpList getTagLists(SexpList sent) {
    int size = sent.size();
    SexpList tagLists = new SexpList(size);
    for (int i = 0; i < size; i++)
      tagLists.add(sent.get(i).list().get(1));
    return tagLists;
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

  public void run() {
    try {
      processObjectsThenDie();
    }
    catch (RemoteException re) {
      System.err.println(re);
       try { die(true); }
        catch (RemoteException re2) {
          System.err.println("client " + id + " couldn't die! (" + re + ")");
        }
    }
  }


  // main stuff
  private static String switchboardName = Switchboard.defaultBindingName;
  private static String derivedDataFilename = null;
  private static String inputFilename = null;
  private static String outputFilename = null;
  private static int numClients = 1;

  private static final String[] usageMsg = {
    "usage: [-nc <numClients> | -num-clients <numClients>]",
    "\t[-is <derived data file> | -internal-server <derived data file>] ",
    "\t[ [-sa <sentence input file> | --stand-alone <sentence input file> ",
    "\t       [-out <parse output file>] ] |",
    "\t  <switchboard binding name> ]"
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
        else if (args[i].equals("-is") || args[i].equals("-internal-server")) {
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
        else if (args[i].equals("-nc") || args[i].equals("-num-clients")) {
          if (i + 1 == args.length) {
            System.err.println("error: " + args[i] + " requires an integer");
            usage();
            return false;
          }
          try {
            numClients = Integer.parseInt(args[++i]);
          }
          catch (NumberFormatException nfe) {
            System.err.println("error:" + args[i] + " requires an integer");
            usage();
            return false;
          }
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

    if (numClients < 1) {
      System.err.println("error: number of clients must be greater than zero");
      usage();
      return false;
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

  private static void checkSettings(Properties sbSettings) {
    Iterator it = sbSettings.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      String sbProp = (String)entry.getKey();
      String sbVal = (String)entry.getValue();
      String localVal = Settings.get(sbProp);
      if (sbVal.equals(localVal) == false) {
        System.err.println(className + ": warning: value of property \"" +
                           sbProp + "\" is\n\t\t\"" + localVal + "\"\n\t" +
                           "in settings " +
                           "obtained from \"" +
                           derivedDataFilename + "\" but is\n\t\t\"" + sbVal +
                           "\"\n\tin settings obtained from switchboard");
      }
    }
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
      // create and install a security manager
      if (System.getSecurityManager() == null)
        System.setSecurityManager(new RMISecurityManager());
      // define fallback-default values for the following three
      // fault-tolerance settings
      int defaultRetries = 1, defaultRetrySleep = 1000;
      boolean defaultFailover = true;
      try {
        DecoderServer server = null;
        if (derivedDataFilename != null)
          server = new DecoderServer(derivedDataFilename);

        for (int i = 0; i < numClients; i++) {
          try {
            parser = new Parser(Parser.getTimeout());
            parser.register(switchboardName);
            Properties sbSettings = parser.switchboard.getSettings();
            if (derivedDataFilename != null)
              checkSettings(sbSettings);
            Settings.setSettings(sbSettings);
            if (derivedDataFilename != null) {
              parser.server = server;
              parser.localServer = true;
            }
            else
              parser.getFaultTolerantServer(getRetries(defaultRetries),
                                            getRetrySleep(defaultRetrySleep),
                                            getFailover(defaultFailover));
            new Thread(parser, "Parse Client " + parser.id).start();
          }
          catch (RemoteException re) {
            System.err.println(re);
            if (parser != null) {
      	    try { parser.die(true); }
              catch (RemoteException re2) {
                System.err.println("client " + parser.id +
                                   " couldn't die! (" + re + ")");
              }
            }
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
    parser = null;
    System.gc();
    System.runFinalization();
  }
}
