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

import com.intellij.openapi.util.Pair;
import java.util.HashMap;
import jetbrains.buildServer.iaa.common.Constants;
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
  private HashMap<Long, User> myTestId2ResponsibleFromAudit;
  private final SBuild mySBuild;
  private final SProject mySProject;

  PreviousResponsibleHeuristic(final InvestigationsManager investigationsManager,
                               final HashMap<Long, User> testId2Responsible,
                               final SBuild sBuild) {
    assert sBuild.getBuildType() != null;

    myInvestigationsManager = investigationsManager;
    myTestId2ResponsibleFromAudit = testId2Responsible;
    mySBuild = sBuild;
    mySProject = sBuild.getBuildType().getProject();
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

  @Override
  public Pair<User, String> findResponsibleUser(@NotNull final STestRun sTestRun) {
    STest sTest = sTestRun.getTest();
    User responsibleUser = myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, sTest);
    String description = String.format("%s you were responsible for the test: `%s` in build `%s` previous time",
                                       Constants.REASON_PREFIX, sTest.getName(), mySBuild.getFullName());
    if (responsibleUser == null) {
      responsibleUser = myTestId2ResponsibleFromAudit.get(sTest.getTestNameId());
    }

    return Pair.create(responsibleUser, description);
  }

  @Override
  public Pair<User, String> findResponsibleUser(@NotNull final BuildProblem buildProblem) {
    User responsibleUser = myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, buildProblem);
    String buildProblemType = buildProblem.getBuildProblemData().getType();
    String description =
      String.format("%s you were responsible for the build problem: `%s` in build `%s` previous time",
                    Constants.REASON_PREFIX, buildProblemType, mySBuild.getFullName());

    if (responsibleUser == null) {
      return null;
    }

    return Pair.create(responsibleUser, description);
  }
}
