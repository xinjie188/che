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

import static org.eclipse.che.dto.server.DtoFactory.newDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.ServerConfigDto;

/** @author Florent Benoit */
public class BoosterPropertiesMapping {

  public static final String VERTX_BOOSTER = "Vert.x Health Check Example";

  private final Map<String, List<CommandDto>> hardCodedCommands;

  private final Map<String, String> hardCodedImages;

  private final Map<String, Map<String, ServerConfigDto>> hardcodedServers;

  @Inject
  public BoosterPropertiesMapping() {
    this.hardCodedCommands = new HashMap();
    this.hardCodedImages = new HashMap();
    this.hardcodedServers = new HashMap<>();

    Map<String, String> attrs = new HashMap<>();
    attrs.put("goal", "Run");
    attrs.put("previewUrl", "${server.api-service}");
    CommandDto runNameService =
        newDto(CommandDto.class)
            .withName("Run project")
            .withCommandLine(
                "unset JAVA_OPTS && cd ${current.project.path} mvn compile && mvn vertx:run")
            .withType("custom")
            .withAttributes(attrs);

    this.hardCodedCommands.put(VERTX_BOOSTER, Arrays.asList(runNameService));

    this.hardCodedImages.put(VERTX_BOOSTER, "florentbenoit/vertx-image");

    Map<String, ServerConfigDto> fuseServers = new HashMap<>();
    ServerConfigDto serverConfigDtoGreeter =
        newDto(ServerConfigDto.class).withPort("8080").withProtocol("http");
    fuseServers.put("api-service", serverConfigDtoGreeter);
    this.hardcodedServers.put(VERTX_BOOSTER, fuseServers);
  }

  public List<CommandDto> getCommands(String boosterName) {
    List<CommandDto> commandDtoList = new ArrayList<>();
    if (hardCodedCommands.containsKey(boosterName)) {
      return hardCodedCommands.get(boosterName);
    }

    return commandDtoList;
  }

  public String getImageName(String boosterName) {
    return hardCodedImages.get(boosterName);
  }

  public Map<String, ServerConfigDto> getServers(String boosterName) {
    Map<String, ServerConfigDto> servers = new HashMap<>();
    if (hardcodedServers.containsKey(boosterName)) {
      return hardcodedServers.get(boosterName);
    }

    return servers;
  }
}
