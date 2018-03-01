/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wso2.ifaoperation.Handling;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.ws.axis2.DeleteIFAoperationServiceGovernanceException;
import org.apache.ws.axis2.AddIFAoperationServiceGovernanceException;
import org.apache.ws.axis2.GetIFAoperationArtifactIDsServiceGovernanceException;
import org.apache.ws.axis2.GetIFAoperationDependenciesServiceGovernanceException;
import org.apache.ws.axis2.UpdateIFAoperationServiceGovernanceException;

import org.apache.ws.axis2.IFAoperationStub;
import org.apache.ws.axis2.IFAoperationStub.AddIFAoperation;
import org.apache.ws.axis2.IFAoperationStub.AddIFAoperationResponse;
import org.apache.ws.axis2.IFAoperationStub.DeleteIFAoperation;
import org.apache.ws.axis2.IFAoperationStub.DeleteIFAoperationResponse;
import org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDs;
import org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDsResponse;
import org.apache.ws.axis2.IFAoperationStub.GetIFAoperationDependencies;
import org.apache.ws.axis2.IFAoperationStub.GetIFAoperationDependenciesResponse;
import org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperation;
import org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperationResponse;

import org.wso2.carbon.utils.CarbonUtils;

/**
 *
 * @author attila.rezner
 */
public class Wso2ManipulateIFA {
    
    
    // Location of the Excel data
    public static final String DATA_DIR = "c:/TEMP/wso2upload/";//"/home/attila.rezner/java/wso2greg/xls";//"c:/TEMP/wso2upload/";
    
    public static final String CARBON_HOME = "C:/java/wso2greg-5.4.0";//"/home/attila.rezner/wso2greg-5.4.0";//"C:/java/wso2greg-5.4.0";
    
    private final IFAoperationStub stub;
        
    private static final String APP_CONN_SHEET_NAME = "Application Connections";
    
    public static final String PROPS_BASE_PATH = "src/properties";
    private static final String APP_CONN_PROPS = "ifa.mapping.applicationconn.properties";    
    private static final String INPUT_PROPS = "ifa.mapping.input.data.properties";
    private static final String OUTPUT_PROPS = "ifa.mapping.output.data.properties";
    
    private static Properties appConnProps, inputProps, outputProps;
    
    private static final int APP_CONN_SHEET_FIELD_NAMES_ROW_NUM = 1;
    private static final int APP_CONN_SHEET_FIRST_DATA_ROW_NUM = 3;
    private static final int OPER_SHEET_FIELD_NAMES_ROW_NUM = 9;
    private static final int DATA_FIELD_NAMES_ROW_NUM = 7;
    
