
/**
 * UpdateIFAoperationServiceGovernanceException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v20  Built on : Aug 19, 2016 (07:04:18 UTC)
 */

package org.apache.ws.axis2;

public class UpdateIFAoperationServiceGovernanceException extends java.lang.Exception{

    private static final long serialVersionUID = 1518595548631L;
    
    private org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperationServiceGovernanceException faultMessage;

    
        public UpdateIFAoperationServiceGovernanceException() {
            super("UpdateIFAoperationServiceGovernanceException");
        }

        public UpdateIFAoperationServiceGovernanceException(java.lang.String s) {
           super(s);
        }

        public UpdateIFAoperationServiceGovernanceException(java.lang.String s, java.lang.Throwable ex) {
          super(s, ex);
        }

        public UpdateIFAoperationServiceGovernanceException(java.lang.Throwable cause) {
            super(cause);
        }
    

    public void setFaultMessage(org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperationServiceGovernanceException msg){
       faultMessage = msg;
    }
    
    public org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperationServiceGovernanceException getFaultMessage(){
       return faultMessage;
    }
}
    