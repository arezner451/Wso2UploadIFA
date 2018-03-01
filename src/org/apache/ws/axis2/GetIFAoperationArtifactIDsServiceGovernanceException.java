
/**
 * GetIFAoperationArtifactIDsServiceGovernanceException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v20  Built on : Aug 19, 2016 (07:04:18 UTC)
 */

package org.apache.ws.axis2;

public class GetIFAoperationArtifactIDsServiceGovernanceException extends java.lang.Exception{

    private static final long serialVersionUID = 1518595548645L;
    
    private org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDsServiceGovernanceException faultMessage;

    
        public GetIFAoperationArtifactIDsServiceGovernanceException() {
            super("GetIFAoperationArtifactIDsServiceGovernanceException");
        }

        public GetIFAoperationArtifactIDsServiceGovernanceException(java.lang.String s) {
           super(s);
        }

        public GetIFAoperationArtifactIDsServiceGovernanceException(java.lang.String s, java.lang.Throwable ex) {
          super(s, ex);
        }

        public GetIFAoperationArtifactIDsServiceGovernanceException(java.lang.Throwable cause) {
            super(cause);
        }
    

    public void setFaultMessage(org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDsServiceGovernanceException msg){
       faultMessage = msg;
    }
    
    public org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDsServiceGovernanceException getFaultMessage(){
       return faultMessage;
    }
}
    