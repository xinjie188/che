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
package org.eclipse.che.ide.statepersistance;

import static org.eclipse.che.ide.statepersistance.AppStateConstants.APP_STATE;
import static org.eclipse.che.ide.statepersistance.AppStateConstants.WORKSPACE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.json.JsonFactory;
import elemental.json.JsonObject;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.util.loging.Log;

/**
 * User preferences was used to storage serialized IDE state. The class provides back compatibility
 * and allows to get IDE state from preferences and clean up them.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class AppStateBackCompatibility {

  private JsonFactory jsonFactory;
  private final PreferencesManager preferencesManager;
  private final AppContext appContext;

  @Inject
  public AppStateBackCompatibility(
      AppContext appContext, JsonFactory jsonFactory, PreferencesManager preferencesManager) {
    this.appContext = appContext;
    this.jsonFactory = jsonFactory;
    this.preferencesManager = preferencesManager;
  }

  /**
   * Allows to get IDE state for current workspace from user preferences.
   *
   * @return IDE state of current workspace or {@code null} when this one is not found
   */
  @Nullable
  JsonObject getAppState() {
    JsonObject allWsState = getAllWorkspacesState();
    if (allWsState == null) {
      return null;
    }

    String wsId = appContext.getWorkspace().getId();
    JsonObject workspaceSettings = allWsState.getObject(wsId);

    return workspaceSettings != null ? workspaceSettings.get(WORKSPACE) : null;
  }

  /**
   * Allows to get states for all workspaces from user preferences.
   *
   * @return app states of all workspaces for current user or {@code null} when these ones are not
   *     found
   */
  @Nullable
  JsonObject getAllWorkspacesState() {
    try {
      String json = preferencesManager.getValue(APP_STATE);
      return jsonFactory.parse(json);
    } catch (Exception e) {
      return null;
    }
  }

  /** Allows to remove IDE state for current workspace from user preferences */
  void removeAppState() {
    JsonObject allWsState = getAllWorkspacesState();
    if (allWsState != null) {
      String wsId = appContext.getWorkspace().getId();
      allWsState.remove(wsId);
      writeToPreferences(allWsState);
    }
  }

  /**
   * Provide ability to write to preferences state for all workspaces. It's used to clean up user
   * preferences
   */
  private Promise<Void> writeToPreferences(JsonObject state) {
    final String json = state.toJson();
    preferencesManager.setValue(APP_STATE, json);
    return preferencesManager
        .flushPreferences()
        .catchError(
            error -> {
              Log.error(
                  AppStateBackCompatibility.class,
                  "Failed to store app's state to user's preferences: " + error.getMessage());
            });
  }
}
