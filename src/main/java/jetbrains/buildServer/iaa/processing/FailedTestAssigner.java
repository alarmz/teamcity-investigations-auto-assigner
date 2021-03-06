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

package jetbrains.buildServer.iaa.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryFactory;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

class FailedTestAssigner {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private static final Logger LOGGER = Logger.getInstance(FailedTestAssigner.class.getName());

  FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
  }

  void assign(final HeuristicResult heuristicsResult, final SProject sProject, final List<STestRun> sTestRuns) {
    for (STestRun sTestRun: sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);

      if (responsibility != null) {
        final STest test = sTestRun.getTest();
        final TestName testName = test.getName();

        LOGGER.info(String.format("Automatically assigning investigation to %s in %s # %s because of %s",
                                  responsibility.getUser().getUsername(),
                                  sProject.describe(false),
                                  testName,
                                  responsibility.getDescription()));

        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testName, sProject.getProjectId(),
          ResponsibilityEntryFactory.createEntry(
            testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null,
            Dates.now(), responsibility.getDescription(), sProject, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
          )
        );
      }
    }
  }
}
