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
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.processing.HeuristicContext;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

public class PreviousResponsibleHeuristic implements Heuristic {

  private InvestigationsManager myInvestigationsManager;

  PreviousResponsibleHeuristic(InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  @NotNull
  @Override
  public String getName() {
    return "Previous Responsible Heuristic";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Assign an investigation to a user if the user was responsible previous time.";
  }

  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();
    SProject sProject = heuristicContext.getProject();
    Iterable<STestRun> sTestRuns = heuristicContext.getTestRuns();

    HashMap<Long, User> testId2Responsible = myInvestigationsManager.findInAudit(sTestRuns, sProject);
    for (STestRun sTestRun : heuristicContext.getTestRuns()) {
      STest sTest = sTestRun.getTest();

      User responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, sTest);

      if (responsibleUser == null) {
        responsibleUser = testId2Responsible.get(sTest.getTestNameId());
      }

      if (responsibleUser != null) {
        String description = String.format("%s you were responsible for the test: `%s` in build `%s` previous time",
                                           Constants.REASON_PREFIX, sTest.getName(), sBuild.getFullName());

        result.addResponsibility(sTestRun, new Responsibility(responsibleUser, description));
      }
    }

    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      User responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem);
      if (responsibleUser != null) {
        String buildProblemType = buildProblem.getBuildProblemData().getType();
        String description =
          String.format("%s you were responsible for the build problem: `%s` in build `%s` previous time",
                        Constants.REASON_PREFIX, buildProblemType, sBuild.getFullName());

        result.addResponsibility(buildProblem, new Responsibility(responsibleUser, description));
      }
    }

    return result;
  }
}
