/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wso2uploadifa;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

import java.io.File;
import org.apache.axis2.AxisFault;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

/**
 *
 * @author attila.rezner
 */
public class Wso2UploadIFA21 {

    // Carbon home - this should point to the folder where your WSO2 Governance Registry is unzipped
    public static final String CARBON_HOME = "C:/java/wso2greg-5.4.0";
    
    public static final String AXIS_2_CONFIGURATION = CARBON_HOME + File.separator
            + "repository" + File.separator + "conf" + File.separator
            + "axis2" + File.separator + "axis2_client.xml";
    // Server access URL
    public static final String SERVICE_URL = "https://localhost:9443/services/";
    // Default admin user name of the server
    public static final String USERNAME = "admin";
    // Default admin password
    public static final String PASSWORD = "admin";
    // Location of the Excel data
    public static final String DATA_DIR = "c:/TEMP/wso2upload/";
    // Location of the properties file. The files contains the mapping between the Excel data cells and Governance

    public static void main(String[] args) throws AxisFault, RegistryException {
        //
        String trustStore = CARBON_HOME + File.separator + "repository" + File.separator
                + "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("carbon.repo.write.mode", "true");        
        
        ConfigurationContext configContext = 
            ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                AXIS_2_CONFIGURATION);

        Registry registry = new WSRegistryServiceClient(
            SERVICE_URL, USERNAME, PASSWORD, configContext);            
                    
        Registry governanceRegistry = 
            GovernanceUtils.getGovernanceUserRegistry(registry, USERNAME);

        GenericArtifactManager manager = new GenericArtifactManager(
            governanceRegistry, "endpoint");
        
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();
        for (GenericArtifact artifact : artifacts) {            
                manager.removeGenericArtifact(artifact.getId());
                System.out.println(artifact);            
        }
    }
               
}
