/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.iaa;

import java.util.Arrays;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class BuildApplicabilityCheckerTest extends BaseTestCase {

  private SProject project;
  private BuildProblemResponsibilityEntry responsibilityEntry1;
  private BuildApplicabilityChecker applicabilityChecker;
  private BuildProblemImpl buildProblem;
  private InvestigationsManager investigationsManager;
  private SBuild sBuild;
  private BuildProblemData buildProblemData;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    buildProblem = Mockito.mock(BuildProblemImpl.class);
    project = Mockito.mock(SProject.class);
    sBuild = Mockito.mock(SBuild.class);
    SProject parentProject = Mockito.mock(SProject.class);
    final SProject project2 = Mockito.mock(SProject.class);
    responsibilityEntry1 = Mockito.mock(BuildProblemResponsibilityEntry.class);
    buildProblemData = Mockito.mock(BuildProblemData.class);
    BuildProblemResponsibilityEntry responsibilityEntry2 = Mockito.mock(BuildProblemResponsibilityEntry.class);
    investigationsManager = Mockito.mock(InvestigationsManager.class);

    when(project.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(project.getParentProject()).thenReturn(parentProject);
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(buildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);
    when(buildProblem.isMuted()).thenReturn(false);
    when(buildProblem.isNew()).thenReturn(true);
    when(buildProblem.getAllResponsibilities()).thenReturn(Arrays.asList(responsibilityEntry1, responsibilityEntry2));
    when(investigationsManager.checkUnderInvestigation(project, sBuild, buildProblem)).thenReturn(false);
    when(investigationsManager.checkUnderInvestigation(project2, sBuild, buildProblem)).thenReturn(false);
    applicabilityChecker = new BuildApplicabilityChecker(investigationsManager);
  }

  public void Test_BuildProblemIsMuted() {
    when(buildProblem.isMuted()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNotMuted() {
    when(buildProblem.isMuted()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemNotNew() {
    when(buildProblem.isNew()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNew() {
    when(buildProblem.isNew()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigation() {
    when(investigationsManager.checkUnderInvestigation(project, sBuild, buildProblem)).thenReturn(true);
    when(responsibilityEntry1.getProject()).thenReturn(project);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(investigationsManager.checkUnderInvestigation(project, sBuild, buildProblem)).thenReturn(false);
    when(responsibilityEntry1.getProject()).thenReturn(project);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemHasIncompatibleType() {
    when(buildProblemData.getType()).thenReturn("Incompatible Type");

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemHasCompatibleType() {
    when(buildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);

    boolean isApplicable = applicabilityChecker.check(project, sBuild, buildProblem);

    Assert.assertTrue(isApplicable);
  }
}