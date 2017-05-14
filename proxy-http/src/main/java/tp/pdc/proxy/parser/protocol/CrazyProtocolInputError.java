package tp.pdc.proxy.parser.protocol;

import tp.pdc.proxy.ProxyProperties;

public enum CrazyProtocolInputError {

	NO_MATCH("[NO_MATCH]"),
	//if one of the following codes are generated the request is considered invalid -> Goodbye!
	NOT_VALID("[NOT_VALID]"),
	TOO_LONG("[TOO_LONG]");

	private String output;
	private byte[] outputBytes;

	CrazyProtocolInputError(String output) {
		this.output = output;
		outputBytes = output.getBytes(ProxyProperties.getInstance().getCharset());
	}

	@Override
	public String toString() {
		return output;
	}
	
	public byte[] getBytes() { return outputBytes; }
}
