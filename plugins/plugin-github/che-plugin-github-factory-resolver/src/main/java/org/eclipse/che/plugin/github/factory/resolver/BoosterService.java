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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.plugin.github.factory.resolver.BoosterPropertiesMapping.VERTX_BOOSTER;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST service to manage booster repositories.
 *
 * @author Florent Benoit
 */
@Path("/booster")
public class BoosterService {

  public static final List<String> AUTHORIZED_BOOSTERS = Arrays.asList(VERTX_BOOSTER);

  private static final Logger LOG = LoggerFactory.getLogger(BoosterService.class);

  private static Response RESPONSE_TRUE = Response.ok("true").build();
  private static Response RESPONSE_FALSE = Response.ok("false").build();

  @Inject private BoosterHelper boosterHelper;

  @Inject private GithubURLParser githubUrlParser;

  // FIXME: need to add error report for each false case
  @GET
  @Path("/check")
  @Produces(APPLICATION_JSON)
  public Response checkHasBooster(@QueryParam("url") String url) throws ApiException {

    if (url == null) {
      return RESPONSE_FALSE;
    }

    if (!(url.startsWith("https://github.com/"))) {
      return RESPONSE_FALSE;
    }

    try {
      String boosterName = boosterHelper.getBoosterName(githubUrlParser.parse(url));
      if (!AUTHORIZED_BOOSTERS.contains(boosterName)) {
        return RESPONSE_FALSE;
      }
    } catch (NotFoundException e) {
      return RESPONSE_FALSE;
    }

    return RESPONSE_TRUE;
  }
}
