package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import danbikel.parser.util.*;
import java.util.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

public class EMParser extends Parser {
  // private constants
  private final static boolean debug = false;
  private final static boolean debugCacheStats = true;
  private final static String className = EMParser.class.getName();
  private final static boolean flushAfterEverySentence = true;
  private final static int outputInterval = 1000;

  // additional data member
  protected EMDecoder decoder;

  public EMParser(String derivedDataFilename)
    throws RemoteException, IOException, ClassNotFoundException {
    super(derivedDataFilename);
  }

  public EMParser(DecoderServerRemote server)
    throws RemoteException, IOException, ClassNotFoundException {
    super(server);
  }

  public EMParser(int timeout) throws RemoteException {
    super(timeout);
  }
  public EMParser(int timeout, int port) throws RemoteException {
    super(timeout, port);
  }
  public EMParser(int port,
		RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
  }

  protected Decoder getNewDecoder(int id, DecoderServerRemote server) {
    decoder = new EMDecoder(id, server);
    return decoder;
  }

  protected Object process(Object obj) throws RemoteException {
    if (decoder == null) {
      decoder = (EMDecoder)getNewDecoder(id, server);
    }
    sent = (SexpList)obj;
    return parseAndCollectEventCounts(sent);
  }

  public CountsTable parseAndCollectEventCounts(SexpList sent)
    throws RemoteException {
    if (sentContainsWordsAndTags(sent))
      return decoder.parseAndCollectEventCounts(getWords(sent),
                                                getTagLists(sent));
    else if (sent.isAllSymbols())
      return decoder.parseAndCollectEventCounts(sent);
    else if (Language.training.isValidTree(sent) ||
             Language.training.isValidTree(sent.get(0))) {
      sent = preProcess(sent);
      return decoder.parseAndCollectEventCounts(getWordsFromTree(sent),
                                                getTagListsFromTree(sent),
                                                ConstraintSets.get(sent));
    }
    else {
      System.err.println(className + ": error: sentence \"" + sent +
                         "\" has a bad format:\n\tmust either be all symbols " +
                         "or a list of lists of the form (<word> (<tag>*))");
      return null;
    }
  }

  protected SexpList preProcess(Sexp tree) {
    boolean stripOuterParens = (tree.list().length() == 1 &&
	    		        tree.list().get(0).isList());
    if (stripOuterParens)
      tree = tree.list().get(0);

    Language.training.prune(tree);
    Language.training.addBaseNPs(tree);
    Language.training.repairBaseNPs(tree);
    //Language.training.addGapInformation(tree);
    //Language.training.relabelSubjectlessSentences(tree);
    Language.training.removeNullElements(tree);
    Language.training.raisePunctuation(tree);
    //Language.training.identifyArguments(tree);
    Language.training.stripAugmentations(tree);
    return tree.list();
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
  private static String settingsFilename = null;
  private static int numClients = 1;

  private static final String[] usageMsg = {
    "usage: [-nc <numClients> | -num-clients <numClients>]",
    "\t[-sf <settings file> | --settings <settings file>]",
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
        if (args[i].equals("-sf") || args[i].equals("--settings")) {
          if (i + 1 == args.length) {
            System.err.println("error: " + args[i] + " requires a filename");
            usage();
            return false;
          }
          settingsFilename = args[++i];
        }
        else if (args[i].equals("-sa") || args[i].equals("--stand-alone")) {
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
            System.err.println("error: " + args[i] + " requires a filename");
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
            System.err.println("error: " + args[i] + " requires an integer");
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
      outputFilename = inputFilename + ".counts";
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
    EMParser parser = null;
    if (inputFilename != null) {
      try {
        File inFile = new File(inputFilename);
        if (!inFile.exists()) {
          System.err.println(className + ": error: file \"" + inputFilename +
                             "\" does not exist");
          return;
        }
        if (settingsFilename != null)
          Settings.load(settingsFilename);
        parser = new EMParser(derivedDataFilename);
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
        CountsTable eventCounts = new CountsTable();
	int outputCounter = 0;
        for (int i = 1; ((sent = Sexp.read(tok)) != null); i++) {
          System.err.println("processing sentence No. " + i);
          time.reset();
          CountsTable currEvents =
            parser.parseAndCollectEventCounts(sent.list());
          System.err.println("elapsed time: " + time);
          System.err.println("cummulative average elapsed time: " +
                             Time.elapsedTime(totalTime.elapsedMillis() / i));
          if (currEvents != null) {
            Iterator it = currEvents.entrySet().iterator();
            while (it.hasNext()) {
              MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
              eventCounts.add(entry.getKey(), entry.getDoubleValue());
            }
	    outputCounter++;
          }
          /*
          if (flushAfterEverySentence)
            out.flush();
          */
	  if (outputCounter == outputInterval) {
	    EventCountsWriter.outputEvents(eventCounts, out);
	    eventCounts.clear();
	    outputCounter = 0;
	  }
        }
        EventCountsWriter.outputEvents(eventCounts, out);
        out.flush();
        System.err.println("\ntotal elapsed time: " + totalTime);
        System.err.println("\nHave a nice day!");
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
            parser = new EMParser(Parser.getTimeout());
            parser.register(switchboardName);
            Properties sbSettings = parser.switchboard.getSettings();
            if (derivedDataFilename != null)
              checkSettings(sbSettings);
            Settings.setSettings(sbSettings);
            if (settingsFilename != null)
              Settings.load(settingsFilename);
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

    if (debugCacheStats) {
      parser = null;
      System.gc();
      System.runFinalization();
    }
  }
}
