package mysimpleagent.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.stream.Stream;

public class HttpValidator {
    private static final Logger logger = LoggerFactory.getLogger(HttpValidator.class.getSimpleName());

    public static <T> void throwIfNotOk(HttpResponse<T> response) {
        logger.atDebug()
                .addKeyValue("statusCode", response.statusCode())
                .addKeyValue("expected", "2XX")
                .log("status code: validating...");
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            //TODO improve the error message with the response body (check content-type to get the body right)
            String errMsg = String.format("HTTP Error: %d", response.statusCode());
            throw new RuntimeException(errMsg);
        }
        logger.atDebug().log("status code: validated");
    }
}
