package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;
import java.io.*;

public class PrintDisn {
  private PrintDisn() {}

  public static void printLogProbDisn(PrintWriter writer, ModelCollection mc,
                                      Model model, int level, Event hist,
                                      Set futures, Transition tmpTrans) {
    Transition trans = tmpTrans;
    trans.setHistory(hist);
    Iterator it = futures.iterator();
    for (int idx = 0; it.hasNext(); idx++) {
      Event future = (Event)it.next();
      trans.setFuture(future);
      double logProb = model.estimateLogProbUsingPrecomputed(trans, level);
      if (logProb > Constants.logOfZero) {
        // if the future is a word symbol, then then this will get a count
        // for it; otherwise, wordFreq will simply be zero
        double wordFreq = mc.vocabCounter().count(((SexpEvent)future).toSexp());
        writer.println(future + "\t" + wordFreq + "\t" + logProb);
      }
    }
  }

  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println("error: need five arguments");
      System.err.println("usage: <derived data filename> <back-off level> " +
                         "<structure class name>\n\t<event> <output filename>");
      System.exit(1);
    }


    String mcFilename = args[0];
    int level = Integer.parseInt(args[1]);
    String structureName = args[2];
    String eventStr = args[3];
    String outputFilename = args[4];


    try {
      SexpList list = Sexp.read(eventStr).list();
      boolean hasSubcat = list.length() > 1;
      SexpEvent hist = hasSubcat ? new SexpSubcatEvent() : new SexpEvent();
      hist.setSexp(list.get(0));
      if (hasSubcat)
        hist.add(Subcats.get(list.listAt(1)));

      System.err.println("history context: " + hist);

      // create output file
      OutputStream os = new FileOutputStream(outputFilename);
      OutputStreamWriter osw = new OutputStreamWriter(os, Language.encoding());
      PrintWriter writer = new PrintWriter(new BufferedWriter(osw));

      // set up data structures
      Set futures = new HashSet();
      Transition tmpTrans = new Transition(null, null);

      ModelCollection mc = Trainer.loadModelCollection(mcFilename);
      Iterator models = mc.modelList().iterator();
      while (models.hasNext()) {
        Model model = (Model)models.next();
        int numModels = model.numModels();
        for (int i = 0; i < numModels; i++) {
          Model ithModel = model.getModel(i);
          //writeModelStats(ithModel);
          String structureClassName =
            ithModel.getProbStructure().getClass().getName();
          if (structureName.equals(structureClassName)) {
            AnalyzeDisns.getFutures(futures, ithModel, level);
            System.err.println("Writing distribution to file \"" +
                               outputFilename + "\".");
            printLogProbDisn(writer, mc, ithModel, level, hist, futures,
                             tmpTrans);
          }
        }
      }
      writer.flush();
      writer.close();
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }
}
