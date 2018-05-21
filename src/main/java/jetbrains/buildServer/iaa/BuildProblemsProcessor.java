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

import com.intellij.openapi.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.heuristics.HeuristicFactory;
import jetbrains.buildServer.iaa.utils.CustomParameters;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryFactory;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

class BuildProblemsProcessor {
  private final SBuild mySBuild;
  private final SProject mySProject;
  private final FailedBuildInfo myFailedBuildInfo;
  private final TestApplicabilityChecker myTestApplicabilityChecker;
  private final BuildApplicabilityChecker myBuildApplicabilityChecker;
  private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  private final Integer myThreshold;
  private final List<HeuristicFactory> myHeuristicFactories;

  BuildProblemsProcessor(final SBuild sBuild,
                         final TestApplicabilityChecker testApplicabilityChecker,
                         final BuildApplicabilityChecker buildApplicabilityChecker,
                         final TestNameResponsibilityFacade testNameResponsibilityFacade,
                         final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                         List<HeuristicFactory> heuristicFactories) {
    assert sBuild.getBuildType() != null;

    mySBuild = sBuild;
    mySProject = sBuild.getBuildType().getProject();
    myThreshold = CustomParameters.getMaxTestsPerBuildThreshold(mySBuild);
    myFailedBuildInfo = new FailedBuildInfo();
    myTestApplicabilityChecker = testApplicabilityChecker;
    myBuildApplicabilityChecker = buildApplicabilityChecker;
    myHeuristicFactories = heuristicFactories;
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
  }

  boolean process() {
    boolean shouldDelete = mySBuild.isFinished();

    List<STestRun> testRuns = getApplicableTests();
    myFailedBuildInfo.processed += testRuns.size();

    List<BuildProblem> buildProblems = getApplicableBuildProblems();
    myFailedBuildInfo.processed += buildProblems.size();

    ResponsibleUserFinder responsibleUserFinder =
      new ResponsibleUserFinder(myHeuristicFactories, mySBuild, testRuns, buildProblems);

    for (STestRun testRun : testRuns) {

      Pair<User, String> responsible = responsibleUserFinder.findResponsible(testRun);

      if (responsible != null) {
        assignTestInvestigation(testRun.getTest(), responsible);
      }
    }

    for (BuildProblem buildProblem : buildProblems) {
      Pair<User, String> responsible = responsibleUserFinder.findResponsible(buildProblem);

      if (responsible != null) {
        assignBuildProblemInvestigation(buildProblem, responsible);
      }
    }

    myFailedBuildInfo.addProcessedTestRuns(testRuns);

    return shouldDelete;
  }

  private List<BuildProblem> getApplicableBuildProblems() {
    if (myFailedBuildInfo.processed >= myThreshold || !(mySBuild instanceof BuildEx)) return Collections.emptyList();

    List<BuildProblem> buildProblems = ((BuildEx)mySBuild).getBuildProblems();
    for (BuildProblem buildProblem : buildProblems) {
      BuildProblemImpl buildProblemImpl = (BuildProblemImpl)buildProblem;
      buildProblemImpl.getBuildProblemData();
    }

    return buildProblems.stream()
                        .filter(buildProblem -> buildProblem instanceof BuildProblemImpl)
                        .filter(problem -> myBuildApplicabilityChecker
                          .isApplicable(mySProject, mySBuild, (BuildProblemImpl)problem))
                        .collect(Collectors.toList());
  }

  private List<STestRun> getApplicableTests() {
    if (myFailedBuildInfo.processed >= myThreshold) return Collections.emptyList();

    List<STestRun> failedTests = requestBrokenTestsWithStats(mySBuild);

    return failedTests.stream()
                      .filter(myFailedBuildInfo::checkNotProcessed)
                      .filter(testRun -> myTestApplicabilityChecker
                        .isApplicable(mySProject, mySBuild, testRun))
                      .limit(myThreshold - myFailedBuildInfo.processed)
                      .collect(Collectors.toList());
  }

  private List<STestRun> requestBrokenTestsWithStats(@NotNull final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);
    return stats.getFailedTests();
  }



  private void assignTestInvestigation(STest test, Pair<User, String> responsibleUser) {
    final TestName testName = test.getName();
    myTestNameResponsibilityFacade.setTestNameResponsibility(
      testName, mySProject.getProjectId(), ResponsibilityEntryFactory.createEntry(
        testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, responsibleUser.getFirst(), null,
        Dates.now(), responsibleUser.getSecond(), mySProject, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
      )
    );
  }

  private void assignBuildProblemInvestigation(BuildProblem problem, Pair<User, String> responsibleUser) {
    myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
      problem, mySProject.getProjectId(),
      new BuildProblemResponsibilityEntryImpl(
        ResponsibilityEntry.State.TAKEN, responsibleUser.getFirst(), null, Dates.now(), responsibleUser.getSecond(),
        ResponsibilityEntry.RemoveMethod.WHEN_FIXED, mySProject, problem.getId()
      )
    );
  }
}
