import java.net.ConnectException
import groovy.json.JsonSlurper
import groovy.json.JsonParserType

/**
* Initializes an Instance of the CustomizationService
* @author  Dennis Brueseke
* @version 1.0
*/
class GetInstance{

    /**
     * [execute description]
     * @return [description]
     */
    CustomizationService execute(String host, int port, String accessToken){
        new CustomizationService(host,port,accessToken)
    }
}

/**
 * Provides methods for customization tasks
 */
class CustomizationService{

    private String host, accessToken
    private int port
    private final static String apiPath = 'api'

    /**
     * Creates CustomizationService from the host, port number and access token
     */
    public CustomizationService(String host, int port, String accessToken) throws ConnectException{
        boolean pitIsAvailable
        try{
            pitIsAvailable = ping(host,port,accessToken)
        }catch (Exception e){
            pitIsAvailable = false
            throw e
        }

        if(pitIsAvailable){
            this.host = host
            this.port = port
            this.accessToken = accessToken
        }else{
            throw new ConnectException ("Connection refused. Check host, port and accessToken for validity")
        }
    }

    /**
     * Ping the PIT
     * @param  host        Host URL where the PIT is running
     * @param  port        Port
     * @param  accessToken GeneratedPIT access token
     * @return             Connection is possible true/false
     */
    private static boolean ping(String host, int port, String accessToken){
        URL url = "${host}:${port}/${apiPath}/ping/?token=${accessToken}".toString().toURL()

        def response = url.getText()
        response = parseJSON(response)

        int status = response.status
        status in [200]
    }

    private static def parseJSON(String jsonText){
        def parser = new JsonSlurper().setType(JsonParserType.LAX)
        parser.parseText(jsonText)
    }
}
