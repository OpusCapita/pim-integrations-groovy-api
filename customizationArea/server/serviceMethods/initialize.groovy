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

    private Closure pingPath = {-> "/api/ping"}
    private Closure productPath = {catalogId, productId -> "/api/catalog/$catalogId/product/$productId"}
    private Closure generalClassificationPath = { -> "/api/classification"}
    private Closure generalAttributePath = { -> "/api/attribute"}
    private Closure classificationPath = {classificationId -> "${generalClassificationPath()}/$classificationId"}
    private Closure classificationGroupPath = {classificationId, classificationGroupId -> "/api/classification/$classificationId/classificationGroup/$classificationGroupId"}
    private Closure classificationGroupsPath = {classificationId -> "${classificationPath(classificationId)}/classificationGroup"}
    private Closure attributePath = {attributeId -> "${generalAttributePath()}/$attributeId"}
    private Closure attributesByClassificationPath = {classificationId -> "${classificationPath(classificationId)}/attribute"}
    private Closure attributesByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attribute"}

    /**
     * Creates customizationService that provides several methods for customization
     * @param  host                         The URL PIT runs on
     * @param  accessToken                  Accesstoken that is provided by the PIT
     * @param  port                         The port PIT runs on
     * @throws IllegalArgumentException     Host or port is not valid.
     */
    public CustomizationService(String host, String accessToken, def port = null)
    throws IllegalArgumentException {

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
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response ping() throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = pingPath()
        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve a product
     * @param  productId ProductId of the desired product
     * @param  catalogId CatalogId of the desired product
     * @param  languageIds   Optional - All language-specific fields will be filtered to only include languages with the matching languageIds. If not provided, all language-specific fields are returned in all languages.
     * @param  exclude   Optional - A list of field ids. If defined, the result will not include the defined fields. If not provided, all fields will be returned.
     * Available values : attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId, prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     * @param  include   Optional - A list of field ids. If defined, the result will include the defined fields.
     * Available values : attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId, prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     * @return           A Response that contains the product
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getProduct(String catalogId, String productId, ArrayList < String > languageIds = [],
        ArrayList < String > exclude = [], ArrayList < String > include = [])
        throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        def query = [: ]

        if (languageIds) {
            query.put('languageIds', languageIds.join(','))
        }
        if (exclude) {
            query.put('exclude', exclude.join(','))
        }
        if (include) {
            query.put('include', include.join(','))
        }

        String path = productPath(catalogId, productId)

        Response response = restGet(path, query)

        return response
    }
    /**
     * Retrieve a classification
     * @param  classificationId             ClassificationId
     * @return                              classification
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getClassification(String classificationId, ArrayList<String> include = []) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException{

        def query = [: ]
        String path = classificationPath(classificationId)

        if (include) {
            query.put('include', include.join(','))
        }

        Response response = restGet(path, query)

        return response
    }

    /**
     * Retrieve a classificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return                              classificationGroup
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getClassificationGroup(String classificationId, String classificationGroupId) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all classificationGroup from a specific classification
     * @param  classificationId             ClassificationId
     * @return                              List of classificationGroups
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getClassificationGroupsByClassification(String classificationId) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupsPath(classificationId)

        Response response = restGet(path)

        return response
    }


    /**
     * Retrieve all Attributes assigned to a Classification
     * @param  classificationId             ClassificationId
     * @return                              List of Attributes
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getAttributesByClassification(String classificationId) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributesByClassificationPath(classificationId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Attributes assigned to a Classification
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return                              List of Attributes
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getAttributesByClassificationGroup(String classificationId, String classificationGroupId) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributesByClassificationGroupPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Classifications
     * @return                              List of all Classifications
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getAllClassifications() throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = generalClassificationPath()

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Attributes
     * @return                              List of all Attributes
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getAllAttributes() throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = generalAttributePath()

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve an Attribute
     * @param  attributeId                  attributeId
     * @return                              attribute
     * @throws NotAuthorizedException       <br>
     * 401 NotAuthorizedException - The token you have provided is not valid.
     * @throws UnknownHostException         <br>
     * 402 Unknown Host Error: The host you have provided is not available.
     * @throws InternalServerErrorException <br>
     * 500 Internal Server Error - Occurs only on unknown errors in PIT. If you encounter a 500, this is most likely a bug in PIT.
     * @throws PIMAccessDeniedException     <br>
     * 580 PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.<br>
     * @throws PIMUnreachableException      <br>
     * 581 PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
     * @throws PIMInternalErrorException    <br>
     * 582 PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
     */
    public Response getAttribute(String attributeId) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributePath(attributeId)

        Response response = restGet(path)

        return response
    }


    private Response restGet(String path, LinkedHashMap query = [: ]) {

        def response
        query.put('token', accessToken)

        try {
            response = restClient.get([path: path,
                contentType: ContentType.APPLICATION_JSON,
                query: query
            ])
        } catch (Exception e) {
            if(e instanceof HttpResponseException && e.response.data.status == 404){
                return new Response([],404,true)
            }
            handleException(e)
        }

        return new Response(response.data.result,response.data.status,false)
    }

    private static void handleException(Exception e) throws NotAuthorizedException, InternalServerErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {
        switch (e.class) {
            case HttpResponseException:
                if (!e.response.data) {
                    throw new UnknownHostException("Error 402: Host is not available. This might be due to invalid host or port.")
                }

                def responseStatus = e.response.data.status
                def responseMessage = e.response.data.message
                String errorMessage = "Error $responseStatus: $responseMessage"

                if (responseStatus == 401) {
                    throw new NotAuthorizedException(errorMessage)
                } else if (responseStatus == 580) {
                    throw new PIMAccessDeniedException(errorMessage)
                } else if (responseStatus == 581) {
                    throw new PIMUnreachableException(errorMessage)
                } else if (responseStatus == 582) {
                    throw new PIMInternalErrorException(errorMessage)
                }else if (responseStatus >= 500) {
                    throw new InternalServerErrorException(errorMessage)
                }

                throw e

            case [UnknownHostException, HttpHostConnectException, ConnectTimeoutException]:
                throw new UnknownHostException("Error 402: Host is not available. This might be due to invalid host or port. Reason: $e.message")
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
    private def value
    private int status
    private boolean isEmpty = false

    public Response(def value, int status, boolean isEmpty) {
        this.value = value
        this.status = status
        this.isEmpty = isEmpty
    }

    public def getValue(){
        value
    }

    public int getStatus(){
        status
    }

    public boolean getIsEmpty(){
        isEmpty
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

class PIMAccessDeniedException extends RuntimeException {
    public PIMAccessDeniedException(String message) {
        super(message)
    }
}

class PIMUnreachableException extends RuntimeException {
    public PIMUnreachableException(String message) {
        super(message)
    }
}

class PIMInternalErrorException extends RuntimeException {
    public PIMInternalErrorException(String message) {
        super(message)
    }
}
