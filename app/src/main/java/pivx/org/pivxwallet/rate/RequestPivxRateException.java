package rext.org.rextwallet.rate;

/**
 * Created by furszy on 7/5/17.
 */
public class RequestRextRateException extends Exception {
    public RequestRextRateException(String message) {
        super(message);
    }

    public RequestRextRateException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestRextRateException(Exception e) {
        super(e);
    }
}
