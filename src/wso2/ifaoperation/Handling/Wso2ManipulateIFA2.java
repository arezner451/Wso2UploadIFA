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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.apache.axis2.AxisFault;
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
import org.apache.ws.axis2.DeleteIFAoperationServiceGovernanceException;
import org.apache.ws.axis2.AddIFAoperationServiceGovernanceException;
import org.apache.ws.axis2.GetIFAoperationArtifactIDsServiceGovernanceException;
import org.apache.ws.axis2.IFAoperationStub;
import org.apache.ws.axis2.IFAoperationStub.AddIFAoperation;
import org.apache.ws.axis2.IFAoperationStub.AddIFAoperationResponse;
import org.wso2.carbon.utils.CarbonUtils;

/**
 *
 * @author attila.rezner
 */
public class Wso2ManipulateIFA2 {
    
    // Location of the Excel data
    public static final String DATA_DIR = "c:/TEMP/wso2upload/";
    
    public static final String CARBON_HOME = "C:/java/wso2greg-5.4.0";
    
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
    throws RemoteException, GetIFAoperationArtifactIDsServiceGovernanceException, 
           DeleteIFAoperationServiceGovernanceException, AddIFAoperationServiceGovernanceException, 
           Exception {
              
        String trustStore = CARBON_HOME + File.separator + "repository" + File.separator
                + "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("carbon.repo.write.mode", "true");        
                
        appConnProps = getProperties(
            PROPS_BASE_PATH + File.separator + APP_CONN_PROPS);
        
        inputProps = getProperties(
            PROPS_BASE_PATH + File.separator + INPUT_PROPS);

        outputProps = getProperties(
            PROPS_BASE_PATH + File.separator + OUTPUT_PROPS);        
        
        try {
            
//            ConfigurationContext configContext = 
//                ConfigurationContextFactory.createConfigurationContextFromFileSystem(
//                    AXIS_2_CONFIGURATION);

//            Registry registry = new WSRegistryServiceClient(
//                SERVICE_URL, USERNAME, PASSWORD, configContext);            
            
//        WSRegistryServiceClient wsreg = (WSRegistryServiceClient)registry;
//        Resource res = wsreg.get("/_system/governance/trunk/ifaoperations/CampaignServiceV1.cacheStatusOutboundCampaignV3");
//        System.out.println(new String((byte[])res.getContent()));
            
            Vector excelFileNames = new Vector();
            // Get the work books            
            Workbook[] workbooks = getWorkbooks(new File(DATA_DIR), excelFileNames);

            String axis2Repo = CARBON_HOME + File.separator + "repository/deployment/client";
            String axis2Conf = CARBON_HOME + File.separator + "repository/conf/axis2/axis2_client.xml";
            
            ConfigurationContext configContext = 
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo, axis2Conf);
            
            Wso2ManipulateIFA wso2ManipulateIFA =           //172.17.65.63 - middlewareapibrowser
                new Wso2ManipulateIFA(configContext, "https://localhost:9443/services/IFAoperation", false);
           
            Enumeration excelFileNameIter = excelFileNames.elements();
            
            // roll over all workbooks
            for (Workbook workbook : workbooks) {
                
                String excelFileName = (String)excelFileNameIter.nextElement();
                
                Vector operNames = getOperationNames(workbook);
                                
                Iterator iter = operNames.iterator();
                while (iter.hasNext()) {
                    String operationName = (String)iter.next();
                    
                    System.out.println(
                        "parsing operation " +operationName +" in " +excelFileName);
                    
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
    
    public Wso2ManipulateIFA2(ConfigurationContext configContext,String epr, boolean listener) 
    throws AxisFault{
        
        stub = new IFAoperationStub(configContext,epr,listener);
        CarbonUtils.setBasicAccessSecurityHeaders("admin", "admin", stub._getServiceClient());
    }
    
    public String addIFAoperation(String addIFAoperationXmlString) 
    throws RemoteException, AddIFAoperationServiceGovernanceException {

        AddIFAoperation addIFAoperation = new AddIFAoperation();
        addIFAoperation.setInfo(addIFAoperationXmlString);
        
        AddIFAoperationResponse addIFAoperationResponse = 
            stub.addIFAoperation(addIFAoperation);

        //prints the artifact id of the added iFAoperation artifact
        System.out.println(addIFAoperationResponse.get_return());
        
        return addIFAoperationResponse.get_return();    
    }

    private static Workbook[] getWorkbooks(File usersDir, Vector excelFileNames) 
    throws Exception {
        
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
                        excelFileNames.add(file.getName());
                    } 
                    else {
                        POIFSFileSystem fs = new POIFSFileSystem(ins);
                        workbooks.add(new HSSFWorkbook(fs));
                        excelFileNames.add(file.getName());
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
                    attributeMap = putAttribute(
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
            
            if (inputLine) {  
                
                Enumeration<Object> props = inputProps.keys();
                while (props.hasMoreElements()) {
                    attributeMap = putDataAttribute(
                        attributeMap, 
                        operationSheet, 
                        row, 
                        inputProps, 
                        (String)props.nextElement(), 
                        inputParamIndex
                    );                    
                }
                
                inputParamIndex++;
            } 
            if (outputLine) {

                Enumeration<Object> props = outputProps.keys();
                while (props.hasMoreElements()) {
                    attributeMap = putDataAttribute(
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
    
    private static Map<String, String> putAttribute(
        Map<String, String> attributeMap_, Sheet appConnSheet_, Row row_, 
        Properties props_, String attrName) {
      
        try {
            attributeMap_.put(
                attrName, 
                getCellValue(
                    row_.getCell(
                        propCellIndex(
                            props_.getProperty(attrName), 
                            appConnSheet_.getRow(APP_CONN_SHEET_FIELD_NAMES_ROW_NUM)
                        )
                    )
                )
            );   
        } catch (IllegalArgumentException iae) {
            attributeMap_.put(
                attrName, 
                ""
            );             
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {        
            return attributeMap_;
        }
    }
    
    /*
    *   adds a number to attributeName to make identical in attribute map.
    *   this number is removed during the final xml creation.
    */    
    private static Map<String, String> putDataAttribute(
        Map<String, String> attributeMap_, Sheet dataSheet_, Row row_, 
        Properties props_, String attrName, int j_) {

        try {        
            attributeMap_.put(
                attrName +String.valueOf(j_), 
                getCellValue(
                    row_.getCell(
                        propCellIndex(
                            props_.getProperty(attrName), 
                            dataSheet_.getRow(DATA_FIELD_NAMES_ROW_NUM)
                        )
                    )
                )
            );
        } catch (IllegalArgumentException iae) {
            attributeMap_.put(
                attrName +String.valueOf(j_), 
                ""
            );            
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println(e.getMessage());            
        } finally {               
            return attributeMap_;
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
