package org.lastnpe.eea.test;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * Simple wrapper around a {@link Reader} to preserve the EOL marks when doing
 * line orientated reading.
 * 
 * @author Jason Faust
 *
 */
public class LineReader implements AutoCloseable {

    private PushbackReader reader;

    public LineReader(Reader reader) {
        this.reader = new PushbackReader(reader);
    }

    public String line() throws IOException {
        var sb = new StringBuilder();
        while (true) {
            int c = reader.read();
            // EOF
            if (c == -1) {
                return sb.length() == 0 ? null : sb.toString();
            }
            sb.append((char) c);
            if (c == '\r') {
                int d = reader.read();
                if (d != -1) {
                    if (d == '\n') {
                        sb.append((char) d);
                    } else {
                        reader.unread(d);
                    }
                }
                return sb.toString();
            }
            if (c == '\n') {
                return sb.toString();
            }
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
