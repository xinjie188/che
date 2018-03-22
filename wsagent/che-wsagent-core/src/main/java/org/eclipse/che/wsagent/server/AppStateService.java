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
package org.eclipse.che.wsagent.server;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.fs.server.WsPathUtils.absolutize;
import static org.eclipse.che.api.fs.server.WsPathUtils.resolve;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

import com.google.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.fs.server.FsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@Path("app/state")
public class AppStateService {
  private static final String USER_DIR_PREFIX = "user_";
  private static final String APP_STATE_HOLDER = "appState";

  private static final Logger LOG = LoggerFactory.getLogger(AppStateService.class);

  private FsManager fsManager;

  @Inject
  public AppStateService(FsManager fsManager) {
    this.fsManager = fsManager;
  }

  @GET
  @Path("/{userId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public String getAppState(@PathParam("userId") String userId)
      throws ServerException, BadRequestException {

    String appStateHolderPath = getAppStateHolderPath(userId);
    try {
      if (fsManager.existsAsFile(appStateHolderPath)) {
        return fsManager.readAsString(appStateHolderPath);
      }
    } catch (ServerException | NotFoundException | ConflictException e) {
      LOG.error("Can not get app state for user %s, the reason is: %s", userId, e.getCause());
      throw new ServerException("Can not save app state for user " + userId);
    }
    return "";
  }

  @POST
  @Path("update/{userId}")
  @Consumes(APPLICATION_JSON)
  public void saveState(@PathParam("userId") String userId, String json)
      throws ServerException, BadRequestException {

    String appStateHolderPath = getAppStateHolderPath(userId);
    try {
      if (fsManager.existsAsFile(appStateHolderPath)) {
        fsManager.update(appStateHolderPath, json);
      } else {
        fsManager.createFile(appStateHolderPath, json, false, true);
      }
    } catch (NotFoundException | ConflictException e) {
      LOG.error("Can not save app state for user %s, the reason is: %s", userId, e.getCause());

      throw new ServerException("Can not save app state for user " + userId);
    }
  }

  private String getAppStateHolderPath(String userId) throws BadRequestException {
    if (isNullOrEmpty(userId)) {
      throw new BadRequestException("User ID should be defined");
    }

    String userDir = USER_DIR_PREFIX + userId;
    return resolve(absolutize(CHE_DIR), format("/%s/%s", userDir, APP_STATE_HOLDER));
  }
}
