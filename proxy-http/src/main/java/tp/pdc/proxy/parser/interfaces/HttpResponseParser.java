package tp.pdc.proxy.parser.interfaces;

/**
 * Created by Bianchi on 27/4/17.
 */
public interface HttpResponseParser extends Parser {

    int getStatusCode();

    boolean hasStatusCode();
}
