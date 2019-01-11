import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import javax.ws.rs.NotAuthorizedException

public class Initialize {
    /**
    * Initialize customizationService that provides several methods for customization
    * @param  host                         The PIT Host
    * @param  accessToken                  Accesstoken that is provided by the PIT
    * @example execute("http://your-host.de:5000", "api-token")
    * @throws IllegalArgumentException     Host or port is not valid.
     */
    CustomizationService execute(String host, String accessToken) throws IllegalArgumentException {
        new CustomizationService(host, accessToken)
    }
}


class CustomizationService {
    private String accessToken
    private RESTClient restClient
    private final static Integer TIMEOUT = new Integer(60000)

    private Closure pingPath = {-> "/api/ping"}

    private Closure productPath = {catalogId, productId -> "/api/catalog/$catalogId/product/$productId"}
    private Closure productAttributeValuesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/attributeValue"}
    private Closure productClassificationGroupsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/classificationGroup"}
    private Closure productAssortmentsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/contract"}
    private Closure productPricesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/price"}
    private Closure productRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/relation"}
    private Closure productReverseRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/reverseRelation"}
    private Closure productDocumentsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/document"}
    private Closure productVariantsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/variant"}

    private Closure generalClassificationPath = { -> "/api/classification"}
    private Closure classificationPath = {classificationId -> "${generalClassificationPath()}/$classificationId"}
    private Closure classificationGroupPath = {classificationId, classificationGroupId -> "/api/classification/$classificationId/classificationGroup/$classificationGroupId"}
    private Closure classificationGroupChildrenPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/classificationGroup"}
    private Closure classificationGroupAttributeValuesPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attributeValue"}
    private Closure productsByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/product"}
    private Closure classificationGroupsPath = {classificationId -> "${classificationPath(classificationId)}/classificationGroup"}

    private Closure generalAttributePath = { -> "/api/attribute"}
    private Closure attributePath = {attributeId -> "${generalAttributePath()}/$attributeId"}
    private Closure attributesByClassificationPath = {classificationId -> "${classificationPath(classificationId)}/attribute"}
    private Closure attributesByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attribute"}

    /**
     * Creates customizationService that provides several methods for customization
     * @param  host                         The PIT Host
     * @param  accessToken                  Accesstoken that is provided by the PIT
     * @throws IllegalArgumentException     Host or port is not valid.
     */
    public CustomizationService(String host, String accessToken)
    throws IllegalArgumentException {
        String url = host


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
     * @throws UnknownHostException<br>
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response ping() throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

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
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProduct(String catalogId, String productId, ArrayList<String> languageIds = [],
        ArrayList < String > exclude = [], ArrayList < String > include = [])
        throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        def query = [:]
        query.put('languageIds', languageIds.join(','))
        query.put('exclude', exclude.join(','))
        query.put('include', include.join(','))


        String path = productPath(catalogId, productId)

        Response response = restGet(path, query)

        return response
    }

