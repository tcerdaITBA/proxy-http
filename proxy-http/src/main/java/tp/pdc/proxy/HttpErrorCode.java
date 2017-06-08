package tp.pdc.proxy;

import java.nio.charset.Charset;

public enum HttpErrorCode {
	BAD_REQUEST_400("400 Bad Request", "Request syntax errors"),
	NO_HOST_400("400 Bad Request", "Missing host in headers and URL"),
	UNRESOLVED_ADDRESS_400("400 Bad Request", "Host address could not be resolved"),
	BAD_HOST_FORMAT_400("400 Bad Request", "Invaild host format"),
	BAD_BODY_FORMAT_400("400 Bad Request", "Invalid body format"),
	LENGTH_REQUIRED_411("411 Length Required",
		"Missing valid content-length and transfer-encoding: chunked headers"),
	REQUEST_URI_TOO_LONG_414("414 Request URI too long", "Request URI too long"),
	HEADER_FIELD_TOO_LARGE_431("431 Request headers fields too large", "Header field too large"),
	TOO_MANY_HEADERS_NO_HOST_431("431 Request headers fields too large",
		"Header field too large and still no host found"),
	NOT_IMPLEMENTED_501("501 Method not implemented", "Method not implemented"),
	BAD_GATEWAY_502("502 Bad Gateway", "Failed to connect to server");

	private static final String SEPARATOR = ": ";

	private final byte[] response;
	private final String errorMessage;

	private HttpErrorCode (String errorCode, String body) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("HTTP/1.1 ").append(errorCode).append("\r\n")
			.append("Content-type: text/plain\r\n").append("Connection: close\r\n")
			.append("Content-length: ")
			.append(body.length() + errorCode.length() + SEPARATOR.length()).append("\r\n\r\n")
			.append(errorCode).append(SEPARATOR).append(body);

		Charset charset = ProxyProperties.getInstance().getCharset();
		this.response = stringBuilder.toString().getBytes(charset);
		this.errorMessage = body;
	}

	public byte[] getBytes () {
		return response;
	}

	public String getErrorMessage () {
		return errorMessage;
	}
}
