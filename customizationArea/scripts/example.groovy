import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

ctx = Holders.applicationContext
Log log = LogFactory.getLog(this.class)

String host = 'http://fn-pit.northeurope.cloudapp.azure.com'
String accessToken = 'this-is-a-sample-token'
int port = 5000

def customizationService = ctx.customizationService.initialize(host, accessToken, port)

try {
    def result = customizationService.ping()
    log.info 'Ping works!'
    log.info result.getValue() // 'pong'
} catch (Exception e) {
    log.error 'There was an error', e
    // Review the error on what went wrong.
    // For example, if InternalServerErrorException is thrown. An Error code and a detailed description is provided
    // See the Groovy API Doc for more details
}
