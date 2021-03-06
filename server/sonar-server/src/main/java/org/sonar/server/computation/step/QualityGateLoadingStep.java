/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.qualitygate.MutableQualityGateHolder;
import org.sonar.server.computation.qualitygate.QualityGate;
import org.sonar.server.computation.qualitygate.QualityGateService;

import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.PRE_ORDER;

/**
 * This step retrieves the QualityGate for the current {@link ReportQueue.Item} and stores it in
 * {@link MutableQualityGateHolder}.
 */
public class QualityGateLoadingStep implements ComputationStep {
  private static final Logger LOGGER = Loggers.get(QualityGateLoadingStep.class);

  private static final String PROPERTY_QUALITY_GATE = "sonar.qualitygate";

  private final TreeRootHolder treeRootHolder;
  private final ProjectSettingsRepository projectSettingsRepository;
  private final QualityGateService qualityGateService;
  private final MutableQualityGateHolder qualityGateHolder;

  public QualityGateLoadingStep(TreeRootHolder treeRootHolder, ProjectSettingsRepository projectSettingsRepository,
    QualityGateService qualityGateService, MutableQualityGateHolder qualityGateHolder) {
    this.treeRootHolder = treeRootHolder;
    this.projectSettingsRepository = projectSettingsRepository;
    this.qualityGateService = qualityGateService;
    this.qualityGateHolder = qualityGateHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(PROJECT, PRE_ORDER) {
      @Override
      public void visitProject(Component project) {
        executeForProject(project);
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    String projectKey = project.getKey();
    Settings settings = projectSettingsRepository.getProjectSettings(projectKey);
    String qualityGateSetting = settings.getString(PROPERTY_QUALITY_GATE);

    if (qualityGateSetting == null || StringUtils.isBlank(qualityGateSetting)) {
      LOGGER.debug("No quality gate is configured for project " + projectKey);
      qualityGateHolder.setNoQualityGate();
      return;
    }

    try {
      long qualityGateId = Long.parseLong(qualityGateSetting);
      Optional<QualityGate> qualityGate = qualityGateService.findById(qualityGateId);
      if (qualityGate.isPresent()) {
        qualityGateHolder.setQualityGate(qualityGate.get());
      }
      else {
        qualityGateHolder.setNoQualityGate();
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
        String.format("Unsupported value (%s) in property %s", qualityGateSetting, PROPERTY_QUALITY_GATE),
        e);
    }
  }

  @Override
  public String getDescription() {
    return "Retrieve Quality Gate";
  }
}
