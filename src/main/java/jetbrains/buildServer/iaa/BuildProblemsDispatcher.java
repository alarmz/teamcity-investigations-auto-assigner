/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.stat.BuildTestsListener;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;

public class BuildProblemsDispatcher {
  @NotNull private final ScheduledExecutorService myDaemon;

  private ConcurrentHashMap<Long, BuildProblemsProcessor> failedBuildProcessors;


  public BuildProblemsDispatcher(@NotNull final BuildTestsEventDispatcher buildTestsEventDispatcher,
                                 @NotNull final BuildServerListenerEventDispatcher buildServerListenerEventDispatcher,
                                 @NotNull final BuildProblemsProcessorFactory myBuildProblemsProcessorFactory) {
    failedBuildProcessors = new ConcurrentHashMap<>();
    myDaemon = ExecutorsFactory.newFixedScheduledDaemonExecutor("Investigator-Auto-Assigner-", 1);
    myDaemon.scheduleWithFixedDelay(this::processBrokenBuildsOneThread, 2, 2, TimeUnit.MINUTES);


    buildTestsEventDispatcher.addListener(new BuildTestsListener() {
      public void testPassed(@NotNull SRunningBuild sRunningBuild, @NotNull List<Long> list) {
        //
      }

      public void testFailed(@NotNull SRunningBuild build, @NotNull List<Long> testNameIds) {
        if (shouldIgnore(build)) return;
        failedBuildProcessors.putIfAbsent(build.getBuildId(), myBuildProblemsProcessorFactory.getProcessor(build));
      }

      public void testIgnored(@NotNull SRunningBuild sRunningBuild, @NotNull List<Long> list) {
        //
      }
    });

    buildServerListenerEventDispatcher.addListener(new BuildServerAdapter() {

      @Override
      public void buildProblemsChanged(@NotNull SBuild sBuild,
                                       @NotNull List<BuildProblemData> before,
                                       @NotNull List<BuildProblemData> after) {
        if (shouldIgnore(sBuild) || !(sBuild instanceof BuildEx)) return;
        failedBuildProcessors.putIfAbsent(sBuild.getBuildId(), myBuildProblemsProcessorFactory.getProcessor(sBuild));
      }

      @Override
      public void serverShutdown() {
        myDaemon.shutdown();
      }
    });
  }

  private void processBrokenBuildsOneThread() {
    for (Long processorKey : failedBuildProcessors.keySet()) {
      Boolean shouldDelete = failedBuildProcessors.get(processorKey).process();
      if (shouldDelete) {
        failedBuildProcessors.remove(processorKey);
      }
    }
  }

  private static boolean shouldIgnore(@NotNull SBuild build) {
    return checkFeatureDisabled(build) || build.isPersonal() || build.getBuildType() == null;
  }

  private static boolean checkFeatureDisabled(@NotNull SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    return descriptors.isEmpty();
  }
}
