import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import javax.ws.rs.NotAuthorizedException

/**
* Entry point to the PIM Api, to be used from scripts in the PIM customization area.
*/
public class PimApi {
   /**
   * This method is implicitly executed whenever you call <tt>ctx.customizationService.pimApi()</tt>.
   * Use the <tt>initialize</tt>-method of its return value to instantiate a new API object.
   * In other words: <tt>ctx.customizationService.pimApi().initialize(host, accessToken)</tt> calls the constructor of 
   * {@link PitGroovyApi} and returns the new object..
   */
    HashMap execute() throws IllegalArgumentException {
        [
          initialize: { String host, String accessToken ->
            new PitGroovyApi(host, accessToken)
          } 
        ]
    }
}

 /**
 * The main API object.
 */
class PitGroovyApi {
    private String accessToken
    private RESTClient restClient
    private final static Integer TIMEOUT = new Integer(60000)

    private Closure pingPath = {-> "/api/ping"}

    private Closure productPath = {catalogId, productId -> "/api/catalog/$catalogId/product/$productId"}
    private Closure productAttributeValuesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/attributeValue"}
    private Closure productClassificationGroupsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/classificationGroup"}
    private Closure productAssortmentPath = {catalogId, productId -> "${productPath(catalogId, productId)}/assortment"}
    private Closure productPricesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/price"}
    private Closure productRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/relation"}
    private Closure productReverseRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/reverseRelation"}
    private Closure productDocumentsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/document"}
    private Closure productVariantsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/variant"}

    private Closure generalClassificationPath = { -> "/api/classification"}
    private Closure classificationPath = {classificationId -> "${generalClassificationPath()}/$classificationId"}
    private Closure classificationGroupPath = {classificationId, classificationGroupId -> "/api/classification/$classificationId/classificationGroup/$classificationGroupId"}
    private Closure classificationGroupSubgroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/classificationGroup"}
    private Closure classificationGroupAttributeValuesPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attributeValue"}
    private Closure productsByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/product"}
    private Closure classificationGroupsPath = {classificationId -> "${classificationPath(classificationId)}/classificationGroup"}

    private Closure generalAttributePath = { -> "/api/attribute"}
    private Closure attributePath = {attributeId -> "${generalAttributePath()}/$attributeId"}
    private Closure attributesByClassificationPath = {classificationId -> "${classificationPath(classificationId)}/attribute"}
    private Closure attributesByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attribute"}

    private Closure generalCatalogsPath = { -> "/api/catalog"}
    private Closure catalogPath = {catalogId -> "${generalCatalogsPath()}/$catalogId"}

    private Closure generalContractsPath = { -> "/api/contract"}
    private Closure contractPath = {contractId -> "${generalContractsPath()}/$contractId"}
    
    private Closure generalSuppliersPath = { -> "/api/supplier"}
    private Closure supplierPath = {supplierId -> "${generalSuppliersPath()}/$supplierId"}

    private Closure generalManufacturersPath = { -> "/api/manufacturer"}
    private Closure manufacturerPath = {manufacturerId -> "${generalManufacturersPath()}/$manufacturerId"}
        
    private Closure generalAttributeSectionPath = { -> "/api/attributeSection"}
    private Closure attributeSectionPath = {attributeSectionId -> "${generalAttributeSectionPath()}/$attributeSectionId"}

    /**
     * Creates a new API object with the given url and access token.
     * @param  url The URL to the PIT installation, for example: <tt>http://example.com:5000</tt>
     * @param  accessToken  The access token which can be configured within PIT.
     * @throws IllegalArgumentException    
     */
    public PitGroovyApi(String url, String accessToken)
    throws IllegalArgumentException {
    
        if (!isValidURL(url)) {
            throw new IllegalArgumentException("Error: url is not valid.")
        }
        this.accessToken = accessToken

        restClient = new RESTClient(url)
        restClient.client.getParams().setParameter("http.socket.timeout", TIMEOUT)
        restClient.client.getParams().setParameter("http.connection.timeout", TIMEOUT)
    }

