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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.wso2.carbon.governance.api.exception.GovernanceException;

/**
 *
 * @author attila.rezner
 */
public class Wso2UploadIFA2 {

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
            
            ConfigurationContext configContext = 
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    AXIS_2_CONFIGURATION);

            Registry registry = new WSRegistryServiceClient(
                SERVICE_URL, USERNAME, PASSWORD, configContext);            
            
//        WSRegistryServiceClient wsreg = (WSRegistryServiceClient)registry;
//        Resource res = wsreg.get("/_system/governance/trunk/ifaoperations/CampaignServiceV1.cacheStatusOutboundCampaignV3");
//        System.out.println(new String((byte[])res.getContent()));
        
            Registry governanceRegistry = 
                GovernanceUtils.getGovernanceUserRegistry(registry, USERNAME);

            GovernanceArtifactConfiguration artifactConfiguration = 
                GovernanceUtils.findGovernanceArtifactConfiguration(
                    IFAOPERATION, governanceRegistry);

            String nameAttribute = 
                artifactConfiguration.getArtifactNameAttribute();

            String namespaceAttribute = 
                artifactConfiguration.getArtifactNamespaceAttribute();

            GenericArtifactManager manager = new GenericArtifactManager(
                governanceRegistry, IFAOPERATION);
                        
            // Get the work books
            Workbook[] workbooks = getWorkbooks(new File(DATA_DIR));
                        
            // roll over all workbooks
            for (Workbook workbook : workbooks) {
                
                Vector operationNames = getOperationNames(workbook);
                
                Iterator iter = operationNames.iterator();
                while (iter.hasNext()) {
                    String operationName = (String)iter.next();
                    
                    Map<String, String> attributeMap = getOperationAttributeMap(
                        workbook, operationName);

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
//                        artifact.setAttribute(removeNumbering(e.getKey()), e.getValue());
                        artifact.addAttribute(removeNumbering(e.getKey()), e.getValue());
                    }
                    System.out.println(artifact.toString());
                    try {
                        manager.addGenericArtifact(artifact);
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

    private static String getCellValue(Cell cell) {
        if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue();
        } else if (cell != null && cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else {
            return "";
        }
    }
            
    private static Vector getOperationNames(Workbook workbook) {
        
        // list operation names on ApplicationConnection sheet column 6
        Sheet applicationConnectionsSheet = 
            workbook.getSheet("Application Connections");
        
        Vector operationNames = new Vector();
                
        // operations find on 6th column from 3rd row.
        for (int i = 3; i < applicationConnectionsSheet.getLastRowNum(); i++) {
            Row row = applicationConnectionsSheet.getRow(i);            
            // operationName is in 6th column, so 6th cell in every row
            String operationNameString = getCellValue(row.getCell(6));
            if (!operationNameString.equals("")) {
                operationNames.add(operationNameString);
            }
        }
        
        return operationNames;
    }
    
    private static Map<String, String> getOperationAttributeMap(
        Workbook workbook, String operationName) 
    throws Exception {

        // get row of operation on ApplicationConnection sheet, fill attributes
        Sheet applicationConnectionsSheet = workbook.getSheet("Application Connections");
        
        Map<String, String> attributeMap = new HashMap<>();
        // roll over on operations in Appl Conn sheet
        for (int i = 3; i < applicationConnectionsSheet.getLastRowNum(); i++) {
            Row row = applicationConnectionsSheet.getRow(i);            
            
            if (operationName.equalsIgnoreCase(getCellValue(row.getCell(6)))) {            
                attributeMap.put("application_caller", getCellValue(row.getCell(0)));
                attributeMap.put("application_destination", getCellValue(row.getCell(1)));
                attributeMap.put("application_direction", getCellValue(row.getCell(2)));            
                attributeMap.put("overview_description", getCellValue(row.getCell(3)));
                attributeMap.put("overview_interfaceId", getCellValue(row.getCell(4)));
                attributeMap.put("overview_serviceName", getCellValue(row.getCell(5)));
                attributeMap.put("overview_operationName", getCellValue(row.getCell(6)));
                attributeMap.put("business_businessObjects", getCellValue(row.getCell(7)));
                attributeMap.put("business_businessObjectsDetals", getCellValue(row.getCell(8)));
                attributeMap.put("technical_isPersisted", getCellValue(row.getCell(9)));
                attributeMap.put("technical_dependencyType", getCellValue(row.getCell(10)));
                attributeMap.put("technical_frequency", getCellValue(row.getCell(11)));
                attributeMap.put("technical_initiator", getCellValue(row.getCell(12)));
                attributeMap.put("technical_operation", getCellValue(row.getCell(13)));
                attributeMap.put("technical_interfaceTechnology", getCellValue(row.getCell(14)));
                attributeMap.put("technical_interfaceTechnologyComment", getCellValue(row.getCell(15)));
                attributeMap.put("design_designComment", getCellValue(row.getCell(16)));
                attributeMap.put("design_newExistingFlag", getCellValue(row.getCell(17)));

                break;
            }
        }
        
        // goto sheet of operation fetch input/output data from all rows
        Sheet operationSheet = workbook.getSheet(operationName);        
            
        boolean inputLine = false;
        boolean outputLine = false;
        // 9th row is the first real data row...
        for (int i = 9; i < operationSheet.getLastRowNum(); i++) {
            Row row = operationSheet.getRow(i);        
            
            if (getCellValue(row.getCell(1)).contains("Input")) {
                inputLine = true;                
            }
            if (getCellValue(row.getCell(1)).contains("Output")) {
                outputLine = true;
                inputLine = false;
            }
            
            if (inputLine) {
                attributeMap.put("inputdata_name" +i, getCellValue(row.getCell(2)));
                attributeMap.put("inputdata_sourceSystem" +i, getCellValue(row.getCell(3)));
                attributeMap.put("inputdata_objectType" +i, getCellValue(row.getCell(4)));
                attributeMap.put("inputdata_length" +i, getCellValue(row.getCell(5)));
                attributeMap.put("inputdata_lov" +i, getCellValue(row.getCell(6)));
                attributeMap.put("inputdata_multiplicity" +i, getCellValue(row.getCell(7)));
                attributeMap.put("inputdata_description" +i, getCellValue(row.getCell(8)));
            } 
            if (outputLine) {
                attributeMap.put("outputdata_name" +i, getCellValue(row.getCell(2)));
                attributeMap.put("outputdata_sourceSystem" +i, getCellValue(row.getCell(3)));
                attributeMap.put("outputdata_objectType" +i, getCellValue(row.getCell(4)));
                attributeMap.put("outputdata_length" +i, getCellValue(row.getCell(5)));
                attributeMap.put("outputdata_lov" +i, getCellValue(row.getCell(6)));
                attributeMap.put("outputdata_multiplicity" +i, getCellValue(row.getCell(7)));
                attributeMap.put("outputdata_description" +i, getCellValue(row.getCell(8)));
            }       
        }
        
        return attributeMap;
    }
    
    private static String removeNumbering(String attributeName) {
        return attributeName.replaceAll("\\d", "");
    }
    
}