    public static void main(String[] args) 
    throws AddIFAoperationServiceGovernanceException, 
           DeleteIFAoperationServiceGovernanceException, 
           GetIFAoperationArtifactIDsServiceGovernanceException,            
           RemoteException,
           Exception {
              
//        String trustStore = CARBON_HOME + File.separator + "repository" + File.separator
//                + "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";
        String trustStore = CARBON_HOME + "/repository/resources/security/middlewareapibrowser.jks";

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("carbon.repo.write.mode", "true");        
                
        appConnProps = getProperties(
            PROPS_BASE_PATH + File.separator + APP_CONN_PROPS);
        
        inputProps = getProperties(
            PROPS_BASE_PATH + File.separator + INPUT_PROPS);

        outputProps = getProperties(
            PROPS_BASE_PATH + File.separator + OUTPUT_PROPS);        
        
        try {

            String axis2Repo = CARBON_HOME + "/repository/deployment/client";
            String axis2Conf = CARBON_HOME + "/repository/conf/axis2/axis2_client.xml";
            
            ConfigurationContext configContext = 
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo, axis2Conf);
            
            Wso2ManipulateIFA wso2ManipulateIFA = 
                new Wso2ManipulateIFA(configContext, "https://middlewareapibrowser:9443/services/IFAoperation", false);

            String[] inputFileNames = getFileNames(new File(DATA_DIR));
            
            for (String inputFileName : inputFileNames) {            
                Workbook workbook = getWorkbook(inputFileName);

                Vector operNames = getOperationNames(workbook);
                                
                Iterator iter = operNames.iterator();
                while (iter.hasNext()) {
                    String operationName = (String)iter.next();
                    
                    System.out.println(
                        "parsing operation " +operationName +" in " +inputFileName);
                    
                    Map<String, String> attributeMap = getOperationAttributeMap(
                        workbook, operationName);
                   
                    String addIFAoperationXmlString = 
                        buildXMLStringFromAttributeMap(attributeMap);
                    
                    try {
                        wso2ManipulateIFA.addIFAoperation(addIFAoperationXmlString);
                    } catch (AxisFault e) {
                        System.out.println(e.getMessage());                        
                    }
                    
                }           
                
            }
                     
        } catch (Exception e) {
            e.printStackTrace();            
        }
        System.exit(0);        
        
    }   
    
    public Wso2ManipulateIFA(ConfigurationContext configContext,String epr, boolean listener) 
    throws AxisFault {
        
        stub = new IFAoperationStub(configContext,epr,listener);
        CarbonUtils.setBasicAccessSecurityHeaders("admin", "admin", stub._getServiceClient());
    }

    public String[] getIFAoperationArtifactIDs() 
    throws GetIFAoperationArtifactIDsServiceGovernanceException, 
           RemoteException {
        
        GetIFAoperationArtifactIDs artifactIDs = new GetIFAoperationArtifactIDs();
        
        GetIFAoperationArtifactIDsResponse getIFAoperationArtifactIDsResponse = 
            stub.getIFAoperationArtifactIDs(artifactIDs);
        
        String[] iFAoperationArifactIDs = getIFAoperationArtifactIDsResponse.get_return();
        
        return iFAoperationArifactIDs;
    }    
    
    /**Method takes artifactID of the iFAoperation instance to be deleted and returns the boolean value
     * @param artifactID
     * @return 
     * @throws java.rmi.RemoteException
     * @throws org.apache.ws.axis2.DeleteIFAoperationServiceGovernanceException */
    public boolean deleteIFAoperation(String artifactID) 
    throws DeleteIFAoperationServiceGovernanceException,
           RemoteException {
        
        DeleteIFAoperation deleteIFAoperation = new DeleteIFAoperation();
        
        deleteIFAoperation.setArtifactId(artifactID);
        
        DeleteIFAoperationResponse deleteIFAoperationResponse = 
            stub.deleteIFAoperation(deleteIFAoperation);
        
        boolean deleted = deleteIFAoperationResponse.get_return();
        
        if(deleted){
            System.out.println(artifactID+ " : The specified artifact has been deleted");
        }
        return deleted;
    }    

    public String addIFAoperation(String addIFAoperationXmlString) 
    throws AddIFAoperationServiceGovernanceException,
           RemoteException {

        AddIFAoperation addIFAoperation = new AddIFAoperation();
        addIFAoperation.setInfo(addIFAoperationXmlString);
        
        AddIFAoperationResponse addIFAoperationResponse = 
            stub.addIFAoperation(addIFAoperation);

        //prints the artifact id of the added iFAoperation artifact
        System.out.println(addIFAoperationResponse.get_return());
        
        return addIFAoperationResponse.get_return();    
    }
    
    public String[] getIFAoperationDependencies(String artifactID) 
    throws GetIFAoperationDependenciesServiceGovernanceException,
           RemoteException {
        
        GetIFAoperationDependencies iFAoperationDependencies = new GetIFAoperationDependencies();
        
        iFAoperationDependencies.setArtifactId(artifactID);
        
        GetIFAoperationDependenciesResponse iFAoperationDependenciesResponse = 
            stub.getIFAoperationDependencies(iFAoperationDependencies);
        
        return iFAoperationDependenciesResponse.get_return();
    }
    
    public boolean updateIFAoperation(String updateIFAoperationXmlString) 
    throws UpdateIFAoperationServiceGovernanceException,
           RemoteException {

        UpdateIFAoperation updateIFAoperation = new UpdateIFAoperation();
        updateIFAoperation.setUpdatedInfo(updateIFAoperationXmlString);
        
        UpdateIFAoperationResponse updateIFAoperationResponse = 
            stub.updateIFAoperation(updateIFAoperation);

        //prints the artifact id of the added iFAoperation artifact
        System.out.println(updateIFAoperationResponse.get_return());
        
        return updateIFAoperationResponse.get_return();    
    }    
    
    private static String[] getFileNames(File usersDir) {
        File[] files = usersDir.listFiles();
    
        String[] fileNames = new String[files.length];
        
        int i = 0;
        for (File file : files) {
            fileNames[i++] = file.getPath();
        }
        
        return fileNames;
    }
    
    
    private static Workbook getWorkbook(String fileName) 
    throws Exception {
        
        Workbook workbook = null;
        
        InputStream is = new BufferedInputStream(new FileInputStream(fileName));
                
        if (fileName.toLowerCase().contains(("xlsx".toLowerCase()))) {
            workbook = new XSSFWorkbook(is);
        } 
        else {
            POIFSFileSystem fs = new POIFSFileSystem(is);
            workbook = new HSSFWorkbook(fs);
        }
        is.close();
        
        return workbook;
    }
    
    private static Vector getOperationNames(Workbook workbook) {
        
        // list operation names on ApplicationConnection sheet column 6
        Sheet appConnSheet = workbook.getSheet(APP_CONN_SHEET_NAME);
        
        Vector operNames = new Vector();
                
        // operations find on 6th column from 3rd row.
        for (int i = APP_CONN_SHEET_FIRST_DATA_ROW_NUM; i < appConnSheet.getLastRowNum(); i++) {
            Row row = appConnSheet.getRow(i);            
            // operationName is in 6th column, so 6th cell in every row
            try {
                String operName = getCellValue(
                    row.getCell(
                        propCellIndex(
                            appConnProps.getProperty(
                                "overview_operationName"), 
                                appConnSheet.getRow(APP_CONN_SHEET_FIELD_NAMES_ROW_NUM)
                        )
                    )
                );//6
                if (!operName.equals("")) {
                    operNames.add(operName);
                }                
            } catch (IllegalArgumentException iae) {
                System.out.println(iae.getMessage() + "\n" +row.toString());                
            }           
        }
        return operNames;
    }
  
    private static Map<String, String> getOperationAttributeMap(
        Workbook workbook, String operationName) 
    throws Exception {

        // get row of operation on ApplicationConnection sheet, fill attributes
        Sheet appConnSheet = workbook.getSheet(APP_CONN_SHEET_NAME);
        
        Map<String, String> attributeMap = new HashMap<>();
        // roll over on operations in Appl Conn sheet
        for (int i = APP_CONN_SHEET_FIRST_DATA_ROW_NUM; i < appConnSheet.getLastRowNum(); i++) {
            Row row = appConnSheet.getRow(i);            
            
            if (operationName.equalsIgnoreCase(
                getCellValue(
                    row.getCell(
                        propCellIndex(
                            appConnProps.getProperty("overview_operationName"), 
                            appConnSheet.getRow(APP_CONN_SHEET_FIELD_NAMES_ROW_NUM)
                        )
                    )
                )
                )) {
                
                Enumeration<Object> props = appConnProps.keys();
                while (props.hasMoreElements()) {
                    putAttribute(
                        attributeMap, 
                        appConnSheet, 
                        row, 
                        appConnProps, 
                        (String)props.nextElement()
                    );                    
                }                                
                 
                break;
            }
        }
        
        // goto sheet of operation fetch input/output data from all rows
        Sheet operationSheet = workbook.getSheet(operationName);          
        // if no opertaion sheet exists in workbook...  
        if (operationSheet == null) {
            return attributeMap;
        }
        
        boolean inputLine = false;
        int inputParamIndex = 0;
        boolean outputLine = false;
        int outputParamIndex = 0;
        // 9th row is the first real data row...
        for (int i = OPER_SHEET_FIELD_NAMES_ROW_NUM; i < operationSheet.getLastRowNum(); i++) {
            Row row = operationSheet.getRow(i);        
            
            if (getCellValue(row.getCell(1)).contains("Input")) {
                inputLine = true;
            }
            
            if (getCellValue(row.getCell(1)).contains("Output")) {
                outputLine = true;
//                inputLine = false;
            }
                             // input data name is not empty
            if (inputLine && row.getCell(2) != null) {
                
                Enumeration<Object> props = inputProps.keys();
                while (props.hasMoreElements()) {
                    putAttribute(
                        attributeMap, 
                        operationSheet, 
                        row, 
                        inputProps, 
                        (String)props.nextElement(), 
                        inputParamIndex
                    );                    
                }
                
                inputParamIndex++;
            }                 // output data name is not empty
            if (outputLine && row.getCell(2) != null) {

                Enumeration<Object> props = outputProps.keys();
                while (props.hasMoreElements()) {
                    putAttribute(
                        attributeMap, 
                        operationSheet, 
                        row, 
                        outputProps, 
                        (String)props.nextElement(), 
                        outputParamIndex
                    );                    
                }
                                
                outputParamIndex++;
            }       
        }
        
        return attributeMap;
    }
    
    private static void putAttribute(
        Map<String, String> attributeMap_, Sheet sheet_, Row row_, 
        Properties props_, String attrName, int... intArray) {
      
        try {
            attributeMap_.put(
                attrName +((intArray.length > 0) ? String.valueOf(intArray[0]) : ""), 
                getCellValue(
                    row_.getCell(
                        propCellIndex(
                            props_.getProperty(attrName), 
                            sheet_.getRow(
                                ((intArray.length > 0) 
                                    ? DATA_FIELD_NAMES_ROW_NUM 
                                    : APP_CONN_SHEET_FIELD_NAMES_ROW_NUM
                                )
                            )
                        )
                    )
                )
            );   
        } catch (IllegalArgumentException iae) {
            attributeMap_.put(
                attrName +((intArray.length > 0) ? String.valueOf(intArray[0]) : ""), 
                ""
            );             
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell != null && cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return String.valueOf((int)cell.getNumericCellValue());
        } else {
            return "";
        }
    }    
    
    private static String buildXMLStringFromAttributeMap(Map<String, String> attributeMap) {       
        String templateString = 
"<metadata xmlns=\"http://www.wso2.org/governance/metadata\">"+
"    <overview>"+
"        <description>overview_description</description>"+
"        <interfaceId>overview_interfaceId</interfaceId>"+
"        <serviceName>overview_serviceName</serviceName>"+
"        <operationName>overview_operationName</operationName>"+
"    </overview>"+
"    <application>"+
"        <caller>application_caller</caller>"+
"        <destination>application_destination</destination>"+
"        <direction>application_direction</direction>"+
"    </application>"+
"    <business>"+
"        <businessObjects>business_businessObjects</businessObjects>"+
"        <businessObjectsDetails>business_businessObjectsDetails</businessObjectsDetails>"+
"    </business>"+                
"    <technical>"+
"        <isPersisted>technical_isPersisted</isPersisted>"+
"        <dependencyType>technical_dependencyType</dependencyType>"+
"        <frequency>technical_frequency</frequency>"+
"        <initiator>technical_initiator</initiator>"+
"        <operation>technical_operation</operation>"+
"        <interfaceTechnology>technical_interfaceTechnology</interfaceTechnology>"+
"    </technical>"+
"    <design>"+
"        <newExistingFlag>design_newExistingFlag</newExistingFlag>"+
"        <designComment>design_designComment</designComment>"+
"    </design>"+
"</metadata>";

        String templateStringInput =         
"    <inputData>"+
"        <name>inputData_name</name>"+
"        <sourceSystem>inputData_sourceSystem</sourceSystem>"+
"        <objectType>inputData_objectType</objectType>"+
"        <length>inputData_length</length>"+
"        <lov>inputData_lov</lov>"+
"        <multiplicity>inputData_multiplicity</multiplicity>"+
"        <description>inputData_description</description>"+
"    </inputData>";
        
        String templateStringOutput =
"    <outputData>"+
"        <name>outputData_name</name>"+
"        <sourceSystem>outputData_sourceSystem</sourceSystem>"+
"        <objectType>outputData_objectType</objectType>"+
"        <length>outputData_length</length>"+
"        <lov>outputData_lov</lov>"+
"        <multiplicity>outputData_multiplicity</multiplicity>"+
"        <description>outputData_description</description>"+
"    </outputData>";        
        
        templateString = addAttrToXmlString(
            "application_caller", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "application_destination", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "application_direction", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "overview_description", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "overview_interfaceId", templateString, attributeMap);
        
        templateString = addAttrToXmlString(
            "overview_serviceName", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "overview_operationName", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "business_businessObjects", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "business_businessObjectsDetails", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_isPersisted", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_dependencyType", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_frequency", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_initiator", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_operation", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_interfaceTechnology", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "technical_interfaceTechnologyComment", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "design_designComment", templateString, attributeMap);

        templateString = addAttrToXmlString(
            "design_newExistingFlag", templateString, attributeMap);
        
        int i = 0;
        while (attributeMap.get("inputData_name" +i) != null) {
            templateString = templateString.replace(
                "</metadata>", templateStringInput +"</metadata>");

            templateString = addAttrToXmlString(
                "inputData_name", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "inputData_sourceSystem", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "inputData_objectType", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "inputData_lov", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "inputData_multiplicity", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "inputData_description", templateString, attributeMap, i);
            
            i++;
        }
                
        i = 0;        
        while (attributeMap.get("outputData_name" +i) != null) {
            templateString = templateString.replace(
                "</metadata>", templateStringOutput +"</metadata>");
            
            templateString = addAttrToXmlString(
                "outputData_name", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "outputData_sourceSystem", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "outputData_objectType", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "outputData_lov", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "outputData_multiplicity", templateString, attributeMap, i);

            templateString = addAttrToXmlString(
                "outputData_description", templateString, attributeMap, i);
            
            i++;
        }        
        
        return templateString;
    }
    
    private static String addAttrToXmlString(
        String attr, String attrXmlString, Map<String, String> attributeMap, int... intArray) {

        try {
            attrXmlString = attrXmlString.replace(
                attr, 
                attributeMap.get(
                    attr +((intArray.length > 0) ? String.valueOf(intArray[0]) : "")
                )
            );
        } catch (NullPointerException npe) {
            // no attribute found...
        }            
        
        return attrXmlString;
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
    
    private static int propCellIndex(String propertyValue, Row row) {        
        Iterator<Cell> cellIter = row.cellIterator();
        while (cellIter.hasNext()) {
            Cell cell = cellIter.next();
            if (cell.getStringCellValue().trim().equalsIgnoreCase(propertyValue)) {
                
                return cell.getColumnIndex();
            }
        }
        
        return -1;
    }        
        
}
