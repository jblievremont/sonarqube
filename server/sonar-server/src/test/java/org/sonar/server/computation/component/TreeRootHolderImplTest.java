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
package org.sonar.server.computation.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeRootHolderImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  TreeRootHolderImpl treeRootHolder = new TreeRootHolderImpl();
  Component project = DumbComponent.DUMB_PROJECT;

  @Test
  public void setRoot_throws_NPE_if_arg_is_null() {
    thrown.expect(NullPointerException.class);
    treeRootHolder.setRoot(null);
  }

  @Test
  public void getRoot_throws_ISE_if_root_has_not_been_set_yet() {
    thrown.expect(IllegalStateException.class);
    treeRootHolder.getRoot();
  }

  @Test
  public void verify_setRoot_getRoot() {
    treeRootHolder.setRoot(project);
    assertThat(treeRootHolder.getRoot()).isSameAs(project);
  }

  @Test
  public void get_by_ref() throws Exception {
    Component file = DumbComponent.builder(Component.Type.FILE, 4).build();
    Component directory = DumbComponent.builder(Component.Type.DIRECTORY, 3).addChildren(file).build();
    Component module = DumbComponent.builder(Component.Type.MODULE, 2).addChildren(directory).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).addChildren(module).build();
    treeRootHolder.setRoot(project);

    assertThat(treeRootHolder.getComponentByRef(1)).isEqualTo(project);
    assertThat(treeRootHolder.getComponentByRef(2)).isEqualTo(module);
    assertThat(treeRootHolder.getComponentByRef(3)).isEqualTo(directory);
    assertThat(treeRootHolder.getComponentByRef(4)).isEqualTo(file);
  }

  @Test
  public void fail_to_get_by_ref_if_root_not_set() throws Exception {
    thrown.expect(IllegalStateException.class);
    treeRootHolder.getComponentByRef(project.getRef());
  }

  @Test
  public void fail_to_get_by_ref_if_ref_not_found() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    treeRootHolder.setRoot(project);
    treeRootHolder.getComponentByRef(123);
  }
}
