package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import danbikel.parser.constraints.*;
import danbikel.parser.util.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;

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
  private final static boolean debugCacheStats = true;
  private final static String className = Parser.class.getName();
  private final static boolean flushAfterEverySentence = true;
  private final static String decoderClassName =
    Settings.get(Settings.decoderClass);
  private final static String decoderServerClassName =
    Settings.get(Settings.decoderServerClass);
  protected final static Class[] stringTypeArr = {String.class};
  protected final static Class[] intTypeArr = {Integer.TYPE};
  protected final static Class[] newDecoderTypeArr =
    {Integer.TYPE,DecoderServerRemote.class};

  // protected constants
  /** Cached value of {@link Settings#keepAllWords}, for efficiency and
      convenience. */
  protected boolean keepAllWords =
    Boolean.valueOf(Settings.get(Settings.keepAllWords)).booleanValue();

  // public constants
  public final static String outputFilenameSuffix = ".parsed";

  // protected static data
  /**
   * The subclass of <code>Parser</code> to be constructed by
   * the {@link #main(String[])} method of this class.
   */
  protected static Class parserClass = Parser.class;

  // data members
  protected DecoderServerRemote server;
  protected SexpList sent;
  protected Decoder decoder;
  protected boolean localServer = false;
  // the next two data members are only used when not in stand-alone mode
  // (i.e., when using the switchboard), and when the user has specified
  // an input filename and, optionally, an output filename
  protected String internalInputFilename = null;
  protected String internalOutputFilename = null;

  /**
   * A {@link PrintWriter} object wrapped around {@link System#err} for
   * printing in the proper character encoding.
   */
  protected PrintWriter err;

  public Parser(String derivedDataFilename)
    throws RemoteException, IOException, ClassNotFoundException,
	   NoSuchMethodException, java.lang.reflect.InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    server = getNewDecoderServer(derivedDataFilename);
    decoder = getNewDecoder(0, server);
    setUpErrWriter();
  }

  public Parser(DecoderServerRemote server)
    throws RemoteException, IOException, ClassNotFoundException {
    this.server = server;
    decoder = getNewDecoder(0, server);
    setUpErrWriter();
  }

  public Parser(int timeout) throws RemoteException {
    super(timeout);
    setUpErrWriter();
  }
  public Parser(int timeout, int port) throws RemoteException {
    super(timeout, port);
    setUpErrWriter();
  }
  public Parser(int port,
		RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
    setUpErrWriter();
  }

  private void setUpErrWriter() {
    OutputStreamWriter errosw = null;
    try {
      errosw = new OutputStreamWriter(System.err, Language.encoding());
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(className + ": error: couldn't create err output " +
			 "stream using encoding " + Language.encoding() +
			 "(reason: " + uee + ")");
      System.err.println("\tusing default encoding instead");
      errosw = new OutputStreamWriter(System.err);
    }
    err = new PrintWriter(errosw, true);
  }

  /*
  protected Decoder getNewDecoder(int id, DecoderServerRemote server) {
    return new Decoder(id, server);
  }
  */
  protected Decoder getNewDecoder(int id, DecoderServerRemote server) {
    Decoder decoder = null;
    try {
      Class decoderClass = Class.forName(decoderClassName);
      
      Constructor cons = decoderClass.getConstructor(newDecoderTypeArr);
      Object[] argArr = new Object[]{new Integer(id), server};
      decoder = (Decoder)cons.newInstance(argArr);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return decoder;
  }

  // helper method for the "stand-alone" constructor
  protected static DecoderServer getNewDecoderServer(String derivedDataFilename)
    throws RemoteException, IOException, ClassNotFoundException,
	   NoSuchMethodException, java.lang.reflect.InvocationTargetException,
	   IllegalAccessException, InstantiationException {

    Class decoderServerClass = Class.forName(decoderServerClassName);

    DecoderServer server = null;

    Constructor cons = decoderServerClass.getConstructor(stringTypeArr);
    Object[] argArr = new Object[]{derivedDataFilename};
    server = (DecoderServer)cons.newInstance(argArr);

    return server;
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


  /**
   * We override {@link AbstractClient#cleanup()} here so that it
   * sets the internal {@link #server} data member to <code>null</code>
   * only when not debugging cache stats (which is provided as an internal,
   * compile-time option via a private data member).  The default behavior
   * of this method, as defined in the superclass' implementation, is simply
   * to set the server data member to be <code>null</code>.
   */
  /*
  protected void cleanup() {
    if (debugCacheStats == false)
      server = null;
  }
  */

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
    else if (Language.training.isValidTree(sent)) {
      return decoder.parse(getWordsFromTree(sent),
			   getTagListsFromTree(sent),
			   getConstraintsFromTree(sent));
    }
    else {
      err.println(className + ": error: sentence \"" + sent +
			 "\" has a bad format:\n\tmust either be all symbols " +
			 "or a list of lists of the form (<word> (<tag>*))");
      return null;
    }
  }

  protected Sexp convertUnknownWords(Sexp tree, IntCounter currWordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      Word wordObj = Language.treebank().makeWord(tree);

      // change word to unknown, if necessary
      Sexp wordInfo = null;
      try {
	wordInfo = server.convertUnknownWord(wordObj.word(), currWordIdx.get());
      }
      catch (RemoteException re) {
	System.err.println(re);
      }
      Symbol word = null;
      boolean wordIsUnknown = wordInfo.isList();
      if (wordIsUnknown) {
	SexpList wordInfoList = wordInfo.list();
	Symbol features = wordInfoList.symbolAt(1);
	boolean neverObserved = wordInfoList.symbolAt(2) == Constants.trueSym;

	if (keepAllWords) {
	  word = neverObserved ? features : wordInfoList.symbolAt(0);
	}
	else {
	  word = features;
	}

	wordObj.setWord(word);
      }

      tree = Language.treebank().constructPreterminal(wordObj);

      currWordIdx.increment();
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	treeList.set(i, convertUnknownWords(treeList.get(i), currWordIdx));
      }
    }

    return tree;
  }

  protected ConstraintSet getConstraintsFromTree(Sexp tree) {
    // since we're about to destructively modify tree, let's deeply copy it
    tree = tree.deepCopy();
    convertUnknownWords(tree, new IntCounter(0));
    return ConstraintSets.get(tree);
  }

  protected boolean sentContainsWordsAndTags(SexpList sent) {
    int size = sent.size();
    for (int i = 0; i < size; i++) {
      if (!wordTagList(sent.get(i)))
	return false;
    }
    return true;
  }

  protected boolean wordTagList(Sexp sexp) {
    if (sexp.isSymbol())
      return false;
    SexpList list = sexp.list();
    // this is a word-tag list is the first element is a symbol (the word)
    // and the second element is a list containing all symbols (the list
    // of possible tags)
    return (list.size() == 2 && list.get(0).isSymbol() &&
	    list.get(1).isList() && list.get(1).list().isAllSymbols());
  }

  protected SexpList getWords(SexpList sent) {
    int size = sent.size();
    SexpList wordList = new SexpList(size);
    for (int i = 0; i < size; i++)
      wordList.add(sent.get(i).list().get(0));
    return wordList;
  }

  protected SexpList getWordsFromTree(Sexp tree) {
    return getWordsFromTree(new SexpList(), tree);
  }

  protected SexpList getWordsFromTree(SexpList wordList, Sexp tree) {
    if (Language.treebank.isPreterminal(tree)) {
      Word word = Language.treebank.makeWord(tree);
      wordList.add(word.word());
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++)
	getWordsFromTree(wordList, treeList.get(i));
    }
    return wordList;
  }

  protected SexpList getTagListsFromTree(Sexp tree) {
    Sexp taggedSentence = Util.collectTaggedWords(tree);
    return getTagLists(taggedSentence.list());
  }

  protected SexpList getTagLists(SexpList sent) {
    int size = sent.size();
    SexpList tagLists = new SexpList(size);
    for (int i = 0; i < size; i++)
      tagLists.add(sent.get(i).list().get(1));
    return tagLists;
  }

  protected Object process(Object obj) throws RemoteException {
    if (decoder == null) {
      decoder = getNewDecoder(id, server);
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
    err.println(sent);
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

  protected static int getRetries(int defaultValue) {
    return getIntProperty(Settings.serverMaxRetries, defaultValue);
  }

  protected static int getRetrySleep(int defaultValue) {
    return getIntProperty(Settings.serverRetrySleep, defaultValue);
  }

  protected static boolean getFailover(boolean defaultValue) {
    String failoverStr = Settings.get(Settings.serverFailover);
    return ((failoverStr == null) ? defaultValue :
	    Boolean.valueOf(failoverStr).booleanValue());
  }

  protected static int getIntProperty(String property, int defaultValue) {
    String propStr = Settings.get(property);
    return (propStr == null) ? defaultValue : Integer.parseInt(propStr);
  }

  public void run() {
    if (internalInputFilename == null) {
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
    else {
      try {
	processInputFile(internalInputFilename, internalOutputFilename);
	die(false);
      }
      catch (IOException ioe) {
	System.err.println(ioe);
	try { die(true); }
	catch (RemoteException re) {
	  System.err.println("client " + id + " couldn't die! (" + re + ")");
	}
      }
    }
  }


  protected void setInternalFilenames(String inputFilename,
				      String outputFilename) {
    internalInputFilename = inputFilename;
    internalOutputFilename = outputFilename;
  }

  protected void processInputFile(String inputFilename, String outputFilename)
    throws IOException {

    if (decoder == null) {
      decoder = getNewDecoder(id, server);
    }

    InputStream in = null;
    if (inputFilename.equals("-")) {
      in = System.in;
    }
    else {
      File inFile = getFile(inputFilename);
      if (inFile == null)
	return;
      in = new FileInputStream(inFile);
    }

    int bufSize = Constants.defaultFileBufsize;
    OutputStream outputStream = (outputFilename.equals("-") ?
				 (OutputStream)System.out :
				 new FileOutputStream(outputFilename));
    OutputStreamWriter osw =
      new OutputStreamWriter(outputStream, Language.encoding());
    BufferedWriter out = new BufferedWriter(osw, bufSize);
    Sexp sent = null;
    SexpTokenizer tok = new SexpTokenizer(in, Language.encoding(), bufSize);
    Time totalTime = new Time();
    Time time = new Time();
    for (int i = 1; ((sent = Sexp.read(tok)) != null); i++) {
      err.println("processing sentence No. " + i + ": " + sent);
      time.reset();
      Sexp parsedSent = parse(sent.list());
      err.println("elapsed time: " + time);
      err.println("cummulative average elapsed time: " +
			 Time.elapsedTime(totalTime.elapsedMillis() / i));
      out.write(String.valueOf(parsedSent));
      out.write("\n");
      if (flushAfterEverySentence)
	out.flush();
    }
    err.println("\ntotal elapsed time: " + totalTime);
    err.println("\nHave a nice day!");
    out.flush();
  }

  // main stuff
  protected static String switchboardName = Switchboard.defaultBindingName;
  protected static String derivedDataFilename = null;
  protected static String inputFilename = null;
  protected static String outputFilename = null;
  protected static String settingsFilename = null;
  protected static int numClients = 1;
  protected static boolean standAlone = false;
  protected static boolean grabSBSettings = true;

  private static final String[] usageMsg = {
    "usage: [-nc <numClients> | --num-clients <numClients>]",
    "\t[-sf <settings file> | --settings <settings file>]",
    "\t[--no-sb-settings]",
    "\t[-is <derived data file> | --internal-server <derived data file>] ",
    "\t[ [-sa <sentence input file> | --stand-alone <sentence input file> ",
    "\t       [-out <parse output file>] ] |",
    "\t  [<switchboard binding name>] ",
    "\t       [-in <sentence input file>] [-out <parse output file>] ]",
    "where",
    "\t<numClients> is the number of parser clients to start when using",
    "\t\tthe switchboard (ignored when in stand-alone mode)",
    "\t<settings file> is the name of a settings file to load locally",
    "\t--no-sb-settings indicates not to grab settings from the switchboard",
    "\t--internal-server|-is specifies to create an internal decoder server",
    "\t\tinstead of requesting one from the switchboard",
    "\t--stand-alone|-sa specifies not to use the switchboard, but to parse",
    "\t\tthe specified <sentence input file> with an internal",
    "\t\tdecoder server (must be used with --internal-server option)",
    "\t<sentence input file> is the sentence input file, or '-' for stdin",
    "\t<parse outputfile> is the name of the output file, or '-' for stdout",
    "\t-in specifies a sentence input file when using the switchboard,",
    "\t\twhich means the switchboard's object server facility will",
    "\t\tnot be used (only one client may be started in this mode)"
  };

  private static final void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  private static final boolean processArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
	// process switch
	if (args[i].equals("-help") || args[i].equals("-usage")) {
	  usage();
	  System.exit(0);
	}
	if (args[i].equals("-sf") || args[i].equals("--settings")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  settingsFilename = args[++i];
	}
	else if (args[i].equals("--no-sb-settings")) {
	  grabSBSettings = false;
	}
	else if (args[i].equals("-sa") || args[i].equals("--stand-alone")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  standAlone = true;
	  inputFilename = args[++i];
	}
	else if (args[i].equals("-is") || args[i].equals("--internal-server")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  derivedDataFilename = args[++i];
	}
	else if (args[i].equals("-in")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  inputFilename = args[++i];
	}
	else if (args[i].equals("-out")) {
	  if (i + 1 == args.length) {
	    System.err.println("error: " + args[i] + " requires a filename");
	    usage();
	    return false;
	  }
	  outputFilename = args[++i];
	}
	else if (args[i].equals("-nc") || args[i].equals("--num-clients")) {
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
      else {
	switchboardName = args[i];
      }
    }

    if (!standAlone && numClients < 1) {
      System.err.println("error: number of clients must be greater than zero");
      usage();
      return false;
    }

    if (standAlone && derivedDataFilename == null) {
      System.err.println("error: must use --internal-server with -sa");
      usage();
      return false;
    }

    if (!standAlone && inputFilename != null && numClients > 1) {
      System.err.println(
      "error: can't start more than one parsing client thread when internally"+
      "\n\tprocessing an input file; use switchboard's object server facility");
      usage();
      return false;
    }

    // we guarantee that if user specifies an input filename, the static
    // data member outputFilename will also be non-null
    if (inputFilename != null && outputFilename == null) {
      if (inputFilename.equals("-"))
	outputFilename = "stdin.parsed";
      else
	outputFilename = inputFilename + ".parsed";
    }

    return true;
  }

  protected static void checkSettings(Properties sbSettings) {
    Iterator it = sbSettings.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      String sbProp = (String)entry.getKey();
      String sbVal = (String)entry.getValue();
      String localVal = Settings.get(sbProp);
      if (sbVal.equals(localVal) == false) {
	System.err.println(className + ": warning: value of property \"" +
			   sbProp + "\" is\n\t\t\"" + localVal + "\"\n\t" +
			   "in settings obtained locally but is\n\t\t\"" +
			   sbVal +
			   "\"\n\tin settings obtained from switchboard");
      }
    }
  }

  public static void setSettingsFromSwitchboard(SwitchboardRemote sb)
    throws RemoteException {
    Properties sbSettings = sb.getSettings();
    if (derivedDataFilename != null)
      checkSettings(sbSettings);
    Settings.setSettings(sbSettings);
  }

  protected static Parser getNewParser(String derivedDataFilename)
    throws NoSuchMethodException, InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    Parser parser = null;
    Constructor cons = parserClass.getConstructor(stringTypeArr);
    parser = (Parser)cons.newInstance(new Object[]{derivedDataFilename});
    return parser;
  }

  protected static Parser getNewParser(int timeout)
    throws NoSuchMethodException, InvocationTargetException,
	   IllegalAccessException, InstantiationException {
    Parser parser = null;
    Constructor cons = parserClass.getConstructor(intTypeArr);
    parser = (Parser)cons.newInstance(new Object[]{new Integer(timeout)});
    return parser;
  }

  public static File getFile(String filename) {
    return getFile(filename, true);
  }

  public static File getFile(String filename, boolean verbose) {
    File file = new File(filename);
    if (file.exists()) {
      return file;
    }
    else {
      if (verbose)
	System.err.println(className + ": error: file \"" + filename +
			   "\" does not exist");
      return null;
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
    DecoderServer server = null;
    ThreadGroup clientThreads = new ThreadGroup("parser clients");
    if (standAlone) {
      try {
	if (settingsFilename != null) {
	  if (getFile(settingsFilename) == null)
	    return;
	  Settings.load(settingsFilename);
	}
	if (getFile(derivedDataFilename) == null)
	  return;
	//parser = new Parser(derivedDataFilename);
	parser = getNewParser(derivedDataFilename);
	parser.processInputFile(inputFilename, outputFilename);
      }
      catch (InstantiationException ie) {
	System.err.println(ie);
      }
      catch (IllegalAccessException iae) {
	System.err.println(iae);
      }
      catch (java.lang.reflect.InvocationTargetException ite) {
	ite.printStackTrace();
      }
      catch (NoSuchMethodException nsme) {
	System.err.println(nsme);
      }
      catch (RemoteException re) {
	System.err.println(re);
      }
      catch (IOException ioe) {
	System.err.println(ioe);
      }
    }
    else {
      setPolicyFile(Settings.getSettings());
      // create and install a security manager
      if (System.getSecurityManager() == null)
	System.setSecurityManager(new RMISecurityManager());
      // define fallback-default values for the following three
      // fault-tolerance settings
      int defaultRetries = 1, defaultRetrySleep = 1000;
      boolean defaultFailover = true;
      try {
	//DecoderServer server = null;
	if (derivedDataFilename != null) {
	  if (getFile(derivedDataFilename) == null)
	    return;
	  server = getNewDecoderServer(derivedDataFilename);
	}

	for (int i = 0; i < numClients; i++) {
	  try {
	    // first, try to get the switchboard so as to get settings
	    // BEFORE creating a Parser instance, so that any static data
	    // will be correct before the static initializers of Parser
	    // (and any dependent classes) are run
	    SwitchboardRemote sb =
	      AbstractSwitchboardUser.getSwitchboard(switchboardName,
						     defaultRetries);
	    if (grabSBSettings && sb != null) {
	      setSettingsFromSwitchboard(sb);
	      if (settingsFilename != null)
		Settings.load(settingsFilename);
	    }
	    //parser = new Parser(Parser.getTimeout());
	    parser = getNewParser(Parser.getTimeout());
	    parser.register(switchboardName);
	    if (grabSBSettings && sb == null) {
	      setSettingsFromSwitchboard(parser.switchboard);
	      if (settingsFilename != null) {
		Settings.load(settingsFilename);
	      }
	    }
	    if (derivedDataFilename != null) {
	      parser.server = server;
	      parser.localServer = true;
	    }
	    else
	      parser.getFaultTolerantServer(getRetries(defaultRetries),
					    getRetrySleep(defaultRetrySleep),
					    getFailover(defaultFailover));
	    if (inputFilename != null) {
	      parser.setInternalFilenames(inputFilename, outputFilename);
	    }
	    new Thread(clientThreads, parser,
		       "Parse Client " + parser.id).start();
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
      catch (InstantiationException ie) {
	System.err.println(ie);
      }
      catch (IllegalAccessException iae) {
	System.err.println(iae);
      }
      catch (java.lang.reflect.InvocationTargetException ite) {
	ite.printStackTrace();
      }
      catch (NoSuchMethodException nsme) {
	System.err.println(nsme);
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

    // we only print cache stats when in stand-alone mode or when
    // there is an internal server
    if (debugCacheStats) {
      if (standAlone){
	System.err.println(((DecoderServer)parser.server).getModelCacheStats());
      }
      else if (derivedDataFilename != null) {
	while (clientThreads.activeCount() > 0) {
	  try {
	    Thread.currentThread().sleep(1000);
	  }
	  catch (InterruptedException ie) {
	    System.err.println(ie);
	  }
	}
	System.err.println(server.getModelCacheStats());
      }
      else {
	System.err.println(className + ": warning: not printing model cache " +
			   "stats because decoder server is a remote object");
      }
      /*
      parser = null;
      server = null;
      System.gc();
      System.runFinalization();
      */
    }
  }
}
