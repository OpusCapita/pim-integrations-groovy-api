import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import javax.ws.rs.NotAuthorizedException
import groovy.json.internal.LazyMap

public class Initialize {
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
    private String accessToken
    private RESTClient restClient
    private final static Integer TIMEOUT = new Integer(1000)

    private Closure pingPath = { -> "/api/ping"}
    private Closure productPath = {catalogId, productId -> "/api/catalog/$catalogId/product/$productId"}

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
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response ping() throws NotAuthorizedException, InternalServerErrorException, UnknownHostException {
        def response

        try {
            response = restClient.get([path: pingPath(),
                contentType: ContentType.APPLICATION_JSON,
                query: [
                    token: accessToken
                ]
            ])
        } catch (Exception e) {
            handleException(e)
        }

        new Response(response.data)
    }

    /**
     * Creates a product for data consumption
     * @param  productId ProductId of the desired product
     * @param  catalogId CatalogId of the desired product
     * @param  languageIds   Optional - All language-specific fields will be filtered to only include languages with the matching languageIds. If not provided, all language-specific fields are returned in all languages.
     * @param  exclude   Optional - A list of field ids. If defined, the result will not include the defined fields. If not provided, all fields will be returned.
     * Available values : attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId,
     * prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     *
     * @param  include   Optional - A list of field ids. If defined, the result will include the defined fields.
     * Available values : attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId,
     * prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     *
     * @return           A Response that contains the product
     * @throws NotAuthorizedException<br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException<br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException<br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.<br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getProduct(String catalogId, String productId, ArrayList <String> languageIds = [], ArrayList <String> exclude = [] , ArrayList <String> include = []) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException {

        def response
        def query = [token: accessToken]

        if (languageIds) {
            query.put('languageIds', languageIds.join(','))
        }
        if (exclude) {
                query.put('exclude', exclude.join(','))
        }
        if (include) {
            query.put('include', include.join(','))
        }

        String path = productPath(catalogId,productId)

        try {
            response = restClient.get([path: path,
                contentType: ContentType.APPLICATION_JSON,
                query: query
            ])
        } catch (Exception e) {
            if(e instanceof HttpResponseException){
                if(e.response.data.status == 404){
                    throw new BadRequestException("Error 404: No such Product found [catalogId:$catalogId, productId:$productId]")
                }
            }

            handleException(e)
        }

        new Response(response.data)
    }

    private static void handleException(Exception e) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException {
        switch (e.class) {
            case HttpResponseException:
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
                }

                throw e

            case [UnknownHostException, HttpHostConnectException, ConnectTimeoutException]:
                throw new UnknownHostException("Error 402: Host is not available this might be due to invalid host or port. Reason: $e.message")
            default:
                throw e
        }
    }

    private boolean isValidURL(String url) {
        String[] schemes = ["http", "https"]
        UrlValidator urlValidator = new UrlValidator(schemes)
        urlValidator.isValid(url)
    }
}

class Response {
    private LazyMap json
    private boolean isEmpty

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

class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message)
    }
}
