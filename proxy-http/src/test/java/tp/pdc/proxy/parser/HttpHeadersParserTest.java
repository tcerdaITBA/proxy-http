package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class HttpHeadersParserTest {

    String headers;
    ByteBuffer inputBuffer, outputBuffer;
    HttpHeaderParser parser;

    @Before
    public void init(){
        headers = "Host: google.com\r\n"
                + "user-agent: internet explorer 2\r\n"
                + "connection: close\r\n"
                + "transfer-encoding: chunked\r\n"
                + "connection-no: close\r\n"
                + "transfer-size: chunked\r\n\r\n";
        parser = new HttpHeadersParserImpl();
        outputBuffer = ByteBuffer.allocate(8000);
    }

    @Test
    public void parseTest() throws ParserFormatException {
        inputBuffer = ByteBuffer.wrap(headers.getBytes());
        assertTrue(parser.parse(inputBuffer,outputBuffer));
    }

    @Test
    public void RelevantHeadersTest() throws ParserFormatException {
        inputBuffer = ByteBuffer.wrap(headers.getBytes());
        outputBuffer.clear();
        parser.parse(inputBuffer,outputBuffer);
        assertTrue(parser.hasRelevantHeaderValue(Header.HOST));
        assertTrue(parser.hasRelevantHeaderValue(Header.CONNECTION));
        assertTrue(parser.hasRelevantHeaderValue(Header.TRANSFER_ENCODING));
        assertFalse(parser.hasRelevantHeaderValue(Header.CONTENT_LENGTH));

        assertArrayEquals("google.com".getBytes(), parser.getRelevantHeaderValue(Header.HOST));
        assertArrayEquals("close".getBytes(), parser.getRelevantHeaderValue(Header.CONNECTION));
        assertArrayEquals("chunked".getBytes(), parser.getRelevantHeaderValue(Header.TRANSFER_ENCODING));
    }
}