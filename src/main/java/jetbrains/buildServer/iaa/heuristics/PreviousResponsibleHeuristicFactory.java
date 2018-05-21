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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class PreviousResponsibleHeuristicFactory implements HeuristicFactory {

  @NotNull private final InvestigationsManager myInvestigationsManager;

  public PreviousResponsibleHeuristicFactory(@NotNull InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  @Override
  public Heuristic createHeuristic(final SBuild sBuild,
                                   final List<STestRun> applicableTestRuns,
                                   final List<BuildProblem> applicableBuildProblems) {
    assert sBuild.getBuildType() != null;

    SProject project = sBuild.getBuildType().getProject();
    List<STest> applicableTests = applicableTestRuns.stream().map(STestRun::getTest).collect(Collectors.toList());
    HashMap<Long, User> testId2Responsible = myInvestigationsManager.findInAudit(applicableTests, project);

    return new PreviousResponsibleHeuristic(myInvestigationsManager, testId2Responsible, sBuild);
  }
}
