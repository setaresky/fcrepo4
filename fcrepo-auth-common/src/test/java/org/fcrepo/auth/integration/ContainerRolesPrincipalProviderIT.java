/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.integration;

import org.apache.http.auth.BasicUserPrincipal;

import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.fcrepo.auth.common.ServletContainerAuthenticationProvider;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.services.ContainerServiceImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;

import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-test/mocked-fad-repo-2.xml"})
public class ContainerRolesPrincipalProviderIT {

    private static Logger logger =
            getLogger(ContainerRolesPrincipalProviderIT.class);

    @Inject
    private FedoraRepository repo;

    @Inject
    private FedoraAuthorizationDelegate fad;

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    public void testFactory() {
        Assert.assertNotNull(
                "AuthenticationProvider must return a AuthenticationProvider",
                ServletContainerAuthenticationProvider.getInstance());
    }

    @Test
    public void testEmptyPrincipalProvider() throws RepositoryException {
        when(request.getRemoteUser()).thenReturn("fred");
        when(request.getUserPrincipal()).thenReturn(
                new BasicUserPrincipal("fred"));
        when(
                request.isUserInRole(Mockito
                        .eq(ServletContainerAuthenticationProvider.FEDORA_USER_ROLE)))
                .thenReturn(true);
        Mockito.reset(fad);
        when(fad.hasPermission(any(Session.class), any(Path.class), any(String[].class))).thenReturn(true);

        final ServletCredentials credentials =
                new ServletCredentials(request);
        final FedoraSession session = repo.login(credentials);
        final Session jcrSession = getJcrSession(session);
        final Privilege[] rootPrivs =
                jcrSession.getAccessControlManager().getPrivileges("/");
        for (final Privilege p : rootPrivs) {
            logger.debug("got priv: " + p.getName());
        }
        final ContainerService os = new ContainerServiceImpl();
        os.findOrCreate(session, "/myobject");
        verify(fad, atLeastOnce()).hasPermission(any(Session.class), any(Path.class), any(String[].class));
    }


}
