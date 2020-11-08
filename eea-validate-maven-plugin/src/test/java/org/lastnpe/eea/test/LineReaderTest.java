package org.lastnpe.eea.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LineReader}.
 * 
 * @author Jason Faust
 *
 */
class LineReaderTest {

    @Test
    void testEOF() throws Exception {
        var data = "one\rtwo\nthree\r\nfour";

        try (var test = new LineReader(new StringReader(data))) {
            assertEquals("one\r", test.line());
            assertEquals("two\n", test.line());
            assertEquals("three\r\n", test.line());
            assertEquals("four", test.line());
            assertNull(test.line());
        }
    }

    @Test
    void testLF() throws Exception {
        var data = "one\rtwo\nthree\r\nfour\n";

        try (var test = new LineReader(new StringReader(data))) {
            assertEquals("one\r", test.line());
            assertEquals("two\n", test.line());
            assertEquals("three\r\n", test.line());
            assertEquals("four\n", test.line());
            assertNull(test.line());
        }
    }

    @Test
    void testCR() throws Exception {
        var data = "one\rtwo\nthree\r\nfour\r";

        try (var test = new LineReader(new StringReader(data))) {
            assertEquals("one\r", test.line());
            assertEquals("two\n", test.line());
            assertEquals("three\r\n", test.line());
            assertEquals("four\r", test.line());
            assertNull(test.line());
        }
    }

    @Test
    void testCRLF() throws Exception {
        var data = "one\rtwo\nthree\r\nfour\r\n";

        try (var test = new LineReader(new StringReader(data))) {
            assertEquals("one\r", test.line());
            assertEquals("two\n", test.line());
            assertEquals("three\r\n", test.line());
            assertEquals("four\r\n", test.line());
            assertNull(test.line());
        }
    }

}
