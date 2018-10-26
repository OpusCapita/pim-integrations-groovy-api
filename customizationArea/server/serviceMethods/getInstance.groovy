
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

    String host, accessToken
    int port

    /**
     * Creates CustomizationService from the host, port number and access token
     * @param host        Host url
     * @param port        Port
     * @param accessToken Valid access token
     */
    public CustomizationService(String host, int port, String accessToken){
        this.host = host
        this.port = port
        this.accessToken = accessToken
    }

    /**
     * Ping the PIT
     * @return             Ping response
     */
    public String ping(){
        URL url = getUrl('ping')
        def response = url.getText()
        response
    }

    URL getUrl(String method){
        String urlAsString = "$host:$port/api/$method/?token=$accessToken".toString()
        urlAsString.toURL()
    }
}
