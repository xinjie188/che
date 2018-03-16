/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.github.factory.resolver;

import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.testng.Assert.assertEquals;

import com.jayway.restassured.response.Response;
import org.eclipse.che.plugin.urlfactory.URLChecker;
import org.eclipse.che.plugin.urlfactory.URLFetcher;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests for {@link BoosterService}.
 *
 * @author Florent Benoit
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class BoosterServiceTest {

  @Spy private URLFetcher urlFetcher = new URLFetcher();

  @Spy private URLChecker urlChecker = new URLChecker();

  @Spy private BoosterHelper boosterHelper = new BoosterHelper(urlChecker, urlFetcher);

  @Spy private GithubURLParser githubURLParser = new GithubURLParserImpl();

  @InjectMocks private BoosterService boosterService;

  @Test
  public void shouldCheckValidProject() throws Exception {

    final Response response =
        given()
            .auth()
            .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
            .when()
            .get(
                SECURE_PATH
                    + "/booster/check?url=https://github.com/openshiftio-vertx-boosters/vertx-health-checks-booster");

    assertEquals(response.getStatusCode(), 200);
    assertEquals(response.getBody().print(), "true");
  }

  @Test
  public void shouldCheckInvalidProject() throws Exception {

    final Response response =
        given()
            .auth()
            .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
            .when()
            .get(SECURE_PATH + "/booster/check?url=https://github.com/eclipse/che");

    assertEquals(response.getStatusCode(), 200);
    assertEquals(response.getBody().print(), "false");
  }
}
