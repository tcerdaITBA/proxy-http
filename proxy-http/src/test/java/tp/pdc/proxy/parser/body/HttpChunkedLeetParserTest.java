package tp.pdc.proxy.parser.body;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.body.HttpChunkedParser;
import tp.pdc.proxy.properties.ProxyProperties;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpChunkedLeetParserTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private HttpChunkedParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	@Before
	public void setUp () throws Exception {
		parser = new HttpChunkedParser(true);
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testLeet () throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "7\r\n" + "holi co\r\n" + "8\r\n" + "mo te va\r\n" + "0\r\n" + "\r\n";

		String expectedOutput =
			"7\r\n" + "h0l1 <0\r\n" + "8\r\n" + "m0 t3 v4\r\n" + "0\r\n" + "\r\n";

		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}

}
