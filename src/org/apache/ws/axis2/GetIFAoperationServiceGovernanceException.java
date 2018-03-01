
/**
 * GetIFAoperationServiceGovernanceException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v20  Built on : Aug 19, 2016 (07:04:18 UTC)
 */

package org.apache.ws.axis2;

public class GetIFAoperationServiceGovernanceException extends java.lang.Exception{

    private static final long serialVersionUID = 1518595548679L;
    
    private org.apache.ws.axis2.IFAoperationStub.GetIFAoperationServiceGovernanceException faultMessage;

    
        public GetIFAoperationServiceGovernanceException() {
            super("GetIFAoperationServiceGovernanceException");
        }

        public GetIFAoperationServiceGovernanceException(java.lang.String s) {
           super(s);
        }

        public GetIFAoperationServiceGovernanceException(java.lang.String s, java.lang.Throwable ex) {
          super(s, ex);
        }

        public GetIFAoperationServiceGovernanceException(java.lang.Throwable cause) {
            super(cause);
        }
    

    public void setFaultMessage(org.apache.ws.axis2.IFAoperationStub.GetIFAoperationServiceGovernanceException msg){
       faultMessage = msg;
    }
    
    public org.apache.ws.axis2.IFAoperationStub.GetIFAoperationServiceGovernanceException getFaultMessage(){
       return faultMessage;
    }
}
    