import org.apache.commons.validator.routines.UrlValidator
class Initialize{

    /**
     * [execute description]
     * @param  host                         [description]
     * @param  port                         [description]
     * @param  accessToken                  [description]
     * @return                              [description]
     * @throws IllegalArgumentException     [description]
     * @throws InternalServerErrorException [description]
     */
    CustomizationService execute(String host, int port, String accessToken)throws IllegalArgumentException, InternalServerErrorException{
        new CustomizationService(host, port, accessToken)
    }
}

/**
 * Provides methods for customization tasks
 */
class CustomizationService{

    String host, accessToken
    int port

    /**
     * Creates CustomizationService from the host, port number and access token
     * @param  host                         [description]
     * @param  null                         [description]
     * @param  accessToken                  [description]
     * @throws IllegalArgumentException     [description]
     * @throws InternalServerErrorException [description]
     */
    public CustomizationService(String host, int port = null, String accessToken) throws IllegalArgumentException, InternalServerErrorException{
        this.host = host
        this.port = port ?: 5000
        this.accessToken = accessToken
        String url = getURL()
        if(!isValidURL(url)){
            throw new IllegalArgumentException("Error: the host you have provided is not valid.")
        }
        ping() // verify access token
    }

    /**
     * Checks your installation by connecting to PIM.
     * @return pong on success, or a descriptive error on failure.
     * @throws IllegalArgumentException <br>
     * 401 Unauthorized - Occurs e.g. when the token you have provided is not valid. <br>
     * 404 Not Found - Occurs e.g. when the host you have provided is not valid.
     * @throws InternalServerErrorException<br>
     * 500 Internal Server Error - Occurs only on unknown errors. If you encounter a 500, this is most likely a bug. <br>
     * 580 PIM Access Denied - This occurs when the PIM user you are using is not allowed to perform an operation.<br>
     * 581 PIM Unreachable - The connection to PIM was unsuccessful, most likely because your configuration of the host is wrong.<br>
     * 582 PIM Internal Error Exception - The connection to PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.<br>
     * 583 PIM 404 - The connection to the PIM host was successful, but it returned a 404.
     */
    public def ping() throws IllegalArgumentException, InternalServerErrorException{
        URL url = getURL('ping')
        def response = url.getText()
        def jsonResponse = asJson(response)
        int status = ping.status

        if(status == 401){
            throw new IllegalArgumentException("Error $status: the token you have provided is not vali.")
        }else if(status > 500){
            throw new InternalServerErrorException("TODO")
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

    private def asJson(String jsonString){
        new JsonSlurper().parseText(jsonString)
    }
}
