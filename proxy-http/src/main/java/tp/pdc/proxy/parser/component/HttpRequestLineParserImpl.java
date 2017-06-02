package tp.pdc.proxy.parser.component;

import static tp.pdc.proxy.parser.utils.AsciiConstants.CR;
import static tp.pdc.proxy.parser.utils.AsciiConstants.LF;
import static tp.pdc.proxy.parser.utils.AsciiConstants.SP;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class HttpRequestLineParserImpl implements HttpRequestLineParser {

    private static final int METHOD_NAME_SIZE = ProxyProperties.getInstance().getMethodBufferSize();
    private static final int URI_HOST_SIZE = ProxyProperties.getInstance().getURIHostBufferSize();

    private RequestLineParserState state;
    private Method method;
    private byte[] hostValue;

    private int buffered = 0;

    private final ByteBuffer methodName, URIHostBuf;

    private HttpVersionParser versionParser;

    @Override public boolean readMinorVersion () {
        return versionParser.readMinorVersion();
    }

    @Override public boolean readMajorVersion () {
        return versionParser.readMajorVersion();
    }

    @Override public int getMajorHttpVersion () {
        return versionParser.getMajorHttpVersion();
    }

    @Override public int getMinorHttpVersion () {
        return versionParser.getMinorHttpVersion();
    }

    private enum RequestLineParserState {
        START, METHOD_READ, HTTP_VERSION, CR,
        URI_READ, HOST_PROTOCOL, URI_HOST_ADDR, URI_NO_HOST, URI_HOST_SLASH,
        READ_OK, ERROR,
    }

    public HttpRequestLineParserImpl () {
        versionParser = new HttpVersionParserImpl(CR.getValue());
        state = RequestLineParserState.START;
        methodName = ByteBuffer.allocate(METHOD_NAME_SIZE);
        URIHostBuf = ByteBuffer.allocate(URI_HOST_SIZE);
    }

    @Override public byte[] getHostValue () {
        if (!hasHost())
            throw new NoSuchElementException("Host not read");
        return hostValue;
    }

    @Override public boolean hasMethod () {
        return method != null;
    }
    
    @Override public Method getMethod() {
        if (!hasMethod())
            throw new NoSuchElementException("Method not read");
		return method;
	}


    @Override public boolean hasHost () {
        return hostValue != null;
    }

    @Override public boolean parse (ByteBuffer input, ByteBuffer output)
        throws ParserFormatException {
        while (input.hasRemaining() && output.hasRemaining() && output.remaining() > buffered) {
            byte c = input.get();

            switch (state) {
                case START:
                    if (ParseUtils.isAlphabetic(c)) {
                        state = RequestLineParserState.METHOD_READ;
                        saveMethodByte(c);
                    } else {
                        handleError("Error while parsing method");
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        saveMethodByte(c);
                    } else if (c == SP.getValue() && processMethod()) {
                        state = RequestLineParserState.URI_READ;
                        output.put(methodName).put(c);
                        buffered = 0;
                    } else {
                        handleError("Error while parsing method");
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
                        handleError("Error while parsing URI");
                    }
                    break;

                case URI_NO_HOST:
                    if (c == SP.getValue()) {
                        state = RequestLineParserState.HTTP_VERSION;
                        output.put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        output.put(c);
                    } else {
                        handleError("Error while parsing relative URI");
                    }
                    break;

                case HOST_PROTOCOL:
                    if (ParseUtils.isUriCharacter(c)) {
                        state = c != '/' ? state : RequestLineParserState.URI_HOST_SLASH;
                        output.put(c);
                    } else {
                        handleError("Error while parsing protocol");
                    }
                    break;

                case URI_HOST_SLASH:
                    if (c == '/') {
                        state = RequestLineParserState.URI_HOST_ADDR;
                        output.put(c);
                    } else {
                        handleError("Error while parsing relative URI");
                    }
                    break;

                case URI_HOST_ADDR:
                    if (c == SP.getValue() || c == '/') {
                        loadHostValue();
                        output.put(hostValue).put(c);
                        state = c == '/' ? RequestLineParserState.URI_NO_HOST :
                            RequestLineParserState.HTTP_VERSION;
                        buffered = 0;
                    } else if (ParseUtils.isUriCharacter(c)) {
                        saveHostByte(c);
                    } else {
                        handleError("Error while parsing URI address");
                    }
                    break;

                case HTTP_VERSION:
                    // Vuelve al byte que sacó
                    input.position(input.position() - 1);
                    if (versionParser.parse(input, output)) {
                        state = RequestLineParserState.CR;
                    }
                    break;

                case CR:
                    if (c == LF.getValue()) {
                        state = RequestLineParserState.READ_OK;
                        output.put(c);
                        return true;
                    } else {
                        handleError("Error while parsing CR first line");
                    }
                    break;

                default:
                    handleError("Error while parsing first line");
            }
        }

        if (output.remaining() <= buffered)
            output.limit(output.position()); // Así se simula que el buffer está lleno

        assertBufferCapacity(output);

        return false;
    }

    private void assertBufferCapacity(ByteBuffer buffer) {
        if (buffer.capacity() <= buffered)
            throw new IllegalArgumentException("Output buffer too small");
    }

    private void saveMethodByte(byte c) throws ParserFormatException {
        if (!methodName.hasRemaining())
            throw new ParserFormatException("Method name too long");
        methodName.put(c);
        buffered++;
    }

    private void saveHostByte(byte c) throws ParserFormatException {
        if (!URIHostBuf.hasRemaining())
            throw new ParserFormatException("Host too long", HttpErrorCode.REQUEST_URI_TOO_LONG_414);
        URIHostBuf.put(c);
        buffered++;
    }

    private void loadHostValue() {
        URIHostBuf.flip();
        hostValue = new byte[URIHostBuf.remaining()];
        URIHostBuf.get(hostValue);
    }

    private boolean processMethod() {
        int strLen = methodName.position();
        methodName.flip();
        method = Method.getByBytes(methodName, strLen);
        return method != null; // Valid method
    }

    private void handleError(String message) throws ParserFormatException {
        state = RequestLineParserState.ERROR;
        throw new ParserFormatException(message);
    }

    @Override public boolean hasFinished () {
        return state == RequestLineParserState.READ_OK;
    }

    @Override public void reset () {
        versionParser.reset();
        state = RequestLineParserState.START;
        methodName.clear();
        URIHostBuf.clear();
        method = null; 
        hostValue = null;
        buffered = 0;
    }
}
