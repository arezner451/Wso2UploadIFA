
/**
 * IFAoperationCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v20  Built on : Aug 19, 2016 (07:04:18 UTC)
 */

    package org.apache.ws.axis2;

    /**
     *  IFAoperationCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class IFAoperationCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public IFAoperationCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public IFAoperationCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for deleteIFAoperation method
            * override this method for handling normal response from deleteIFAoperation operation
            */
           public void receiveResultdeleteIFAoperation(
                    org.apache.ws.axis2.IFAoperationStub.DeleteIFAoperationResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from deleteIFAoperation operation
           */
            public void receiveErrordeleteIFAoperation(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getIFAoperationDependencies method
            * override this method for handling normal response from getIFAoperationDependencies operation
            */
           public void receiveResultgetIFAoperationDependencies(
                    org.apache.ws.axis2.IFAoperationStub.GetIFAoperationDependenciesResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getIFAoperationDependencies operation
           */
            public void receiveErrorgetIFAoperationDependencies(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for addIFAoperation method
            * override this method for handling normal response from addIFAoperation operation
            */
           public void receiveResultaddIFAoperation(
                    org.apache.ws.axis2.IFAoperationStub.AddIFAoperationResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from addIFAoperation operation
           */
            public void receiveErroraddIFAoperation(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getIFAoperation method
            * override this method for handling normal response from getIFAoperation operation
            */
           public void receiveResultgetIFAoperation(
                    org.apache.ws.axis2.IFAoperationStub.GetIFAoperationResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getIFAoperation operation
           */
            public void receiveErrorgetIFAoperation(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateIFAoperation method
            * override this method for handling normal response from updateIFAoperation operation
            */
           public void receiveResultupdateIFAoperation(
                    org.apache.ws.axis2.IFAoperationStub.UpdateIFAoperationResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from updateIFAoperation operation
           */
            public void receiveErrorupdateIFAoperation(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getIFAoperationArtifactIDs method
            * override this method for handling normal response from getIFAoperationArtifactIDs operation
            */
           public void receiveResultgetIFAoperationArtifactIDs(
                    org.apache.ws.axis2.IFAoperationStub.GetIFAoperationArtifactIDsResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getIFAoperationArtifactIDs operation
           */
            public void receiveErrorgetIFAoperationArtifactIDs(java.lang.Exception e) {
            }
                


    }
    