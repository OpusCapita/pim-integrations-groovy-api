import org.apache.commons.validator.routines.UrlValidator
import javax.ws.rs.InternalServerErrorException
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import groovyx.net.http.URIBuilder
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
    private String pitApiPath = "/pit/api"
    private Closure pingPath = {-> "/ping"}

    private Closure productPath = {catalogId, productId -> "/catalog/$catalogId/product/$productId"}
    private Closure productAttributeValuesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/attributeValue"}
    private Closure productClassificationGroupsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/classificationGroup"}
    private Closure productAssortmentPath = {catalogId, productId -> "${productPath(catalogId, productId)}/assortment"}
    private Closure productPricesPath = {catalogId, productId -> "${productPath(catalogId, productId)}/price"}
    private Closure productRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/relation"}
    private Closure productReverseRelationsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/reverseRelation"}
    private Closure productDocumentsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/document"}
    private Closure productVariantsPath = {catalogId, productId -> "${productPath(catalogId, productId)}/variant"}

    private Closure generalClassificationPath = { -> "/classification"}
    private Closure classificationPath = {classificationId -> "${generalClassificationPath()}/$classificationId"}
    private Closure classificationGroupPath = {classificationId, classificationGroupId -> "/classification/$classificationId/classificationGroup/$classificationGroupId"}
    private Closure classificationGroupSubgroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/classificationGroup"}
    private Closure classificationGroupAttributeValuesPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attributeValue"}
    private Closure productsByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/product"}
    private Closure classificationGroupsPath = {classificationId -> "${classificationPath(classificationId)}/classificationGroup"}

    private Closure generalAttributePath = { -> "/attribute"}
    private Closure attributePath = {attributeId -> "${generalAttributePath()}/$attributeId"}
    private Closure attributesByClassificationPath = {classificationId -> "${classificationPath(classificationId)}/attribute"}
    private Closure attributesByClassificationGroupPath = {classificationId, classificationGroupId -> "${classificationGroupPath(classificationId, classificationGroupId)}/attribute"}

    private Closure generalCatalogsPath = { -> "/catalog"}
    private Closure catalogPath = {catalogId -> "${generalCatalogsPath()}/$catalogId"}
    private Closure allProductsByCatalogPath = {catalogId -> "${generalCatalogsPath()}/$catalogId/product"}

    private Closure allProductsByClassificationPath = {classificationId -> "${generalClassificationPath()}/$classificationId/product"}
    private Closure allProductsByContractPath = {contractId -> "${generalContractsPath()}/$contractId/product"}
    private Closure allProductsBySupplierPath = {supplierId -> "${generalSuppliersPath()}/$supplierId/product"}

    private Closure generalContractsPath = { -> "/contract"}
    private Closure contractPath = {contractId -> "${generalContractsPath()}/$contractId"}
    
    private Closure generalSuppliersPath = { -> "/supplier"}
    private Closure supplierPath = {supplierId -> "${generalSuppliersPath()}/$supplierId"}

    private Closure generalManufacturersPath = { -> "/manufacturer"}
    private Closure manufacturerPath = {manufacturerId -> "${generalManufacturersPath()}/$manufacturerId"}
    
    private Closure generalPriceTypePath = { -> "/priceType"}
    private Closure priceTypePath = {priceTypeId -> "${generalPriceTypePath()}/$priceTypeId"}
        
    private Closure generalAttributeSectionPath = { -> "/attributeSection"}
    private Closure attributeSectionPath = {attributeSectionId -> "${generalAttributeSectionPath()}/$attributeSectionId"}

    private Closure generalUnitOfMeasurePath = { -> "/unitOfMeasure"}
    private Closure unitOfMeasurePath = {unitOfMeasureId -> "${generalUnitOfMeasurePath()}/$unitOfMeasureId"}

    private Closure generalDocumentPath =  { -> "/document"}
    private Closure documentPath = {documentId -> "${generalDocumentPath()}/$documentId"}

    private Closure generalBoilerplatePath = { -> "/boilerplate"}
    private Closure boilerplatePath = {boilerplateId -> "${generalBoilerplatePath()}/$boilerplateId"}
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
     * @param  options Optional
     * <p>
     * Key: languageIds - All language-specific fields will be filtered to only include languages with the matching languageIds. If not provided, all language-specific fields are returned in all languages.
     * <p>
     * Key: exclude - A list of field ids. If defined, the result will include the defined fields.
     * <p>
     * Available values - attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId, prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     * <p>
     * Key: include - A list of field ids. If defined, the result will include the defined fields.
     * <p>
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * Available values - attributeValues, classificationGroupAssociations, contracts, docAssociations, extProductId, keywords, master , manufacturerId, manufacturerName, mfgProductId, prices, productIdExtension, relations, reverseRelations, salesUnitOfMeasureId, statusId, supplierId, unitOfMeasureId, validFrom , validTo, variants
     * @return A Response that contains the product
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getProduct(String catalogId, String productId, HashMap options = [:]) {
        def languageIds = options.languageIds
        def exclude = options.exclude
        def include = options.include
        def variantId = options.variantId

        def query = [:]
        if(languageIds){
            query.put('languageIds', languageIds.join(','))    
        }
        if(exclude){
            query.put('exclude', exclude.join(','))
        }
        if(include){
            query.put('include', include.join(','))
        }
        if(variantId){
            query.put('variantId', variantId)
        }
        String path = productPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve a classification
     * @param classificationId ClassificationId
     * @param options Optional
     * <p>
     * key: include - Fields which should be included
     * <p>
     * @return classification
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassification(String classificationId, options = [:])  {
        def query = [:]
        def include = options.include
        if(include){
            query.put('include', include.join(','))
        }
        String path = classificationPath(classificationId)
        
        restGet(path, query)
    }

    /**
     * Retrieve a classificationGroup
     * @param  classificationId ClassificationId
     * @param  classificationGroupId ClassificationGroupId
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
     * @param classificationId ClassificationId
     * @param classificationGroupId ClassificationGroupId
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
     * Retrieve a list of Subgroups from a ClassificationGroup
     * @param classificationId ClassificationId
     * @param classificationGroupId ClassificationGroupId
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * @return List of classificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupSubgroups(String classificationId, String classificationGroupId, options = [:]) {
        def limit = options.limit
        def offset = options.offset
        def query = [:]
        if(limit){
            query.put('limit', limit)
        }
        if(offset){
            query.put('offset', offset)
        }
        String path = classificationGroupSubgroupPath(classificationId, classificationGroupId)
        restGet(path, query)
    }

    /**
     * Retrieve all classificationGroup from a specific classification
     * @param  classificationId ClassificationId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are classificationGroupId, statusId and orderNo.
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of classificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getClassificationGroupsByClassification(String classificationId, options=[:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = classificationGroupsPath(classificationId)
        restGet(path, query)
    }

    /**
     * Retrieve all Attributes assigned to a Classification
     * @param  classificationId ClassificationId
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
     * @param  classificationId ClassificationId
     * @param  classificationGroupId ClassificationGroupId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are productId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: catalogId - If set, products will be filtered by this catalogId
     * <p>
     * <p>
     * Key: supplierId - If set, products will be filtered by this supplierId
     * <p>
     * <p>
     * Key: contractId - If set, products will be filtered by this contractId
     * <p>
     * <p>
     * Key: statusIdFrom - If set, only products with at least this statusId will be returned
     * <p>
     * <p>
     * Key: statusIdTo - If set, only products with at most this statusId will be returned
     * <p>
     * @return List of Products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllProductsByClassificationGroup(String classificationId, String classificationGroupId, options=[:]) {
        def limit = options.limit
        def offset = options.offset
        def sort = options.sort
        def order = options.order
        def catalogId = options.catalogId
        def supplierId = options.supplierId
        def contractId = options.contractId
        def statusIdFrom = options.statusIdFrom
        def statusIdTo = options.statusIdTo
        def query = [:]
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(order){
            query.put("order", order)
        }
        if(catalogId){
            query.put("catalogId", catalogId)
        }
        if(supplierId){
            query.put("supplierId", supplierId)
        }
        if(contractId){
            query.put("contractId", contractId)
        }
        if(statusIdFrom){
            query.put("statusIdFrom", statusIdFrom)
        }
        if(statusIdTo){
            query.put("statusIdTo", statusIdTo)
        }
        String path = productsByClassificationGroupPath(classificationId, classificationGroupId)
        restGet(path, query)
    }

    /**
     * Retrieve all Classifications
     * @param  options Optional
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are classificationId and orderNo
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all Classifications
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllClassifications(options = [:]) {
        def query = [:]
        def order = options.order
        def sort = options.sort
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        String path = generalClassificationPath()
        restGet(path, query)
    }

    /**
     * Retrieve all Attributes
     * @param options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are attributeId and orderNo
     * <p>
     * @return List of all Attributes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllAttributes(options = [:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalAttributePath()
        restGet(path, query)
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
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * <p>
     * Key: languageIds - All language-specific fields will be filtered to only include languages with the matching languageIds. If not provided, all language-specific fields are returned in all languages.
     * <p>
     * @return List of ProductAttributeValues
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAttributeValues(String catalogId, String productId, def options=[:]) {
        def query = [:]
        def variantId = options.variantId
        def languageIds = options.languageIds
        if(variantId){
            query.put("variantId", variantId)
        }
        if(languageIds){
            query.put("languageIds", languageIds.join(","))
        }
        String path = productAttributeValuesPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all ClassificationGroups of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * @return List of ClassificationGroups
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductClassificationGroups(String catalogId, String productId, options = [:]) {
        def variantId = options.variantId
        def query = [:]
        if(variantId){
            query.put("variantId", variantId)
        }
        String path = productClassificationGroupsPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all Assortments of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * @return List of Assortments
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductAssortments(String catalogId, String productId, options = [:]) {
          def variantId = options.variantId
        def query = [:]
        if(variantId){
            query.put("variantId", variantId)
        }
        String path = productAssortmentPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all Prices of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * @return List of Prices
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductPrices(String catalogId, String productId, options = [:]) {
        def variantId = options.variantId
        def query = [:]
        if(variantId){
            query.put("variantId", variantId)
        }
        String path = productPricesPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all relations of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * @return List of relations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductRelations(String catalogId, String productId, options = [:]) {
        def variantId = options.variantId
        def query = [:]
        if(variantId){
            query.put("variantId", variantId)
        }
        String path = productRelationsPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all reverse-relations of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * @return List of ReverseRelations
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductReverseRelations(String catalogId, String productId, options = [:]) {
        def variantId = options.variantId
        def query = [:]
        if(variantId){
            query.put("variantId", variantId)
        }
        String path = productReverseRelationsPath(catalogId, productId)
        restGet(path, query)
    }

    /**
     * Retrieve all documents of a Product
     * @param  catalogId CatalogId
     * @param  productId ProductId
     * @param  options Optional
     * <p>
     * Key: variantId - If set, the values are returned for the variant instead of the product.
     * <p>
     * <p>
     * Key: languageIds - All language-specific fields will be filtered to only include languages with the matching languageIds. If not provided, all language-specific fields are returned in all languages.
     * <p>
     * @return List of documents
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getProductDocuments(String catalogId, String productId, options = [:]) {
        def languageIds = options.languageIds
        def variantId = options.variantId
        def query = [:]
        if(languageIds){
            query.put('languageIds', languageIds.join(','))
        }
        if(variantId){
            query.put('variantId', variantId)
        }
        String path = productDocumentsPath(catalogId, productId)
        restGet(path, query)
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
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are catalogId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all Catalogs
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllCatalogs(options = [:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(limit){
            query.put("limit", limit)
        }
        String path = generalCatalogsPath()
        restGet(path, query)
    }

    /**
     * Retrieve an Catalog
     * @param  catalogId catalogId
     * @return List of Catalogs
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
     * Retrieve all Products from a Catalog
     * @param  catalogId catalogId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are productId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: supplierId - If set, products will be filtered by this supplierId
     * <p>
     * <p>
     * Key: contractId - If set, products will be filtered by this contractId
     * <p>
     * <p>
     * Key: classificationId - If set, products will be filtered by this classificationId
     * <p>
     * <p>
     * Key: classificationGroupId - If set, products will be filtered by this classificationGroupId
     * <p>
     * <p>
     * Key: statusIdFrom - If set, only products with at least this statusId will be returned
     * <p>
     * <p>
     * Key: statusIdTo - If set, only products with at most this statusId will be returned
     * <p>
     * @return List of products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllProductsByCatalogId(String catalogId, options = [:]) {
        def limit = options.limit
        def offset = options.offset
        def sort = options.sort
        def order = options.order
        def supplierId = options.supplierId
        def contractId = options.contractId
        def classificationId = options.classificationId
        def classificationGroupdId = options.classificationGroupId
        def statusIdFrom = options.statusIdFrom
        def statusIdTo = options.statusIdTo
        def query = [:]
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(order){
            query.put("order", order)
        }
        if(supplierId){
            query.put("supplierId", supplierId)
        }
        if(contractId){
            query.put("contractId", contractId)
        }
        if(classificationId){
            query.put("classificationId", classificationId)
        }
        if(classificationGroupId){
            query.put("classificationGroupId", classificationGroupId)
        }
        if(statusIdFrom){
            query.put("statusIdFrom", statusIdFrom)
        }
        if(statusIdTo){
            query.put("statusIdTo", statusIdTo)
        }
        String path = allProductsByCatalogPath(catalogId)
        restGet(path, query)
    }

    /**
     * Retrieve all Products from a Supplier
     * @param  supplierId supplierId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are productId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: catalogId - If set, products will be filtered by this catalogId
     * <p>
     * <p>
     * Key: contractId - If set, products will be filtered by this contractId
     * <p>
     * <p>
     * Key: classificationId - If set, products will be filtered by this classificationId
     * <p>
     * <p>
     * Key: classificationGroupId - If set, products will be filtered by this classificationGroupId
     * <p>
     * <p>
     * Key: statusIdFrom - If set, only products with at least this statusId will be returned
     * <p>
     * <p>
     * Key: statusIdTo - If set, only products with at most this statusId will be returned
     * <p>
     * @return List of products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllProductsBySupplierId(String supplierId, options=[:]) {
        def limit = options.limit
        def offset = options.offset
        def sort = options.sort
        def order = options.order
        def catalogId = options.catalogId
        def contractId = options.contractId
        def classificationId = options.classificationId
        def classificationGroupId = options.classificationGroupId
        def statusIdFrom = options.statusIdFrom
        def statusIdTo = options.statusIdTo
        def query = [:]
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(order){
            query.put("order", order)
        }
        if(catalogId){
            query.put("catalogId", catalogId)
        }
        if(contractId){
            query.put("contractId", contractId)
        }
        if(classificationId){
            query.put("classificationId", classificationId)
        }
        if(classificationGroupId){
            query.put("classificationGroupId", classificationGroupId)
        }
        if(statusIdFrom){
            query.put("statusIdFrom", statusIdFrom)
        }
        if(statusIdTo){
            query.put("statusIdTo", statusIdTo)
        }
        String path = allProductsBySupplierPath(supplierId)
        restGet(path, query)
    }
     /**
     * Retrieve all Products from a Classification
     * @param  classificationId classificationId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are productId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: catalogId - If set, products will be filtered by this catalogId
     * <p>
     * <p>
     * Key: supplierId - If set, products will be filtered by this supplierId
     * <p>
     * <p>
     * Key: contractId - If set, products will be filtered by this contractId
     * <p>
     * <p>
     * Key: classificationGroupId - If set, products will be filtered by this classificationGroupId
     * <p>
     * <p>
     * Key: statusIdFrom - If set, only products with at least this statusId will be returned
     * <p>
     * <p>
     * Key: statusIdTo - If set, only products with at most this statusId will be returned
     * <p>
     * @return List of products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllProductsByClassificationId(String classificationId, options=[:]) {
        def limit = options.limit
        def offset = options.offset
        def sort = options.sort
        def order = options.order
        def catalogId = options.catalogId
        def supplierId = options.supplierId
        def contractId = options.contractId
        def classificationGroupId = options.classificationGroupId
        def statusIdFrom = options.statusIdFrom
        def statusIdTo = options.statusIdTo
        def query = [:]
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(order){
            query.put("order", order)
        }
        if(catalogId){
            query.put("catalogId", catalogId)
        }
        if(supplierId){
            query.put("supplierId", supplierId)
        }
        if(contractId){
            query.put("contractId", contractId)
        }
        if(classificationGroupId){
            query.put("classificationGroupId", classificationGroupId)
        }
        if(statusIdFrom){
            query.put("statusIdFrom", statusIdFrom)
        }
        if(statusIdTo){
            query.put("statusIdTo", statusIdTo)
        }
        String path = allProductsByClassificationPath(classificationId)
        restGet(path, query)
    }
     /**
     * Retrieve all Products from a Contract
     * @param  contractId contractId
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are productId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: catalogId - If set, products will be filtered by this catalogId
     * <p>
     * <p>
     * Key: supplierId - If set, products will be filtered by this supplierId
     * <p>
     * <p>
     * Key: classificationId - If set, products will be filtered by this classificationId
     * <p>
     * <p>
     * Key: classificationGroupId - If set, products will be filtered by this classificationGroupId
     * <p>
     * <p>
     * Key: statusIdFrom - If set, only products with at least this statusId will be returned
     * <p>
     * <p>
     * Key: statusIdTo - If set, only products with at most this statusId will be returned
     * <p>
     * @return List of products
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllProductsByContractId(String contractId, options=[:]) {
        def limit = options.limit
        def offset = options.offset
        def sort = options.sort
        def order = options.order
        def catalogId = options.catalogId
        def supplierId = options.supplierId
        def classificationId = options.classificationId
        def classificationGroupId = options.classificationGroupId
        def statusIdFrom = options.statusIdFrom
        def statusIdTo = options.statusIdTo
        def query = [:]
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(order){
            query.put("order", order)
        }
        if(catalogId){
            query.put("catalogId", catalogId)
        }
        if(supplierId){
            query.put("supplierId", supplierId)
        }
        if(classificationId){
            query.put("classificationId", classificationId)
        }
        if(classificationGroupId){
            query.put("classificationGroupId", classificationGroupId)
        }
        if(statusIdFrom){
            query.put("statusIdFrom", statusIdFrom)
        }
        if(statusIdTo){
            query.put("statusIdTo", statusIdTo)
        }
        String path = allProductsByContractPath(contractId)
        restGet(path, query)
    }

    /**
     * Retrieve all Contracts
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are contractId and statusId
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all Contracts
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllContracts(options=[:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalContractsPath()
        restGet(path, query)
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
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all Suppliers
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllSuppliers(options=[:]) {
        def query = [:]
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalSuppliersPath()
        restGet(path, query)
    }


    /**
     * Retrieve all Manufacturers
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all Manufacturers
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllManufacturers(options=[:]) {
        def query = [:]
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalManufacturersPath()
        restGet(path, query)
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
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are attributeSectionId and orderNo
     * <p>
     * @return List of all AttributeSections
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllAttributeSections(options=[:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalAttributeSectionPath()
        restGet(path, query)
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

    /**
     * Retrieve all PriceTypes
     * @param  options Optional
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all PriceTypes
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllPriceTypes(options=[:]) {
        def query = [:]
        def order = options.order
        if(order){
            query.put("order", order)
        }
        String path = generalPriceTypePath()
        restGet(path, query)
    }

    /**
     * Retrieve an PriceType
     * @param  priceTypeId priceTypeId
     * @return priceType
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getPriceType(String priceTypeId) {
        String path = priceTypePath(priceTypeId)
        restGet(path)
    }

    /**
     * Retrieve all UnitOfMeasures
     * @param  options Optional
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * @return List of all UnitOfMeasures
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllUnitOfMeasures(options=[:]) {
        def query = [:]
        def order = options.order
        if(order){
            query.put("order", order)
        }
        String path = generalUnitOfMeasurePath()
        restGet(path, query)
    }

    /**
     * Retrieve an UnitOfMeasure
     * @param  unitOfMeasureId unitOfMeasureId
     * @return unitOfMeasure
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getUnitOfMeasure(String unitOfMeasureId) {
        String path = unitOfMeasurePath(unitOfMeasureId)
        restGet(path)
    }

        /**
     * Retrieve a Document
     * @param  documentId documentId
     * @return document
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getDocument(String documentId) {
        def encodedDocumentId = URLEncoder.encode(documentId,'UTF-8')
        String path = documentPath(encodedDocumentId)
        restGet(path,[meta:true])
    }

      /**
     * Retrieve all boilerplates
     * @return List of all boilerplates
     * @param  options Optional
     * <p>
     * Key: limit - The field which should be used for a limit of how much items should returned. By default the value is 100.
     * <p>
     * <p>
     * Key: offset - The field which should be used to determine how many items to skip at the beginning.
     * <p>
     * <p>
     * Key: order - The order in which the result should be sorted. Available values are asc for ascending and desc for descending
     * <p>
     * <p>
     * Key: sort - The field which should be used for sorting. Available values are boilerplateId and statusId
     * <p>
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */
    public Response getAllBoilerplates(options=[:]) {
        def query = [:]
        def sort = options.sort
        def order = options.order
        def limit = options.limit
        def offset = options.offset
        if(order){
            query.put("order", order)
        }
        if(sort){
            query.put("sort", sort)
        }
        if(limit){
            query.put("limit", limit)
        }
        if(offset){
            query.put("offset", offset)
        }
        String path = generalBoilerplatePath()
        restGet(path,query)
    }

    /**
     * Retrieve a boilerplate
     * @param  boilerplateId boilerplateId
     * @return boilerplates
     * @throws NotAuthorizedException
     * @throws UnknownHostException
     * @throws GroovyAPIInternalErrorException
     * @throws PITInternalErrorException
     * @throws PIMAccessDeniedException
     * @throws PIMUnreachableException
     * @throws PIMInternalErrorException
     */

    public Response getBoilerplate(String boilerplateId) {
        String path = boilerplatePath(boilerplateId)
        restGet(path)
    }
    
    private Response restGet(String path, LinkedHashMap query = [:]) {
        def response
        query.put('token', accessToken)
        def uri = new URIBuilder(new URI(restClient.uri.toString() + pitApiPath + path))
        try {
            response = restClient.get([uri: uri,
                contentType: ContentType.APPLICATION_JSON,
                query: query
            ])

        } catch (Exception e) {
            if(e instanceof HttpResponseException && e.response.data.status == 404){
                return new Response([])
            }
            handleException(e)
        }
        return new Response(response.data)
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
    private def meta
    private def status
    private boolean isEmpty = false

    public Response(def receivedData) {
        this.value = receivedData.result
        this.meta = receivedData.meta
        this.status = receivedData.status
        isEmpty = value ? false : true
    }

    public def getValue(){
        value
    }

    public def getMeta(){
        meta
    }

    public def getStatus(){
        status
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
