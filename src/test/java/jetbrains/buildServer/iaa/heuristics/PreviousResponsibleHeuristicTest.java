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

package jetbrains.buildServer.iaa.heuristics;

import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.processing.HeuristicContext;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class PreviousResponsibleHeuristicTest extends BaseTestCase {

  private PreviousResponsibleHeuristic myHeuristic;
  private InvestigationsManager myInvestigationsManager;
  private SBuild mySBuild;
  private SProject mySProject;
  private BuildProblem myBuildProblem;
  private User myUser;
  private STest mySTest;
  private STestRun mySTestRun;
  private HeuristicContext myBuildHeuristicContext;
  private HeuristicContext myTestHeuristicContext;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myInvestigationsManager = Mockito.mock(InvestigationsManager.class);
    mySBuild = Mockito.mock(SBuild.class);
    final SBuildType sBuildType = Mockito.mock(jetbrains.buildServer.serverSide.SBuildType.class);
    mySProject = Mockito.mock(SProject.class);
    myBuildProblem = Mockito.mock(BuildProblem.class);
    final BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    myUser = Mockito.mock(User.class);
    mySTest = Mockito.mock(STest.class);

    myHeuristic = new PreviousResponsibleHeuristic(myInvestigationsManager);
    when(myBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("Type");
    when(mySBuild.getFullName()).thenReturn("Full SBuild Name");
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(myUser);
    when(mySBuild.getBuildType()).thenReturn(sBuildType);
    when(sBuildType.getProject()).thenReturn(mySProject);
    when(mySTest.getTestNameId()).thenReturn(12982318457L);
    when(mySTest.getProjectId()).thenReturn("2134124");
    mySTestRun = Mockito.mock(STestRun.class);
    when(mySTestRun.getTest()).thenReturn(mySTest);
    myBuildHeuristicContext =
      new HeuristicContext(mySBuild, mySProject, Collections.singletonList(myBuildProblem), Collections.emptyList());
    myTestHeuristicContext =
      new HeuristicContext(mySBuild, mySProject, Collections.emptyList(), Collections.singletonList(mySTestRun));
  }

  public void TestBuildProblemInfo_ResponsibleFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(myUser);

    HeuristicResult result = myHeuristic.findResponsibleUser(myBuildHeuristicContext);

    Assert.assertFalse(result.isEmpty());
    Responsibility responsibility = result.getResponsibility(myBuildProblem);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUser);
  }

  public void TestBuildProblemInfo_ResponsibleNotFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(null);

    HeuristicResult result = myHeuristic.findResponsibleUser(myBuildHeuristicContext);
//
    Assert.assertTrue(result.isEmpty());
  }

  public void TestTestProblemInfo_ResponsibleFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, mySTest)).thenReturn(myUser);

    HeuristicResult result = myHeuristic.findResponsibleUser(myTestHeuristicContext);

    Assert.assertFalse(result.isEmpty());
    Assert.assertNotNull(result.getResponsibility(mySTestRun));
    Responsibility responsibility = result.getResponsibility(mySTestRun);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUser);
  }

  public void TestTestProblemInfo_ResponsibleNotFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, mySTest)).thenReturn(null);

    HeuristicResult result = myHeuristic.findResponsibleUser(myTestHeuristicContext);

    Assert.assertTrue(result.isEmpty());
  }
}