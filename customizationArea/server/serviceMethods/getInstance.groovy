import groovy.json.JsonSlurper
import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import java.util.concurrent.TimeoutException
class Initialize{

    /**
     * [execute description]
     * @param  host                         [description]
     * @param  port                         [description]
     * @param  accessToken                  [description]
     * @return                              [description]
     * @throws IllegalArgumentException     [description]
     */
    CustomizationService execute(String host, def port = null, String accessToken)throws IllegalArgumentException{
        if(port){
            new CustomizationService(host, port, accessToken)
        }else{
            new CustomizationService(host, accessToken)
        }
    }
}

/**
 * Provides methods for customization tasks
 */
class CustomizationService{

    String host, accessToken
    def port = 80
    /**
     * Creates CustomizationService from the host, port number and access token
     * @param  host                         [description]
     * @param  null                         [description]
     * @param  accessToken                  [description]
     * @throws IllegalArgumentException     [description]
     */
    public CustomizationService(String host, def port = null, String accessToken) throws IllegalArgumentException{
        this.host = host
        this.port = port ?: this.port
        this.accessToken = accessToken
        String url = getURL()
        if(!isValidURL(url)){
            throw new IllegalArgumentException("Error: the host url you have provided is not valid.")
        }
    }

    /**
     * Checks your installation by connecting to PIM.
     * @return pong on success, or a descriptive error on failure.
     * @throws UnauthorizedException<br>
     * 401 Unauthorized - Occurs e.g. when the token you have provided is not valid. <br>
     * @throws InternalServerErrorException<br>
     * 500 Internal Server Error - Occurs only on unknown errors. If you encounter a 500, this is most likely a bug.
     * @throws ServerTimeoutException<br>
     * 501 Server Timeout Error - Occurs when the PIT is not reachable. Check for valid host/port.
     * @throws UnknownHostException<br>
     * 502 Host Error: The host you provided is not reachable.
     */
    public def ping() throws UnauthorizedException, InternalServerErrorException, ServerTimeoutException, UnknownHostException{
        URL url = getURL('ping')
        def response
        try{
            response = url.getText([connectTimeout: 3000])
        }catch (SocketTimeoutException e){
            throw new ServerTimeoutException("Error 501: PIT is not reachable. Port or host you have provided might be not valid")
        }catch (UnknownHostException e){
            throw new UnknownHostException("Error 502: The host you provided is not reachable.")
        }catch (IOException e){
            throw new UnauthorizedException("Error 401: the access-token you have provided is not valid.")
        }

        def jsonResponse = asResultJson(response)
        int status = jsonResponse.status

        if(status > 500){
            throw new InternalServerErrorException("Internal Server Error 500: Occurs only on unknown errors. If you encounter a 500, this is most likely a bug.")
        }

        jsonResponse
    }

    private URL getURL(String method = null){
        method = method ? method + '/' : ''
        String urlAsString = "$host:$port/api/$method?token=$accessToken".toString()
        urlAsString.toURL()
    }

    private boolean isValidURL(String url){
        String[] schemes = ["http","https"]
        UrlValidator urlValidator = new UrlValidator(schemes)
        urlValidator.isValid(url)
    }

    private def asResultJson(String jsonString){
        def map = new JsonSlurper().parseText(jsonString)
        map.put('value',map.result)
        map.remove('result')
        map
    }
}

class UnauthorizedException extends IllegalArgumentException{
    public UnauthorizedException(String message){
        super(message)
    }
}

class ServerTimeoutException extends TimeoutException{
    public ServerTimeoutException(String message){
        super(message)
    }
}
