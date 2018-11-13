import groovy.json.JsonSlurper
import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import java.util.concurrent.TimeoutException
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import javax.ws.rs.NotAuthorizedException
import groovy.json.internal.LazyMap
class Initialize {
    /**
     * [execute description]
     * @param  host                         [description]
     * @param  port                         [description]
     * @param  accessToken                  [description]
     * @return                              [description]
     * @throws IllegalArgumentException     [description]
     */
    CustomizationService execute(String host, String accessToken, def port = null) throws IllegalArgumentException {
        new CustomizationService(host, accessToken, port)
    }
}

/**
 * Provides methods for customization tasks
 */
class CustomizationService {

    String accessToken
    RESTClient restClient
    final static Integer TIMEOUT = new Integer(1000)

    /**
     * Creates customizationService that provides several methods for customization
     * @param  host                         The URL PIT runs on
     * @param  accessToken                  Accesstoken that is provided by the PIT
     * @param  port                         The port PIT runs on
     * @throws IllegalArgumentException     Host or port is not valid.
     */
    public CustomizationService(String host, String accessToken, def port = null) throws IllegalArgumentException {
        String portString = port ? ":$port" : ''
        String url = host + portString

        if (!isValidURL(url)) {
            throw new IllegalArgumentException("Error: host or port is not valid.")
        }

        this.accessToken = accessToken
        restClient = new RESTClient(url)
        restClient.client.getParams().setParameter("http.socket.timeout", TIMEOUT)
        restClient.client.getParams().setParameter("http.connection.timeout", TIMEOUT)
    }

    /**
     * Checks your installation by connecting to PIM.
     * @return pong on success, or a descriptive error on failure.
     * @throws NotAuthorizedException<br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException<br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException<br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.<br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.<br>
     * 583 PIM 404 - The connection between PIT and the PIM host was successful, but PIM returned a 404.
     */
    public def ping() throws NotAuthorizedException, InternalServerErrorException, UnknownHostException {
        def response

        try {
            response = restClient.get([path: '/api/ping',
                contentType: ContentType.APPLICATION_JSON,
                query: [
                    token: accessToken
                ]
            ])
        } catch (HttpResponseException e) {
            if (!e.response.data) {
                throw new UnknownHostException("Error 402: Host is not available this might be due to invalid host or port.")
            }

            def responseStatus = e.response.data.status
            def responseMessage = e.response.data.message
            String errorMessage = "Error $responseStatus: $responseMessage"

            if (responseStatus == 401) {
                throw new NotAuthorizedException(errorMessage)
            } else if (responseStatus >= 500) {
                throw new InternalServerErrorException(errorMessage)
            } else {
                throw e
            }
        } catch (UnknownHostException | HttpHostConnectException | ConnectTimeoutException e) {
            throw new UnknownHostException("Error 402: Host is not available this might be due to invalid host or port. Reason: $e.message")
        }

        new Response(response.data)
    }

    private boolean isValidURL(String url) {
        String[] schemes = ["http", "https"]
        UrlValidator urlValidator = new UrlValidator(schemes)
        urlValidator.isValid(url)
    }

}

class Response {
    private LazyMap json

    public Response(LazyMap json) {
        this.json = json
    }

    public def getValue() {
        json.result
    }

    public def getStatus() {
        json.status
    }

}

class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message)
    }
}

class NotAuthorizedException extends RuntimeException {
    public NotAuthorizedException(String message) {
        super(message)
    }
}
