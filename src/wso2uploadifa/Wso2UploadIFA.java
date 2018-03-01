/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wso2uploadifa;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

import javax.xml.namespace.QName;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.wso2.carbon.governance.api.exception.GovernanceException;

/**
 *
 * @author attila.rezner
 */
public class Wso2UploadIFA {

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
    // sheet name 
    public static final String SHEET_NAME = "Application connections";
    // Location of the properties file. The files contains the mapping between the Excel data cells and Governance
    // artifact fields
    public static final String PROPERTIES_BASE_PATH = "src/properties";
    // Property file for ifa
    public static final String IFA_MAPPING_PROPERTIES = "ifa.mapping.applicationconn.properties";
    // IFA Governance artifact key
    public static final String IFAOPERATION = "ifaoperation";

    public static void main(String[] args) {
        //
        String trustStore = CARBON_HOME + File.separator + "repository" + File.separator
                + "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("carbon.repo.write.mode", "true");        
        
        try {
            // Initialize the registry
            ConfigurationContext configContext
                    = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS_2_CONFIGURATION);

            Registry registry
                    = new WSRegistryServiceClient(SERVICE_URL, USERNAME, PASSWORD, configContext);

            Properties ifaProperties
                    = getProperties(PROPERTIES_BASE_PATH + File.separator + IFA_MAPPING_PROPERTIES);
            
            // Get the work books
            Workbook[] workbooks = getWorkbooks(new File(DATA_DIR));
                        
            // roll over all workbooks
            for (Workbook workbook : workbooks) {
                // get Application Connection sheet
                Sheet sheet = workbook.getSheet(SHEET_NAME);

                // if Application Connection sheet not found
                if (sheet == null || sheet.getLastRowNum() == -1) {
                    throw new RuntimeException("The " + SHEET_NAME +" sheet is empty or not exist");
                } else {
                    // Application Connection sheet found
                    System.out.println("Adding data in " + SHEET_NAME);
                }
                // get row num on Application Connection sheet
                int limit = sheet.getLastRowNum();
                // if no rows on Application Connection sheet
                if (limit < 1) {
                    throw new RuntimeException("Column headers were not specified in Asset Data Spreadsheet");
                } else {
                    // if found rows on Application Connection sheet
                    System.out.println("Total number of rows in the sheet Application Connection : " + limit);
                }

                // This line contains column names on Application Connection sheet 
                // to be mapped in properties file.
                Row row = sheet.getRow(1);
                // We use a linked list to keep the order for attributes exist in registry
                List<String> headersAttributeNames = new LinkedList<>();
                String value;
                int count = 0;
                Set artifactAttributes = ifaProperties.keySet();
                // roll over all cells in the row 1 to het attribue names
                while ((value = getCellValue(row.getCell(count++), null)) != null) {
                    headersAttributeNames.add(getMappingName(ifaProperties, value));
                }
                
                Registry governanceRegistry = 
                    GovernanceUtils.getGovernanceUserRegistry(registry, USERNAME);

                GovernanceArtifactConfiguration artifactConfiguration = 
                    GovernanceUtils.findGovernanceArtifactConfiguration(
                        IFAOPERATION, governanceRegistry);

                String nameAttribute = artifactConfiguration.getArtifactNameAttribute();

                String namespaceAttribute = 
                    artifactConfiguration.getArtifactNamespaceAttribute();

                GenericArtifactManager manager = new GenericArtifactManager(
                    governanceRegistry, IFAOPERATION);

                List<QName> addedQNames = new ArrayList<>();

                Map<String, String> attributeMap = new HashMap<>();

                // roll over data on Application Connection sheet
                for (int i = 3; i <= limit; i++) {
                    attributeMap.clear();
                    row = sheet.getRow(i);

                    if (row == null || 
                        row.getCell(1) == null || 
                        row.getCell(1).toString().trim().isEmpty()) {

                        break;
                    } 
                    else {
                        System.out.println("Adding data in row : " + i);
                    }

                    // We use this code to get the cell values of the given attribute. We use a linked list to find the
                    // column index of that attribute and add it to a map with the governance artifact field as the key
                    for (Object attributeObject : artifactAttributes) {
                        String attribute = attributeObject.toString();

                        if (headersAttributeNames.contains(attribute)) {
                            attributeMap.put(attribute, row.getCell(
                                headersAttributeNames.indexOf(attribute)).toString().trim());
                        }
                    }

                    // Creating the artifact QName
                    String namespaceURI = attributeMap.containsKey(namespaceAttribute) 
                        ? attributeMap.get(namespaceAttribute)
                        : null;

                    String localPart = attributeMap.containsKey(nameAttribute) 
                        ? attributeMap.get(nameAttribute)
                        : UUIDGenerator.generateUUID();

                    localPart = removeInvalidCharacters(localPart);
                    QName qName = new QName(namespaceURI, localPart);

                    // Creating the governance artifact with the given fields.
                    GenericArtifact artifact = manager.newGovernanceArtifact(qName);

                    for (Map.Entry<String, String> e : attributeMap.entrySet()) {
                        artifact.setAttribute(e.getKey(), e.getValue());
                    }

                    //  if interfaceId is empty then try to create from service and operation name
                    if (attributeMap.get("overview_interfaceId").isEmpty()) {
                        artifact.setAttribute("overview_interfaceId", 
                            attributeMap.get("overview_serviceName") +"." +
                            attributeMap.get("overview_operationName"));
                    }
                    
                    // Checking for duplicates
                    if (!addedQNames.contains(qName)) { 
                        try {
                            manager.addGenericArtifact(artifact);
                            addedQNames.add(qName);
                        } catch (GovernanceException governanceException) {
                            if (governanceException.getMessage().contains("Failed to add artifact")) {
                                // if exception is : item already exist, then it's ok, else re-throw.
                            }
                            else {
                                throw governanceException;
                            }                    
                        }
                    }
                }            
            }            
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static String removeInvalidCharacters(String localPart) {
        return localPart.replace("\'", "`")
                        .replace("\"", "`")
                        .replace("&", "and")
                        .replace(",", " ")
                        .replace("!", "");
    }

    private static Workbook[] getWorkbooks(File usersDir) throws Exception {
        
        List<Workbook> workbooks = new LinkedList<>();
        File[] files = usersDir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                InputStream ins = null;
                try {
                    ins = new BufferedInputStream(new FileInputStream(file));
                    
                    String extension = FilenameUtils.getExtension(file.getName());                    
                    
                    if ("xlsx".equals(extension.toLowerCase())) {
                        workbooks.add(new XSSFWorkbook(ins));
                    } 
                    else {
                        POIFSFileSystem fs = new POIFSFileSystem(ins);
                        workbooks.add(new HSSFWorkbook(fs));
                    }
                } 
                finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } 
                        catch (IOException e) {
                            // We ignore exceptions here.
                        }
                    }
                }
            }
        }
        return workbooks.toArray(new Workbook[workbooks.size()]);
    }

    private static Properties getProperties(String fileName) throws Exception {
        
        Properties properties = new Properties();
        FileInputStream inputStream = null;
        
        try {
            inputStream = new FileInputStream(fileName);
            properties.load(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // We ignore any exceptions here.
                }
            }
        }
        return properties;
    }

    private static String getMappingName(Properties properties, String value) {
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            if (property.getValue().toString().equals(value)) {
                return property.getKey().toString();
            }
        }
        return null;
    }

    private static String getCellValue(Cell cell, String def) {
        return cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK 
               ? cell.getStringCellValue() 
               : def;
    }
        
}
