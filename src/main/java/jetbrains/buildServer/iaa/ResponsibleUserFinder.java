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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.iaa.heuristics.Heuristic;
import jetbrains.buildServer.iaa.heuristics.HeuristicFactory;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResponsibleUserFinder {
  private static final Logger LOGGER = Logger.getInstance(ResponsibleUserFinder.class.getName());
  private final SBuild mySBuild;
  private List<Heuristic> myOrderedHeuristics;

  ResponsibleUserFinder(@NotNull final List<HeuristicFactory> orderedHeuristicsFactory,
                        @NotNull final SBuild sBuild,
                        @NotNull final List<STestRun> sTestRuns,
                        @NotNull final List<BuildProblem> buildProblems) {
    myOrderedHeuristics = new ArrayList<>();
    mySBuild = sBuild;
    orderedHeuristicsFactory.forEach(
      heuristicFactory -> myOrderedHeuristics.add(heuristicFactory.createHeuristic(sBuild, sTestRuns, buildProblems)));
  }

  @Nullable
  Pair<User, String> findResponsible(@NotNull STestRun sTestRun) {

    long buildId = mySBuild.getBuildId();
    LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s", buildId));

    Pair<User, String> responsibleUser = null;
    for (Heuristic heuristic : myOrderedHeuristics) {
      LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s with heuristic %s",
                                 buildId, heuristic.getName()));
      responsibleUser = heuristic.findResponsibleUser(sTestRun);
      if (responsibleUser != null) {
        LOGGER.info(String.format("Responsible user %s for failed build #%s has been found according to %s",
                                  responsibleUser.first, buildId, responsibleUser.second));
        break;
      }
    }

    if (responsibleUser == null) {
      LOGGER.info(String.format("Responsible user for failed build #%s not found", buildId));
    }
    return responsibleUser;

  }

  @Nullable
  Pair<User, String> findResponsible(@NotNull BuildProblem buildProblem) {

    long buildId = mySBuild.getBuildId();
    LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s", buildId));

    Pair<User, String> responsibleUser = null;
    for (Heuristic heuristic : myOrderedHeuristics) {
      LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s with heuristic %s",
                                 buildId, heuristic.getName()));
      responsibleUser = heuristic.findResponsibleUser(buildProblem);

      if (responsibleUser != null) {
        LOGGER.info(String.format("Responsible user %s for failed build #%s has been found according to %s",
                                  responsibleUser.first, buildId, responsibleUser.second));
        break;
      }
    }

    if (responsibleUser == null) {
      LOGGER.info(String.format("Responsible user for failed build #%s not found", buildId));
    }
    return responsibleUser;

  }
}
