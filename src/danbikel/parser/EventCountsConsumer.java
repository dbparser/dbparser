package danbikel.parser;

import danbikel.util.*;
import danbikel.switchboard.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class EventCountsConsumer implements Consumer, Runnable {
  // constants
  private final static String className = Consumer.class.getName();
  /** The default writing interval for this consumer. */
  public final static int defaultWriteInterval = 500;
  private final static double countThreshold =
    Double.parseDouble(Settings.get(Settings.countThreshold));

  // data members
  private String outName;
  private Writer out;
  private CountsTable events;
  private CountsTable eventsSwap;
  private int counter;
  private int writeInterval = defaultWriteInterval;
  private boolean asynchronousWrite;
  private boolean strictWriteInterval;
  private boolean useCountThreshold;
  private volatile Thread dumper;
  // indicates to dumper thread that it is time to die
  private volatile boolean timeToDie;

  /**
   * Constructs a new event counts consumer.  Writing out consumed event
   * counts to the output file will occur asynchronously (that is, in
   * parallel) with the consumption of new event counts.  Writing
   * to the output file will occur periodicially, occurring sometime after
   * aggregate event counts have been collected from at least
   * {@link #getWriteInterval} sentences.
   */
  public EventCountsConsumer() {
    this(true, false);
  }

  /**
   * Constructs a new event counts consumer.  If the value of the
   * <code>asynchronousWrite</code> parameter is <code>true</code>, then
   * event counts will be aggregated in  an internal <code>CountsTable</code>,
   * and after event counts have been aggregated from at least
   * {@link #getWriteInterval} sentences, the events and their aggregate
   * counts will be passed off to a separate thread for appending to the
   * current output file.  This appending will happen in parallel with the
   * consumption of new events in a freshly-cleared <code>CountsTable</code>.
   *
   * @param asynchronousWrite indicates whether or not to append event counts
   * to the output file asynchronously (that is, in parallel)
   * with the consumption of new event counts
   */
  public EventCountsConsumer(boolean asynchronousWrite) {
    this(asynchronousWrite, false);
  }

  /**
   * Constructs a new event counts consumer.  If the value of the
   * <code>asynchronousWrite</code> parameter is <code>true</code>, then
   * event counts will be aggregated in  an internal <code>CountsTable</code>,
   * and at periodic intervals, the aggregated event counts will be passed
   * off to a separate thread for appending to the current output file.
   * This appending will happen in parallel with the
   * consumption of new events in a freshly-cleared <code>CountsTable</code>.
   * The interval between writing to the output file is determined by the
   * value of {@link #getWriteInterval}.  If <code>asynchronousWrite</code> is
   * <code>true</code> and the <code>strictWriteInterval</code> parameter is
   * <code>true</code>, then it is guaranteed that the internal counts table
   * will contain aggregate counts from exactly {@link #getWriteInterval}
   * sentences before being handed off to the output thread.  If the
   * <code>strictWriteInterval</code> parameter is <code>false</code>, then
   * the internal counts table will contain events from <i>at least</i>
   * {@link #getWriteInterval} sentences.  If the
   * <code>asynchronousWrite</code> parameter is <code>false</code> then
   * the value of this parameter will be ignored
   *
   * @param asynchronousWrite indicates whether or not to append event counts
   * to the output file asynchronously (that is, in parallel)
   * with the consumption of new event counts
   * @param strictWriteInterval if <code>asynchronousWrite</code> is
   * <code>true</code> and this parameter is <code>true</code>, then
   * it is guaranteed that the internal counts table will contain aggregate
   * counts from exactly {@link #getWriteInterval} sentences before
   * being handed off to the output thread; if this parameter is
   * <code>false</code>, then the internal counts table will contain events
   * from <i>at least</i> {@link #getWriteInterval} sentences; if
   * the <code>asynchronousWrite</code> parameter is <code>false</code> then
   * the value of this parameter will be ignored
   */
  public EventCountsConsumer(boolean asynchronousWrite,
			     boolean strictWriteInterval) {
    this.asynchronousWrite = asynchronousWrite;
    this.strictWriteInterval = strictWriteInterval;
    if (asynchronousWrite) {
      events = new CountsTableImpl();
      eventsSwap = new CountsTableImpl();
      timeToDie = false;
    }
  }

  public int getWriteInterval() { return writeInterval; }
  public void setWriteInterval(int writeInterval) {
    this.writeInterval = writeInterval;
  }

  public void useCountThreshold() {
    useCountThreshold = true;
  }
  public void dontUseCountThreshold() {
    useCountThreshold = false;
  }

  public void newFile(String inputFilename, String outputFilename) {
    if (asynchronousWrite) {
      synchronized (this) {
	if (dumper != null) {
	  timeToDie = true; // should have been set to true in processingComplete
	  notifyAll();
	}
      }
      if (dumper != null) {
	try { dumper.join(); }
	catch (InterruptedException ie) { System.err.println(ie); }
      }
      timeToDie = false;
      events.clear();
      eventsSwap.clear();
      counter = 0;
    }

    outName = outputFilename;
    setOutputWriter();

    if (asynchronousWrite) {
      dumper = new Thread(this, "Dumper for \"" + inputFilename + "\"");
      dumper.start();
    }
  }

  void setOutputWriter() {
    try {
      OutputStream os = new FileOutputStream(outName);
      if (outName.endsWith(".gz"))
	os = new GZIPOutputStream(os);
      out = new BufferedWriter(new OutputStreamWriter(os, Language.encoding()),
			       Constants.defaultFileBufsize);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(className +
			 ": error: couldn't create output file \"" +
			 outName + "\"");
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(className +
			 ": error: unsupported encoding: " +
			 Language.encoding());
    }
    catch (IOException ioe) {
      System.err.println(className +
			 ": error: trouble creating file \"" + outName + "\"" +
			 ioe);
    }
  }

  synchronized boolean timeToWrite() {
    return counter >= writeInterval;
  }

  public void consume(NumberedObject obj) {
    if (!obj.processed())
      return;
    if (asynchronousWrite)
      consumeForDumper(obj);
    else {
      try {
	CountsTable currCounts = (CountsTable)obj.get();
	if (useCountThreshold)
	  currCounts.removeItemsBelow(countThreshold);
	EventCountsWriter.outputEvents(currCounts, out);
	out.flush();
      }
      catch (IOException ioe) {
	System.err.println(className + ": error outputting events: " + ioe);
      }
    }
  }

  synchronized public void consumeForDumper(NumberedObject obj) {
    // note that this method is ONLY called if asynchronousWrite is true
    if (strictWriteInterval) {
      try {
	while (timeToWrite())
	  wait();
      }
      catch (InterruptedException ie) {}
    }

    CountsTable currEvents = (CountsTable)obj.get();
    Iterator it = currEvents.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      if (!useCountThreshold || count >= countThreshold)
	events.add(event, count);
    }
    counter++;
    if (timeToWrite())
      notifyAll();
  }

  /**
   * Indicates that there are no more sentences whose event counts are to be
   * consumed.  If this consumer was constructed to have asynchronous writing,
   * then any remaining aggregate counts in the internal
   * <code>CountsTable</code> will be appended to the output file before
   * this method exits.
   *
   * @param inputFilename the input file for which event counts have been
   * consumed
   * @param outputFilename the output file for consumed event counts
   */
  synchronized public void processingComplete(String inputFilename,
					      String outputFilename) {
    if (asynchronousWrite) {
      timeToDie = true;
      notifyAll();
    }
  }

  void writeOutput() {
    boolean keepLooping = true;
    boolean lastLoop = false;
    while (keepLooping) {
      synchronized (this) {
	try {
	  while (!timeToDie && !timeToWrite())
	    wait();
	}
	catch (InterruptedException ie) {
	  System.err.println(ie);
	}
	// while we still have lock, do the swap and reset counter
	eventsSwap.clear();
	CountsTable tmp = eventsSwap;
	eventsSwap = events;
	events = tmp;
	counter = 0;
	if (strictWriteInterval)
	  notifyAll();  // because of the call to wait() in consumeForDumper
      }

      // we no longer have lock, so write out eventsSwap with invocations
      // of consume method happening asynchronously
      try {
	EventCountsWriter.outputEvents(eventsSwap, out);
	out.flush();
      }
      catch (IOException ioe) {
	System.err.println(className + ": error outputting events: " + ioe);
      }
      if (timeToDie) {
	if (lastLoop || events.size() == 0)
	  keepLooping = false;
	else
	  lastLoop = true;
      }
    }
    try {
      out.close();
    }
    catch (IOException ioe) {
      System.err.println(className + ": error: couldn't close file \"" +
			 outName + "\"");
    }
  }

  public void run() {
    writeOutput();
  }
}
