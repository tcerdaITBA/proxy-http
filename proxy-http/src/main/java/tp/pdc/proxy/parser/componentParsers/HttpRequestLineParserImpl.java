package tp.pdc.proxy.parser.componentParsers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.mainParsers.HttpRequestParserImpl;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;

public class HttpRequestLineParserImpl implements HttpRequestLineParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParserImpl.class);

    private RequestLineParserState state;
    private Method method;
    private byte[] hostValue;

    private final ByteBuffer methodName, URIHostBuf;

    private HttpVersionParser versionParser;

    private enum RequestLineParserState {
        START, METHOD_READ, HTTP_VERSION, CR,
        URI_READ, HOST_PROTOCOL, URI_HOST_ADDR, URI_NO_HOST, URI_HOST_SLASH,
        READ_OK, ERROR,
    }

    public HttpRequestLineParserImpl () {
        versionParser = new HttpVersionParserImpl(CR.getValue());
        state = RequestLineParserState.START;
        methodName = ByteBuffer.allocate(16); //TODO: capacity
        URIHostBuf = ByteBuffer.allocate(256);
    }

    @Override public byte[] getHostValue () {
        if (!hasHost())
            throw new NoSuchElementException("Host not read");
        return hostValue;
    }

    @Override public boolean hasMethod (Method method) {
        return method == this.method;
    }

    @Override public boolean hasHost () {
        return hostValue != null;
    }

    @Override public boolean parse (ByteBuffer input, ByteBuffer output)
        throws ParserFormatException {
        while (input.hasRemaining() && output.hasRemaining()) {
            byte c = input.get();

            switch (state) {
                case START:
                    if (ParseUtils.isAlphabetic(c)) {
                        state = RequestLineParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                    } else if (c == SP.getValue() && processMethod()) {
                        if (!handlePutMethod(c, input, output))
                            return false;
                    } else {
                        handleError();
                    }
                    break;

                case URI_READ:
                    if (c == '/') {
                        output.put(c);
                        state = RequestLineParserState.URI_NO_HOST;
                    } else if (Character.toLowerCase(c) == 'h') { // Has protocol so it has host.
                        output.put(c);
                        state = RequestLineParserState.HOST_PROTOCOL;
                    } else {
                        handleError();
                    }
                    break;

                case URI_NO_HOST:
                    if (c == SP.getValue()) {
                        state = RequestLineParserState.HTTP_VERSION;
                        output.put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case HOST_PROTOCOL:
                    if (ParseUtils.isUriCharacter(c)) {
                        state = c != '/' ? state : RequestLineParserState.URI_HOST_SLASH;
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case URI_HOST_SLASH:
                    if (c == '/') {
                        state = RequestLineParserState.URI_HOST_ADDR;
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case URI_HOST_ADDR:
                    if (c == SP.getValue() || c == '/') {
                        if (!handlePutHost(c, input, output))
                            return false;
                    } else if (ParseUtils.isUriCharacter(c)) {
                        URIHostBuf.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case HTTP_VERSION:
                    if (versionParser.parse(c, output)) {
                        state = RequestLineParserState.CR;
                    }
                    break;

                case CR:
                    if (c == LF.getValue()) {
                        state = RequestLineParserState.READ_OK;
                        output.put(c);
                        return true;
                    } else {
                        handleError();
                    }
                    break;

                default:
                    handleError();
            }
        }
        return false;
    }

    // TODO esta y handlePutHost repiten código
    private boolean handlePutMethod(byte c, ByteBuffer input, ByteBuffer output) {
        if (output.remaining() < methodName.limit() + 1) {
            input.position(input.position() - 1);
            return false;
        }

        state = RequestLineParserState.URI_READ;
        output.put(methodName).put(c);
        return true;
    }

    private boolean handlePutHost(byte c, ByteBuffer input, ByteBuffer output) {
        loadHostValue();

        if (output.remaining() < hostValue.length + 1) {
            input.position(input.position() - 1); // "Puts back the read byte".
            return false;
        }

        output.put(hostValue).put(c);
        state = c == '/' ? RequestLineParserState.URI_NO_HOST :
            RequestLineParserState.HTTP_VERSION;
        return true;
    }

    private void loadHostValue() {
        if (hostValue != null)
            return; // already loaded

        URIHostBuf.flip();
        hostValue = new byte[URIHostBuf.remaining()];
        URIHostBuf.get(hostValue);
    }

    private boolean processMethod () {
        if (method != null)
            return true; // Already loaded

        int strLen = methodName.position();
        methodName.flip();
        method = Method.getByBytes(methodName, strLen);
        LOGGER.debug("METHOD: {}", method);
        return method != null; // Valid method
    }

    private void handleError() throws ParserFormatException {
        state = RequestLineParserState.ERROR;
        throw new ParserFormatException("Error while parsing response first line");
    }

    @Override public boolean hasFinished () {
        return state == RequestLineParserState.READ_OK;
    }

    @Override public void reset () {
        versionParser.reset();
        state = RequestLineParserState.START;
        methodName.clear();
        URIHostBuf.clear();
        method = null; hostValue = null;
    }

    @Override
	public Method getMethod() {
		return method;
	}
}