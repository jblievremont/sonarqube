package org.sonar.plugins.dependent;

import org.sonar.api.SonarPlugin;
import org.sonar.plugins.base.api.BaseApi;
import java.util.Collections;
import java.util.List;

public class DependentPlugin extends SonarPlugin {

  public DependentPlugin() {
    // uses a class that is exported by base-plugin
    new BaseApi().doNothing();
  }

  public List getExtensions() {
    return Collections.emptyList();
  }
}
