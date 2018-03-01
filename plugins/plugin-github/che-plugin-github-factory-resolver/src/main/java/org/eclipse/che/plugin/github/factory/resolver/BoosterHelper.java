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

import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.plugin.urlfactory.URLChecker;
import org.eclipse.che.plugin.urlfactory.URLFetcher;
import org.yaml.snakeyaml.Yaml;

/** @author Florent Benoit */
public class BoosterHelper {

  private URLChecker urlChecker;

  private URLFetcher urlFetcher;

  @Inject
  public BoosterHelper(URLChecker urlChecker, URLFetcher urlFetcher) {
    this.urlChecker = urlChecker;
    this.urlFetcher = urlFetcher;
  }

  public String getBoosterName(GithubUrl githubUrl) throws NotFoundException {

    if (!urlChecker.exists(githubUrl.boosterFileLocation())) {
      throw new NotFoundException("No booster at Github URL :" + githubUrl);
    }
    String yamlContent = urlFetcher.fetch(githubUrl.boosterFileLocation());
    Yaml yaml = new Yaml();
    Map map = (Map) yaml.load(yamlContent);
    return (String) map.get("name");
  }
}
