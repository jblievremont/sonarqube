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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.metric.Metric;

import static com.google.common.base.Optional.of;

public class BatchMeasureToMeasure {
  private final RuleCache ruleCache;

  public BatchMeasureToMeasure(RuleCache ruleCache) {
    this.ruleCache = ruleCache;
  }

  public Optional<Measure> toMeasure(@Nullable BatchReport.Measure batchMeasure, Metric metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    Measure.NewMeasureBuilder builder = createBuilder(batchMeasure);
    String data = batchMeasure.hasStringValue() ? batchMeasure.getStringValue() : null;
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(builder, batchMeasure, data);
      case LONG:
        return toLongMeasure(builder, batchMeasure, data);
      case DOUBLE:
        return toDoubleMeasure(builder, batchMeasure, data);
      case BOOLEAN:
        return toBooleanMeasure(builder, batchMeasure, data);
      case STRING:
        return toStringMeasure(builder, batchMeasure);
      case LEVEL:
        return toLevelMeasure(builder, batchMeasure);
      case NO_VALUE:
        return toNoValueMeasure(builder, batchMeasure);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private Measure.NewMeasureBuilder createBuilder(BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasCharactericId() && batchMeasure.hasRuleKey()) {
      throw new IllegalArgumentException("Measure with both characteristicId and ruleKey are not supported");
    }
    if (batchMeasure.hasCharactericId()) {
      return Measure.newMeasureBuilder().forCharacteristic(batchMeasure.getCharactericId());
    }
    if (batchMeasure.hasRuleKey()) {
      RuleDto ruleDto = ruleCache.get(RuleKey.parse(batchMeasure.getRuleKey()));
      return Measure.newMeasureBuilder().forRule(ruleDto.getId());
    }
    return Measure.newMeasureBuilder();
  }

  private static Optional<Measure> toIntegerMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasIntValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(batchMeasure.getIntValue(), data));
  }

  private static Optional<Measure> toLongMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasLongValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(batchMeasure.getLongValue(), data));
  }

  private static Optional<Measure> toDoubleMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasDoubleValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(batchMeasure.getDoubleValue(), data));
  }

  private static Optional<Measure> toBooleanMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasBooleanValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(batchMeasure.getBooleanValue(), data));
  }

  private static Optional<Measure> toStringMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(batchMeasure.getStringValue()));
  }
  
  private static Optional<Measure> toLevelMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    Optional<Measure.Level> level = Measure.Level.toLevel(batchMeasure.getStringValue());
    if (!level.isPresent()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(setCommonProperties(builder, batchMeasure).create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    return of(setCommonProperties(builder, batchMeasure).createNoValue());
  }

  private static Measure.NewMeasureBuilder setCommonProperties(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasAlertStatus()) {
      Optional<Measure.Level> qualityGateStatus = Measure.Level.toLevel(batchMeasure.getAlertStatus());
      if (qualityGateStatus.isPresent()) {
        String text = batchMeasure.hasAlertText() ? batchMeasure.getAlertText() : null;
        builder.setQualityGateStatus(new QualityGateStatus(qualityGateStatus.get(), text));
      }
    }
    if (hasAnyVariation(batchMeasure))  {
      builder.setVariations(createVariations(batchMeasure));
    }
    return builder;
  }

  private static boolean hasAnyVariation(BatchReport.Measure batchMeasure) {
    return batchMeasure.hasVariationValue1()
        || batchMeasure.hasVariationValue2()
        || batchMeasure.hasVariationValue3()
        || batchMeasure.hasVariationValue4()
        || batchMeasure.hasVariationValue5();
  }

  private static MeasureVariations createVariations(BatchReport.Measure batchMeasure) {
    return new MeasureVariations(
        batchMeasure.hasVariationValue1() ? batchMeasure.getVariationValue1() : null,
        batchMeasure.hasVariationValue2() ? batchMeasure.getVariationValue2() : null,
        batchMeasure.hasVariationValue3() ? batchMeasure.getVariationValue3() : null,
        batchMeasure.hasVariationValue4() ? batchMeasure.getVariationValue4() : null,
        batchMeasure.hasVariationValue5() ? batchMeasure.getVariationValue5() : null
    );
  }

}
