package danbikel.parser;

import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.net.MalformedURLException;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

/**
 * Provides probabilities and other resources required by decoders.
 */
public class DecoderServer
  extends AbstractServer implements DecoderServerRemote {

  // data members
  private ModelCollection modelCollection;
  private Symbol topSym = Language.training.topSym();
  private int unknownWordThreshold =
    Integer.parseInt(Settings.get(Settings.unknownWordThreshold));

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on an anonymous port.
   *
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   */
  public DecoderServer(int timeout) throws RemoteException {
    super(timeout);
  }

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on the specified port.
   *
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   * @param port the port on which to receive RMI calls
   */
  public DecoderServer(int timeout, int port) throws RemoteException {
    super(timeout, port);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * that will use the specified timeout for its RMI sockets and
   * will accept RMI calls on the specified port.
   *
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client- and server-side RMI sockets of this object
   * @param port the port on which to receive RMI calls
   */
  public DecoderServer(int maxClients, boolean acceptClientsOnlyByRequest,
		       int timeout, int port) throws RemoteException {
    super(maxClients, acceptClientsOnlyByRequest, timeout, port);
  }

  /**
   * Constructs a new server that will accept RMI calls on the specified
   * port, using the specified socket factories to create RMI sockets.
   *
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  public DecoderServer(int port,
		       RMIClientSocketFactory csf,
		       RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * will accept RMI calls on the specified port and will use the
   * specified socket factories to create its RMI sockets.
   *
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  public DecoderServer(int maxClients,
		       boolean acceptClientsOnlyByRequest,
		       int port,
		       RMIClientSocketFactory csf,
		       RMIServerSocketFactory ssf)
    throws RemoteException {
    super(maxClients, acceptClientsOnlyByRequest, port, csf, ssf);
  }

  /**
   * Sets the model collection from the specified filename, which should
   * be the path to a Java object file.
   */
  private void setModelCollection(String mcFilename)
    throws ClassNotFoundException, IOException, OptionalDataException {
    modelCollection = Trainer.loadModelCollection(mcFilename);
  }

  public SexpList convertUnknownWords(SexpList sentence)
    throws RemoteException {
    CountsTable vocabCounter = modelCollection.vocabCounter();
    int sentLen = sentence.length();
    for (int i = 0; i < sentLen; i++) {
      if (vocabCounter.count(sentence.get(i)) < unknownWordThreshold) {
        Symbol features =
          Language.wordFeatures.features(sentence.get(i).symbol(), i==0);
        sentence.set(i, features);
      }
    }
    return sentence;
  }

  public CountsTable nonterminals() throws RemoteException {
    return modelCollection.nonterminals();
  }

  public Map posMap() throws RemoteException {
    return modelCollection.posMap();
  }

  public Map leftSubcatMap() throws RemoteException {
    return modelCollection.leftSubcatMap();
  }

  public Map rightSubcatMap() throws RemoteException {
    return modelCollection.rightSubcatMap();
  }

  /** Returns 1.0. */
  public double testProb() throws RemoteException {
    return 1.0;
  }

  public double logPrior(int id, TrainerEvent event) {
    Model lexPriorModel = modelCollection.lexPriorModel();
    Model nonterminalPriorModel = modelCollection.nonterminalPriorModel();
    return (lexPriorModel.estimateLogProb(id, event) +
	    nonterminalPriorModel.estimateLogProb(id, event));
  }

  public double logProbHead(int id, TrainerEvent event) {
    if (event.parent() == topSym)
      return logProbTop(id, event);
    else {
      return (modelCollection.headModel().estimateLogProb(id, event) +
	      modelCollection.leftSubcatModel().estimateLogProb(id, event) +
	      modelCollection.rightSubcatModel().estimateLogProb(id, event));
    }
  }

  public double logProbTop(int id, TrainerEvent event) {
    Model topNTModel = modelCollection.topNonterminalModel();
    Model topLexModel = modelCollection.topLexModel();
    return (topNTModel.estimateLogProb(id, event) +
	    topLexModel.estimateLogProb(id, event));
  }

  public double logProbLeft(int id, TrainerEvent event) {
    Model leftModNTModel = modelCollection.leftModNonterminalModel();
    Model leftModWordModel = modelCollection.leftModWordModel();
    return (leftModNTModel.estimateLogProb(id, event) +
	    leftModWordModel.estimateLogProb(id, event));
  }

  public double logProbRight(int id, TrainerEvent event) {
    Model rightModNTModel = modelCollection.rightModNonterminalModel();
    Model rightModWordModel = modelCollection.rightModWordModel();
    return (rightModNTModel.estimateLogProb(id, event) +
	    rightModWordModel.estimateLogProb(id, event));
  }

  public double logProbGap(int id, TrainerEvent event) {
    return modelCollection.gapModel().estimateLogProb(id, event);
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

  /**
   * Starts a decoder server and registers it with the switchboard.  Accepts a
   * single argument, the RMI binding name of the switchboard.
   */
  public static void main(String[] args) {
    String switchboardName = Switchboard.defaultBindingName;
    if (args.length > 1)
      switchboardName = args[0];
    //Create and install a security manager
    if (System.getSecurityManager() == null)
      System.setSecurityManager(new RMISecurityManager());
    DecoderServer decoderServer = null;
    try {
      decoderServer = new DecoderServer(DecoderServer.getTimeout());
      decoderServer.register(switchboardName);
      Settings.setSettings(decoderServer.switchboard.getSettings());
      decoderServer.startAliveThread();
      decoderServer.unexportWhenDead();
    }
    catch (RemoteException re) {
      System.err.println(re);
      if (decoderServer != null) {
	try { decoderServer.die(true); }
	catch (RemoteException re2) {
	  System.err.println("couldn't die! (" + re + ")");
	}
      }
    }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
  }
}
