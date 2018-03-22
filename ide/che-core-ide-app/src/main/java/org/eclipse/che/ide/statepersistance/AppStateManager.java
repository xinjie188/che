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

import static org.eclipse.che.ide.statepersistance.AppStateConstants.PART_STACKS;
import static org.eclipse.che.ide.statepersistance.AppStateConstants.PERSPECTIVES;
import static org.eclipse.che.ide.statepersistance.AppStateConstants.WORKSPACE;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonFactory;
import elemental.json.JsonObject;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.statepersistance.AppStateServiceClient;
import org.eclipse.che.ide.api.statepersistance.StateComponent;
import org.eclipse.che.ide.util.loging.Log;

/**
 * Responsible for persisting and restoring IDE state across sessions.
 *
 * @author Artem Zatsarynnyi
 * @author Yevhen Vydolob
 * @author Vlad Zhukovskyi
 */
@Singleton
public class AppStateManager {
  private final Provider<PerspectiveManager> perspectiveManagerProvider;
  private final Provider<StateComponentRegistry> stateComponentRegistry;

  private final JsonFactory jsonFactory;
  private final PromiseProvider promises;
  private final AppStateServiceClient appStateService;

  private JsonObject appState;

  @Inject
  public AppStateManager(
      Provider<PerspectiveManager> perspectiveManagerProvider,
      Provider<StateComponentRegistry> stateComponentRegistryProvider,
      JsonFactory jsonFactory,
      PromiseProvider promises,
      AppStateServiceClient appStateService) {
    this.perspectiveManagerProvider = perspectiveManagerProvider;
    this.stateComponentRegistry = stateComponentRegistryProvider;
    this.jsonFactory = jsonFactory;
    this.promises = promises;
    this.appStateService = appStateService;
  }

  public Promise<Void> readState() {
    return appStateService
        .getState()
        .thenPromise(
            (Function<String, Promise<Void>>)
                state -> {
                  try {
                    appState = jsonFactory.parse(state);
                  } catch (Exception e) {
                    // create 'clear' state if any deserializing error occurred
                    appState = jsonFactory.createObject();
                  }
                  return null;
                })
        .catchError(
            error -> {
              appState = jsonFactory.createObject();
            });
  }

  public Promise<Void> persistState() {
    JsonObject newAppState = collectAppStateData();
    if (appState == null || !appState.toJson().equals(newAppState.toJson())) {
      appState = newAppState;
      return appStateService.saveState(newAppState.toJson());
    } else {
      return promises.resolve(null);
    }
  }

  /**
   * Gets cached state for given {@code partStackType}. Use {@link #readState()} first to get not
   * cached state
   */
  @Nullable
  public JsonObject getStateFor(PartStackType partStackType) {
    JsonObject workspaceState = appState.getObject(WORKSPACE);
    if (workspaceState == null || !workspaceState.hasKey(PERSPECTIVES)) {
      return null;
    }

    String perspectiveId = perspectiveManagerProvider.get().getPerspectiveId();
    JsonObject perspectives = workspaceState.getObject(PERSPECTIVES);
    if (!perspectives.hasKey(perspectiveId)) {
      return null;
    }

    JsonObject projectPerspective = perspectives.getObject(perspectiveId);
    if (!projectPerspective.hasKey(PART_STACKS)) {
      return null;
    }

    JsonObject partStacks = projectPerspective.getObject(PART_STACKS);
    if (!partStacks.hasKey(partStackType.name())) {
      return null;
    }

    return partStacks.getObject(partStackType.name());
  }

  Promise<Void> restoreState(JsonObject updatedState) {
    appState = updatedState;
    return restoreState();
  }

  Promise<Void> restoreState() {
    try {
      Promise<Void> sequentialRestore = promises.resolve(null);
      for (String key : appState.keys()) {
        Optional<StateComponent> stateComponent =
            stateComponentRegistry.get().getComponentById(key);
        if (stateComponent.isPresent()) {
          StateComponent component = stateComponent.get();
          Log.debug(getClass(), "Restore state for the component ID: " + component.getId());
          sequentialRestore =
              sequentialRestore.thenPromise(
                  ignored -> component.loadState(appState.getObject(key)));
        }
      }
      return sequentialRestore;
    } catch (JsonException e) {
      Log.error(getClass(), e);
    }
    return promises.resolve(null);
  }

  @NotNull
  JsonObject collectAppStateData() {
    JsonObject newAppState = Json.createObject();
    for (StateComponent entry : stateComponentRegistry.get().getComponents()) {
      try {
        Log.debug(getClass(), "Persist state for the component ID: " + entry.getId());
        newAppState.put(entry.getId(), entry.getState());
      } catch (Exception e) {
        Log.error(getClass(), e);
      }
    }
    return newAppState;
  }
}
