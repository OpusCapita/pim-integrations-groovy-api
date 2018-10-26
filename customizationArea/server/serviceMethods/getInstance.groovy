
class GetInstance{

    /**
     * [execute description]
     * @return [description]
     */
    CustomizationService execute(String host, int port, String accessToken){
        new CustomizationService(host, port, accessToken)
    }
}

/**
 * Provides methods for customization tasks
 */
class CustomizationService{

    private URL url
    private final static basePath = '/api/'

    /**
     * Creates CustomizationService from the host, port number and access token
     * @param host        Host url
     * @param port        Port
     * @param accessToken Valid access token
     */
    public CustomizationService(String host, int port, String accessToken){
        url = host.toURL
        url.port = port
        url.path = basePath
        url.query = 'token='+accessToken
    }

    /**
     * Ping the PIT
     * @return             Ping response
     */
    private static boolean ping(){
        url.path = basePath + 'ping/'
        String response = url.getText()
        url.path = basePath
        response
    }

}
