package danbikel.parser;

import danbikel.util.*;
import danbikel.switchboard.*;
import java.io.*;
import java.util.*;

public class EventCountsWriter extends PrintWriter implements ObjectWriter {

  public EventCountsWriter(OutputStream os) throws IOException {
    super(new OutputStreamWriter(os));
  }

  public EventCountsWriter(OutputStream os, String encoding, int bufSize)
    throws IOException {
    super(new BufferedWriter(new OutputStreamWriter(os, encoding),
			     bufSize));
  }

  public EventCountsWriter(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    super(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append),
						    encoding),
			     bufSize));
  }

  public void writeObject(Object obj) throws IOException {
    outputEvents((CountsTable)obj, this);
    flush();
  }

  public static void outputEvents(CountsTable events, Writer out)
    throws IOException {
    Iterator it = events.entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      TrainerEvent event = (TrainerEvent)entry.getKey();
      double count = entry.getDoubleValue();
      String name = null;
      if (event instanceof HeadEvent)
        name = Trainer.headEventSym.toString();
      else if (event instanceof ModifierEvent)
        name = Trainer.modEventSym.toString();
      if (name != null) {
        out.write("(");
        out.write(name);
        out.write(" ");
        out.write(event.toString());
        out.write(" ");
        out.write(String.valueOf(count));
        out.write(")\n");
      }
    }
  }
}