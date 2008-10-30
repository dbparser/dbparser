package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.tools.ant.types.Parameter;

public final class ConcatReader extends BaseParamFilterReader
    implements ChainableReader {

    private File before;
    private File after;

    Reader beforeReader = new EmptyReader();
    Reader afterReader = new EmptyReader();

    public ConcatReader() {
        super();
    }

    public ConcatReader(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        ch = beforeReader.read();
        if (ch == -1) {
            ch = super.read();
        }
        if (ch == -1) {
            ch = afterReader.read();
        }

        return ch;
    }

    public final void setBefore(final File before) {
        this.before = before;
    }

    public final File getBefore() {
        return before;
    }

    public final void setAfter(final File after) {
        this.after = after;
    }

    public final File getAfter() {
        return after;
    }

    public final Reader chain(final Reader rdr) {
        ConcatReader newFilter = new ConcatReader(rdr);
        newFilter.setBefore(getBefore());
        newFilter.setAfter(getAfter());
        newFilter.setInitialized(true);
        return newFilter;
    }

    private final void initialize() throws IOException {
        // get parameters
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if ("before".equals(params[i].getName())) {
                    before = new File(params[i].getValue());
                    continue;
                }
                if ("after".equals(params[i].getName())) {
                    after = new File(params[i].getValue());
                    continue;
                }
            }
        }
        if (before!=null) {
            beforeReader = new BufferedReader(new FileReader(before));
        }
        if (after!=null) {
            afterReader = new BufferedReader(new FileReader(after));
        }
   }

   private class EmptyReader extends Reader {
       public int read(char[] ch, int i1, int i2) { return -1; }
       public void close() {}
   }

}
