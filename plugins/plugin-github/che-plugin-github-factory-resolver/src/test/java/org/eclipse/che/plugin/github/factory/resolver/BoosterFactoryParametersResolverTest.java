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

import static java.util.Collections.singletonMap;
import static org.eclipse.che.plugin.github.factory.resolver.BoosterFactoryParametersResolver.BOOSTER_PARAMETER_NAME;
import static org.eclipse.che.plugin.github.factory.resolver.GithubFactoryParametersResolver.URL_PARAMETER_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RecipeDto;
import org.eclipse.che.api.workspace.shared.dto.ServerConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.plugin.urlfactory.ProjectConfigDtoMerger;
import org.eclipse.che.plugin.urlfactory.URLChecker;
import org.eclipse.che.plugin.urlfactory.URLFactoryBuilder;
import org.eclipse.che.plugin.urlfactory.URLFetcher;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Validate operations performed by the Booster Github Factory service
 *
 * @author Florent Benoit
 */
@Listeners(MockitoTestNGListener.class)
public class BoosterFactoryParametersResolverTest {

  @Spy private BoosterPropertiesMapping boosterPropertiesMapping = new BoosterPropertiesMapping();

  @Spy private URLFetcher urlFetcher = new URLFetcher();

  @Spy private URLChecker urlChecker = new URLChecker();

  @Spy private BoosterHelper boosterHelper = new BoosterHelper(urlChecker, urlFetcher);

  /** Parser which will allow to check validity of URLs and create objects. */
  @Spy private GithubURLParserImpl githubUrlParser = new GithubURLParserImpl();

  /** Converter allowing to convert github URL to other objects. */
  @Spy
  private GithubSourceStorageBuilder githubSourceStorageBuilder = new GithubSourceStorageBuilder();

  /** ProjectDtoMerger */
  @Mock private ProjectConfigDtoMerger projectConfigDtoMerger = new ProjectConfigDtoMerger();

  /** Parser which will allow to check validity of URLs and create objects. */
  @Spy private URLFactoryBuilder urlFactoryBuilder = new URLFactoryBuilder(urlChecker, urlFetcher);

  /** Capturing the project config DTO parameter. */
  @Captor private ArgumentCaptor<ProjectConfigDto> projectConfigDtoArgumentCaptor;

  /** Capturing the parameter when calling {@link URLFactoryBuilder#createFactory(String)} */
  @Captor private ArgumentCaptor<String> jsonFileLocationArgumentCaptor;

  /** Instance of resolver that will be tested. */
  @InjectMocks private BoosterFactoryParametersResolver boosterGithubFactoryParametersResolver;

  /** Check missing parameter name can't be accepted by this resolver */
  @Test
  public void checkMissingParameter() throws BadRequestException {
    Map<String, String> parameters = singletonMap("foo", "this is a foo bar");
    boolean accept = boosterGithubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertFalse(accept);
  }

  /** Check url which is not a booster url can't be accepted by this resolver */
  @Test
  public void checkInvalidParameterAcceptUrl() throws BadRequestException {
    Map<String, String> parameters = singletonMap(URL_PARAMETER_NAME, "http://www.eclipse.org/che");
    boolean accept = boosterGithubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertFalse(accept);
  }

  /** Check not booster github url will be be accepted by this resolver */
  @Test
  public void checkInvalidBoosterAcceptUrl() throws BadRequestException {
    Map<String, String> parameters =
        singletonMap(BOOSTER_PARAMETER_NAME, "https://github.com/codenvy/codenvy.git");
    boolean accept = boosterGithubFactoryParametersResolver.accept(parameters);
    // shouldn't be accepted
    assertTrue(accept);
  }

  /** Check that with a simple valid URL github url it works */
  @Test
  public void shouldReturnGitHubSimpleFactory() throws Exception {

    String githubUrl = "https://github.com/openshiftio-vertx-boosters/vertx-health-checks-booster";

    FactoryDto factoryDto =
        boosterGithubFactoryParametersResolver.createFactory(
            singletonMap(BOOSTER_PARAMETER_NAME, githubUrl));

    WorkspaceConfigDto workspaceConfigDto = factoryDto.getWorkspace();
    assertNotNull(workspaceConfigDto);

    List<CommandDto> commandDtoList = workspaceConfigDto.getCommands();
    assertNotNull(commandDtoList);
    assertEquals(commandDtoList.size(), 1);

    List<ProjectConfigDto> projectConfigDtoList = workspaceConfigDto.getProjects();
    assertNotNull(projectConfigDtoList);
    assertEquals(projectConfigDtoList.size(), 1);
    ProjectConfigDto projectConfigDto = projectConfigDtoList.get(0);

    SourceStorageDto sourceStorageDto = projectConfigDto.getSource();
    assertNotNull(sourceStorageDto);
    assertEquals(sourceStorageDto.getType(), "git");
    assertEquals(sourceStorageDto.getLocation(), githubUrl);
    Map<String, String> sourceParameters = sourceStorageDto.getParameters();
    assertEquals(sourceParameters.size(), 1);
    assertEquals(sourceParameters.get("branch"), "master");

    Map<String, EnvironmentDto> environmentDtoMap = workspaceConfigDto.getEnvironments();
    assertNotNull(environmentDtoMap);
    EnvironmentDto circuitDto = environmentDtoMap.get("vertx-health-checks-booster");
    assertNotNull(circuitDto);
    RecipeDto recipeDto = circuitDto.getRecipe();
    assertNotNull(recipeDto);
    assertEquals(recipeDto.getType(), "dockerimage");
    assertEquals(recipeDto.getContent(), "florentbenoit/vertx-image");

    Map<String, MachineConfigDto> machineConfigDtoMap = circuitDto.getMachines();
    assertNotNull(machineConfigDtoMap);
    assertEquals(machineConfigDtoMap.size(), 1);
    MachineConfigDto machineConfigDto = machineConfigDtoMap.get("ws-machine");
    assertNotNull(machineConfigDto);
    Map<String, ServerConfigDto> serversMap = machineConfigDto.getServers();
    assertNotNull(serversMap);
    ServerConfigDto greeterServiceServer = serversMap.get("api-service");
    assertNotNull(greeterServiceServer);
    assertEquals(greeterServiceServer.getPort(), "8080");
  }
}