    /**
     * Retrieve a classification
     * @param  classificationId             ClassificationId
     * @return classification
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassification(String classificationId, ArrayList<String> include = []) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException{

        def query = [:]
        String path = classificationPath(classificationId)

            query.put('include', include.join(','))

        Response response = restGet(path, query)

        return response
    }

    /**
     * Retrieve a classificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return classificationGroup
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroup(String classificationId, String classificationGroupId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve the attributeValues of a classificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return List of attributeValues
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupAttributeValues(String classificationId, String classificationGroupId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupAttributeValuesPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve a Subgroups of a ClassificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return List of classificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupChildren(String classificationId, String classificationGroupId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupChildrenPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all classificationGroup from a specific classification
     * @param  classificationId             ClassificationId
     * @return List of classificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupsByClassification(String classificationId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = classificationGroupsPath(classificationId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Attributes assigned to a Classification
     * @param  classificationId             ClassificationId
     * @return List of Attributes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAttributesByClassification(String classificationId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributesByClassificationPath(classificationId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Attributes assigned to a ClassificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return List of Attributes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAttributesByClassificationGroup(String classificationId, String classificationGroupId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributesByClassificationGroupPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all products assigned to a ClassificationGroup
     * @param  classificationId             ClassificationId
     * @param  classificationGroupId        ClassificationGroupId
     * @return List of Products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductsByClassificationGroup(String classificationId, String classificationGroupId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productsByClassificationGroupPath(classificationId, classificationGroupId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Classifications
     * @return List of all Classifications
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllClassifications() throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = generalClassificationPath()

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Attributes
     * @return List of all Attributes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllAttributes() throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = generalAttributePath()

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve an Attribute
     * @param  attributeId                  attributeId
     * @return attribute
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAttribute(String attributeId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = attributePath(attributeId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all attributeValues of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of ProductAttributeValues
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAttributeValues(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productAttributeValuesPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all ClassificationGroups of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of ClassificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductClassificationGroups(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productClassificationGroupsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all ContractetProducts of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of ContractetProducts
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAssortments(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productAssortmentsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all Prices of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of Prices
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductPrices(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productPricesPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all relations of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of relations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductRelations(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productRelationsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all reverse-relations of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of ReverseRelations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductReverseRelations(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productReverseRelationsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all documents of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of documents
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductDocuments(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productDocumentsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    /**
     * Retrieve all variants of a Product
     * @param  catalogId                    CatalogId
     * @param  productId                    ProductId
     * @return List of variants
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductVariants(String catalogId, String productId) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {

        String path = productVariantsPath(catalogId, productId)

        Response response = restGet(path)

        return response
    }

    private Response restGet(String path, LinkedHashMap query = [:]) {

        def response
        query.put('token', accessToken)

        try {
            response = restClient.get([path: path,
                contentType: ContentType.APPLICATION_JSON,
                query: query
            ])
        } catch (Exception e) {
            if(e instanceof HttpResponseException && e.response.data.status == 404){
                return new Response([])
            }
            handleException(e)
        }

        return new Response(response.data.result)
    }

    private static getMatchingHttpResponseException(error){
        def responseStatus = error.response.data.status
        def responseMessage = error.response.data.message
        String errorMessage = "Error $responseStatus: $responseMessage"
        switch(responseStatus){
        case 401:
            return new NotAuthorizedException(errorMessage)
            break;
        case 580:
            return new PIMAccessDeniedException(errorMessage)
            break;
        case 581:
            return new PIMUnreachableException(errorMessage)
            break;
        case 582: 
            return new PIMInternalErrorException(errorMessage)
        case {it >= 500}:
            return new GroovyAPIInternalErrorException(errorMessage)
            break;
        default:
            return error;
        break;
        }
    }
    private static void handleException(Exception e) throws NotAuthorizedException, GroovyAPIInternalErrorException, UnknownHostException, PIMAccessDeniedException, PIMUnreachableException, PIMInternalErrorException {
        switch (e.class) {
            case HttpResponseException:
                if (!e.response.data) {
                    throw new UnknownHostException("Host is not available. This might be due to invalid host or port.")
                }
                def error = getMatchingHttpResponseException(e)
                throw error
                break              

            case [UnknownHostException, HttpHostConnectException, ConnectTimeoutException]:
                throw new UnknownHostException("Host is not available. This might be due to invalid host or port. Reason: $e.message")
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
    private boolean isEmpty = false

    public Response(def value) {
        this.value = value
        isEmpty = value ? false : true
    }

    public def getValue(){
        value
    }

    public boolean getIsEmpty(){
        isEmpty
    }
}
/**
 * Internal Server Error - Occurs only on unknown errors in PIT. If you encounter an Internal Server Error, this is most likely a bug in PIT.
 */
class GroovyAPIInternalErrorException extends RuntimeException {
    public GroovyAPIInternalErrorException(String message) {
        super(message)
    }
}
/**
 * NotAuthorizedException - The token you have provided is not valid.
 */
class NotAuthorizedException extends RuntimeException {
    public NotAuthorizedException(String message) {
        super(message)
    }
}

/**
 * PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.
 */
class PIMAccessDeniedException extends RuntimeException {
    public PIMAccessDeniedException(String message) {
        super(message)
    }
}
/**
 * PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
 */
class PIMUnreachableException extends RuntimeException {
    public PIMUnreachableException(String message) {
        super(message)
    }
}
/**
 * PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
 */
class PIMInternalErrorException extends RuntimeException {
    public PIMInternalErrorException(String message) {
        super(message)
    }
}
/**
 * PIT Internal Error Exception - The connection between PIT and the Groovy API was successful, but PIT replied with a 500. This is most likely a bug in PIT.
 */
class PITInternalErrorException extends RuntimeException {
    public PITInternalErrorException(String message) {
        super(message)
    }
}
