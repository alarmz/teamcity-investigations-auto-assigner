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

import java.util.List;
import jetbrains.buildServer.iaa.heuristics.HeuristicFactory;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SBuild;

class BuildProblemsProcessorFactory {
  private final TestApplicabilityChecker myTestApplicabilityChecker;
  private final BuildApplicabilityChecker myBuildApplicabilityChecker;
  private final List<HeuristicFactory> myHeuristicFactories;
  private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;

  BuildProblemsProcessorFactory(final TestApplicabilityChecker testApplicabilityChecker,
                                final BuildApplicabilityChecker buildApplicabilityChecker,
                                final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                                List<HeuristicFactory> heuristicFactories) {
    myTestApplicabilityChecker = testApplicabilityChecker;
    myBuildApplicabilityChecker = buildApplicabilityChecker;
    myHeuristicFactories = heuristicFactories;
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
  }

  BuildProblemsProcessor getProcessor(SBuild sBuild) {
    return new BuildProblemsProcessor(sBuild, myTestApplicabilityChecker, myBuildApplicabilityChecker,
                                      myTestNameResponsibilityFacade, myBuildProblemResponsibilityFacade,
                                      myHeuristicFactories);
  }
}
