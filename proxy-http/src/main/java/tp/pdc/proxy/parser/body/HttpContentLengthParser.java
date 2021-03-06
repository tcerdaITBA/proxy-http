package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

/**
 * Body parser for Content length and l33t flag not activated
 */
public class HttpContentLengthParser implements HttpBodyParser {

	private int contentLength;

	public HttpContentLengthParser (int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {

		if (output.remaining() >= input.remaining() && input.remaining() <= contentLength) {
			contentLength -= input.remaining();
			output.put(input);
		} else if (output.remaining() < input.remaining() && output.remaining() <= contentLength) {
			lengthPut(input, output, output.remaining());
		} else if (output.remaining() >= input.remaining() && input.remaining() > contentLength) {
			lengthPut(input, output, contentLength);
		} else if (output.remaining() < input.remaining() && output.remaining() > contentLength) {
			lengthPut(input, output, contentLength);
		} else {
			throw new IllegalStateException("Parser in inconsistent state");
		}

		return hasFinished();
	}

	private void lengthPut (ByteBuffer input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
		contentLength -= length;
	}

	@Override
	public boolean hasFinished () {
		return contentLength == 0;
	}

}
