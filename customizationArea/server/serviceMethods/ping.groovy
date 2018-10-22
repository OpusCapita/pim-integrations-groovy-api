import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
* Initializes an Instance of the Customizationservice
* @author  Dennis Brueseke
* @version 1.0
*/
class getInstance{

    /**
     * [execute description]
     * @return [description]
     */
    Customizationservice execute(){
        new CustomizationService()
    }

}

/**
 * Provides methods for customization tasks
 */
class CustomizationService{
    Log log = LogFactory.getLog(this.class)

    /**
     * Ping the PIT
     */
    public static String ping(){
        "Some msg"
    }
}
