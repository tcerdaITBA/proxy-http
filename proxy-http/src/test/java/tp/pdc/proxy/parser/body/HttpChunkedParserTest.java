package tp.pdc.proxy.parser.body;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.body.HttpChunkedParser;
import tp.pdc.proxy.properties.ProxyProperties;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class HttpChunkedParserTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private HttpChunkedParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	@Before
	public void setUp () throws Exception {
		parser = new HttpChunkedParser(false);
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished () throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "0007\r\n" + "hola co\r\n" + "08\r\n" + "mo te va\r\n" + "00000000\r\n" + "\r\n";

		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
	
	@Test
	public void testFinishedShortOutput () throws ParserFormatException, UnsupportedEncodingException {
		outputBuffer = ByteBuffer.allocate(1);
		String chunked = "000007\r\n" + "hola co\r\n" + "08\r\n" + "mo te va\r\n" + "00000000\r\n" + "\r\n";
		String actual = "";

		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		while (!parser.parse(inputBuffer, outputBuffer)) {
			assertFalse(parser.hasFinished());
			actual += new String(outputBuffer.array(), PROPERTIES.getCharset());
			outputBuffer.clear();
		}
		
		actual += new String(outputBuffer.array(), PROPERTIES.getCharset());
		
		assertTrue(parser.hasFinished());
		assertEquals(chunked, actual);
	}

	@Test
	public void testNotFinished () throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "7\r\n" + "hola co\r\n" + "8\r\n" + "mo te v";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertFalse(parser.hasFinished());
	}

	@Test
	public void testIncludeCRLF () throws ParserFormatException, UnsupportedEncodingException {
		String chunked =
			"7\r\n" + "hola co\r\n" + "000A\r\n" + "mo te va\r\n\r\n" + "0\r\n" + "\r\n";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testHexaSizeValues () throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "a\r\n" + "hola como \r\n" + "0e\r\n" + "andas en el di\r\n" + "11\r\n"
			+ "a de hoy, queria \r\n" + "1A\r\n" + "saber como vas con tu vida\r\n" + "A1\r\n"
			+ "desde que te fuiste..............................."
			+ ".................................................."
			+ ".................................................." + "..........!\r\n" + "0\r\n"
			+ "\r\n";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
