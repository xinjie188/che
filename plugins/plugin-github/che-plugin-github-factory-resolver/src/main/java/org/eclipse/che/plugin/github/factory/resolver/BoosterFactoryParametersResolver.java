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

import static java.util.Collections.singletonList;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.factory.server.FactoryParametersResolver;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RecipeDto;
import org.eclipse.che.api.workspace.shared.dto.ServerConfigDto;
import org.eclipse.che.plugin.urlfactory.ProjectConfigDtoMerger;
import org.eclipse.che.plugin.urlfactory.URLFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Florent Benoit */
public class BoosterFactoryParametersResolver implements FactoryParametersResolver {

  private static final Logger LOG = LoggerFactory.getLogger(BoosterFactoryParametersResolver.class);

  /** Parameter name. */
  protected static final String BOOSTER_PARAMETER_NAME = "booster";

  /** Parser which will allow to check validity of URLs and create objects. */
  @Inject private GithubURLParser githubUrlParser;

  /** Builder allowing to build objects from github URL. */
  @Inject private GithubSourceStorageBuilder githubSourceStorageBuilder;

  @Inject private URLFactoryBuilder urlFactoryBuilder;

  /** ProjectDtoMerger */
  @Inject private ProjectConfigDtoMerger projectConfigDtoMerger;

  @Inject private BoosterPropertiesMapping boosterPropertiesMapping;

  @Inject private BoosterHelper boosterHelper;

  /**
   * Resolver acceptance based on the given parameters.
   *
   * @param factoryParameters map of parameters dedicated to factories
   * @return true if it will be accepted by the resolver implementation or false if it is not
   *     accepted
   */
  @Override
  public boolean accept(Map<String, String> factoryParameters) {
    // Check if url parameter is a github URL
    return factoryParameters.containsKey(BOOSTER_PARAMETER_NAME)
        && githubUrlParser.isValid(factoryParameters.get(BOOSTER_PARAMETER_NAME));
  }

  /**
   * Create factory object based on provided parameters
   *
   * @param factoryParameters map containing factory data parameters provided through URL
   * @throws BadRequestException when data are invalid
   */
  @Override
  public FactoryDto createFactory(Map<String, String> factoryParameters)
      throws BadRequestException {
    // no need to check null value of url parameter as accept() method has performed the check
    final GithubUrl githubUrl = githubUrlParser.parse(factoryParameters.get("booster"));

    // create default factory
    FactoryDto factory = newDto(FactoryDto.class).withV("4.0");

    // add workspace configuration
    factory.setWorkspace(
        urlFactoryBuilder.buildWorkspaceConfig(
            githubUrl.getRepository(), githubUrl.getUsername(), githubUrl.dockerFileLocation()));

    // Compute project configuration
    ProjectConfigDto projectConfigDto =
        newDto(ProjectConfigDto.class)
            .withSource(githubSourceStorageBuilder.build(githubUrl))
            .withName(githubUrl.getRepository())
            .withType("maven")
            .withPath("/".concat(githubUrl.getRepository()));

    String boosterName = null;
    try {
      boosterName = boosterHelper.getBoosterName(githubUrl);
    } catch (NotFoundException e) {
      LOG.error("Unable to get booster", e);
      throw new BadRequestException("Unable to get booster");
    }

    if (boosterName != null) {

      String newImage = boosterPropertiesMapping.getImageName(boosterName);
      if (newImage != null) {
        RecipeDto recipeDto =
            factory
                .getWorkspace()
                .getEnvironments()
                .entrySet()
                .iterator()
                .next()
                .getValue()
                .getRecipe();
        if (recipeDto != null && "dockerimage".equals(recipeDto.getType())) {
          recipeDto.setContent(newImage);
        }
      }

      Map<String, ServerConfigDto> serverConfigDtoList =
          boosterPropertiesMapping.getServers(boosterName);
      if (serverConfigDtoList.size() > 0) {
        MachineConfigDto machineConfigDto =
            factory
                .getWorkspace()
                .getEnvironments()
                .entrySet()
                .iterator()
                .next()
                .getValue()
                .getMachines()
                .entrySet()
                .iterator()
                .next()
                .getValue();
        Map<String, ServerConfigDto> newServers = new HashMap<>();
        newServers.putAll(machineConfigDto.getServers());
        newServers.putAll(serverConfigDtoList);
        machineConfigDto.setServers(newServers);
      }

      factory
          .getWorkspace()
          .getCommands()
          .addAll(boosterPropertiesMapping.getCommands(boosterName));
    }

    // Store project as part of the factory
    factory.getWorkspace().setProjects(singletonList(projectConfigDto));

    return factory;
  }
}
