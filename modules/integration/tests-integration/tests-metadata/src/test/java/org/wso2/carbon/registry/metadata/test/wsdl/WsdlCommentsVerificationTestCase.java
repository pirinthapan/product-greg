/*
 *Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.registry.metadata.test.wsdl;

import org.apache.axis2.AxisFault;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.endpoints.dataobjects.Endpoint;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.wsdls.WsdlManager;
import org.wso2.carbon.governance.api.wsdls.dataobjects.Wsdl;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.info.stub.RegistryExceptionException;
import org.wso2.carbon.registry.info.stub.beans.utils.xsd.Comment;
import org.wso2.carbon.registry.info.stub.beans.xsd.CommentBean;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.greg.integration.common.clients.InfoServiceAdminClient;
import org.wso2.greg.integration.common.utils.GREGIntegrationBaseTest;
import org.wso2.greg.integration.common.utils.RegistryProviderUtil;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import static org.testng.Assert.*;

public class WsdlCommentsVerificationTestCase extends GREGIntegrationBaseTest{

    private Wsdl wsdl;
    private InfoServiceAdminClient infoServiceAdminclient;
    private WsdlManager wsdlManager;
    private WSRegistryServiceClient wsRegistry;
    private String sessionCookie;

    @BeforeClass(groups = "wso2.greg", alwaysRun = true)
    public void initialize() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_USER);
        sessionCookie = new LoginLogoutClient(automationContext).login();
        infoServiceAdminclient =
                new InfoServiceAdminClient(backendURL,sessionCookie);

        RegistryProviderUtil provider = new RegistryProviderUtil();
        wsRegistry = provider.getWSRegistry(automationContext);
        Registry governanceRegistry = provider.getGovernanceRegistry(wsRegistry, automationContext);
        wsdlManager = new WsdlManager(governanceRegistry);

    }


    @Test(groups = "wso2.greg", description = "comments verification")
    public void testAddWSDL() throws RemoteException,
                                     ResourceAdminServiceExceptionException, GovernanceException,
                                     MalformedURLException {

        wsdl = wsdlManager
                .newWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/platform-integration/"
                         + "platform-automated-test-suite/org.wso2.carbon.automation.test.repo/src/main/resources/artifacts/"
                         + "GREG/wsdl/echo.wsdl");

        wsdl.addAttribute("version", "1.0.0");
        wsdl.addAttribute("author", "Aparna");
        wsdl.addAttribute("description", "added large wsdl using url");
        wsdlManager.addWsdl(wsdl);
        wsdlManager.updateWsdl(wsdl);
        assertFalse(wsdl.getId().isEmpty());
        assertNotNull(wsdl);
        assertTrue(wsdl.getAttribute("author").contentEquals("Aparna"));

    }

    @Test(groups = "wso2.greg", description = "Comments Verification", dependsOnMethods = "testAddWSDL")
    public void testCommentVerification()
            throws AxisFault, RegistryException, RegistryExceptionException {

        infoServiceAdminclient.addComment(
                "This wsdl is added to verify the comments",
                "/_system/governance" + wsdl.getPath(), sessionCookie);
        wsdlManager.updateWsdl(wsdl);
        CommentBean commentBean = infoServiceAdminclient.getComments(
                "/_system/governance" + wsdl.getPath(), sessionCookie);

        boolean status = false;
        for(Comment comment:commentBean.getComments()){
            if(comment.getContent().contains("This wsdl is added to verify the comments")){
                status = true;
            }
        }

        assertTrue(status, "Added comment not found");
        infoServiceAdminclient.removeComment(commentBean.getComments()[0]
                                                     .getCommentPath(), sessionCookie);

    }

    @AfterClass(groups = "wso2.greg", alwaysRun = true, description = "cleaning up the artifacts added")
    public void tearDown() throws RegistryException {
        String pathPrefix = "/_system/governance";
        Endpoint[] endpoints;
        endpoints = wsdl.getAttachedEndpoints();
        String previousGovernanceArtifactPath = "to prevent re-deleting errors";
        
        GovernanceArtifact[] governanceArtifacts = wsdl.getDependents();
        for (GovernanceArtifact tmpGovernanceArtifact : governanceArtifacts) {
            if (!tmpGovernanceArtifact.getPath().contentEquals(previousGovernanceArtifactPath)) {
                wsRegistry.delete(pathPrefix + tmpGovernanceArtifact.getPath());
            }
            previousGovernanceArtifactPath = tmpGovernanceArtifact.getPath();
        }

        for (Endpoint tmpEndpoint : endpoints) {
        	GovernanceArtifact[] dependentArtifacts =  tmpEndpoint.getDependents();
        	for (GovernanceArtifact tmpGovernanceArtifact : dependentArtifacts) {
                wsRegistry.delete(pathPrefix + tmpGovernanceArtifact.getPath());
            }
            wsRegistry.delete(pathPrefix + tmpEndpoint.getPath());
        }
        wsRegistry = null;
        wsdl = null;
        wsdlManager = null;
        infoServiceAdminclient = null;

    }
}