    /**
     * Checks your installation.
     * @return On success, the <tt>Response</tt> simply contains <tt>pong</tt>. On failure, an exception is thrown which should point you in the right direction.
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response ping() {
        String path = pingPath()
        restGet(path)
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
        ArrayList < String > exclude = [], ArrayList < String > include = []) {

        def query = [:]
        query.put('languageIds', languageIds.join(','))
        query.put('exclude', exclude.join(','))
        query.put('include', include.join(','))

        String path = productPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve a classification
     * @return classification
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassification(String classificationId, ArrayList<String> include = [])  {
        def query = [:]
        String path = classificationPath(classificationId)
        query.put('include', include.join(','))
        restGet(path, query)
    }

    /**
     * Retrieve a classificationGroup
     * @return classificationGroup
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroup(String classificationId, String classificationGroupId) {
        String path = classificationGroupPath(classificationId, classificationGroupId)
        restGet(path)
    }

    /**
     * Retrieve the attributeValues of a classificationGroup
     * @return List of attributeValues
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupAttributeValues(String classificationId, String classificationGroupId) {
        String path = classificationGroupAttributeValuesPath(classificationId, classificationGroupId)
        restGet(path)
    }

    /**
     * Retrieve a Subgroups of a ClassificationGroup
     * @return List of classificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupSubgroup(String classificationId, String classificationGroupId) {
        String path = classificationGroupSubgroupPath(classificationId, classificationGroupId)
        restGet(path)
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
    public Response getClassificationGroupsByClassification(String classificationId) {
        String path = classificationGroupsPath(classificationId)
        restGet(path)
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
    public Response getAttributesByClassification(String classificationId) {
        String path = attributesByClassificationPath(classificationId)
        restGet(path)
    }

    /**
     * Retrieve all Attributes assigned to a ClassificationGroup
     * @return List of Attributes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAttributesByClassificationGroup(String classificationId, String classificationGroupId) {
        String path = attributesByClassificationGroupPath(classificationId, classificationGroupId)
        restGet(path)
    }

    /**
     * Retrieve all products assigned to a ClassificationGroup
     * @return List of Products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductsByClassificationGroup(String classificationId, String classificationGroupId) {
        String path = productsByClassificationGroupPath(classificationId, classificationGroupId)
        restGet(path)
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
    public Response getAllClassifications() {
        String path = generalClassificationPath()
        restGet(path)
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
    public Response getAllAttributes() {
        String path = generalAttributePath()
        restGet(path)
    }

    /**
     * Retrieve an Attribute
     * @param  attributeId attributeId
     * @return attribute
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAttribute(String attributeId) {
        String path = attributePath(attributeId)
        restGet(path)
    }

    /**
     * Retrieve all attributeValues of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of ProductAttributeValues
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAttributeValues(String catalogId, String productId) {
        String path = productAttributeValuesPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all ClassificationGroups of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of ClassificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductClassificationGroups(String catalogId, String productId) {
        String path = productClassificationGroupsPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all Assortments of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of Assortments
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAssortments(String catalogId, String productId) {
        String path = productAssortmentPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all Prices of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of Prices
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductPrices(String catalogId, String productId) {
        String path = productPricesPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all relations of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of relations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductRelations(String catalogId, String productId) {
        String path = productRelationsPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all reverse-relations of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of ReverseRelations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductReverseRelations(String catalogId, String productId) {
        String path = productReverseRelationsPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all documents of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of documents
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductDocuments(String catalogId, String productId) {
        String path = productDocumentsPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all variants of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @return List of variants
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductVariants(String catalogId, String productId) {
        String path = productVariantsPath(catalogId, productId)
        restGet(path)
    }

    /**
     * Retrieve all Catalogs
     * @return List of all Catalogs
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllCatalogs() {
        String path = generalCatalogsPath()
        restGet(path)
    }

    /**
     * Retrieve an Catalog
     * @param  catalogId catalogId
     * @return attribute
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getCatalog(String catalogId) {
        String path = catalogPath(catalogId)
        restGet(path)
    }

    /**
     * Retrieve all Contracts
     * @return List of all Contracts
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllContracts() {
        String path = generalContractsPath()
        restGet(path)
    }

    /**
     * Retrieve a Contract
     * @param  contractId contractId
     * @return contract
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getContract(String contractId) {
        String path = contractPath(contractId)
        restGet(path)
    }
/**
     * Retrieve a Supplier
     * @param  supplierId supplierId
     * @return supplier
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getSupplier(String supplierId) {
        String path = supplierPath(supplierId)
        restGet(path)
    }
    /**
     * Retrieve all Suppliers
     * @return List of all Suppliers
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllSuppliers() {
        String path = generalSuppliersPath()
        restGet(path)
    }


    /**
     * Retrieve all Manufacturers
     * @return List of all Manufacturers
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllManufacturers() {
        String path = generalManufacturersPath()
        restGet(path)
    }

    /**
     * Retrieve a Manufacturer
     * @param  manufacturerId manufacturerId
     * @return manufacturer
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getManufacturer(String manufacturerId) {
        String path = manufacturerPath(manufacturerId)
        restGet(path)
    }

    /**
     * Retrieve all AttributeSections
     * @return List of all AttributeSections
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllAttributeSections() {
        String path = generalAttributeSectionPath()
        restGet(path)
    }

    /**
     * Retrieve an AttributeSection
     * @param  attributeSectionId attributeSectionId
     * @return attributeSection
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getAttributeSection(String attributeSectionId) {
        String path = attributeSectionPath(attributeSectionId)
        restGet(path)
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
        case 500: 
            return new PITInternalErrorException(errorMessage)
            break;
        case 580:
            return new PIMAccessDeniedException(errorMessage)
            break;
        case 581:
            return new PIMUnreachableException(errorMessage)
            break;
        case 582: 
            return new PIMInternalErrorException(errorMessage)
            break;
        case {it > 500}:
            return new GroovyAPIInternalErrorException(errorMessage)
            break;
        default:
            return error;
        break;
        }
    }
    private static void handleException(Exception e) {
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
class PimApiException extends RuntimeException {
    public PimApiException(String message) {
        super(message)
    }
}

/**
 * Internal Server Error - Occurs only on unknown errors in PIT. If you encounter an Internal Server Error, this is most likely a bug in PIT.
 */
class GroovyAPIInternalErrorException extends PimApiException{
    public GroovyAPIInternalErrorException(String message) {
        super(message)
    }
}
/**
 * NotAuthorizedException - The token you have provided is not valid.
 */
class NotAuthorizedException extends PimApiException{
    public NotAuthorizedException(String message) {
        super(message)
    }
}

/**
 * PIM Access Denied - This happens if the PIM user configured in the PIT is not authorized to perform an operation.
 */
class PIMAccessDeniedException extends PimApiException{
    public PIMAccessDeniedException(String message) {
        super(message)
    }
}
/**
 * PIM Unreachable - The connection between PIT and PIM was unsuccessful, most likely because your configuration of the host is wrong.
 */
class PIMUnreachableException extends PimApiException{
    public PIMUnreachableException(String message) {
        super(message)
    }
}
/**
 * PIM Internal Error Exception - The connection between PIT and PIM was successful, but PIM replied with a 500. This is most likely a bug in PIM.
 */
class PIMInternalErrorException extends PimApiException{
    public PIMInternalErrorException(String message) {
        super(message)
    }
}
/**
 * PIT Internal Error Exception - The connection between PIT and the Groovy API was successful, but PIT replied with a 500. This is most likely a bug in PIT.
 */
class PITInternalErrorException extends PimApiException{
    public PITInternalErrorException(String message) {
        super(message)
    }
}
