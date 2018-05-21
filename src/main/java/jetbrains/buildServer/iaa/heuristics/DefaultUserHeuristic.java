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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.util.Collection;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultUserHeuristic implements Heuristic {

  private static final Logger LOGGER = Logger.getInstance(DefaultUserHeuristic.class.getName());

  @NotNull private UserModelEx myUserModel;
  @NotNull private final SBuild mySBuild;

  DefaultUserHeuristic(@NotNull final UserModelEx userModel,
                       @NotNull final SBuild sBuild) {
    myUserModel = userModel;
    mySBuild = sBuild;
  }

  @Override
  @NotNull
  public String getName() {
    return "Default User Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to the default responsible user.";
  }

  @Override
  public Pair<User, String> findResponsibleUser(@NotNull final STestRun sTestRun) {
    return findResponsibleUser();
  }

  @Override
  public Pair<User, String> findResponsibleUser(@NotNull final BuildProblem buildProblem) {
    return findResponsibleUser();
  }

  private Pair<User, String> findResponsibleUser() {
    Collection<SBuildFeatureDescriptor> descriptors = mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) return null;

    final SBuildFeatureDescriptor sBuildFeature = (SBuildFeatureDescriptor)descriptors.toArray()[0];
    String defaultResponsible = String.valueOf(sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE));

    if (defaultResponsible == null || defaultResponsible.isEmpty()) return null;
    UserEx responsibleUser = myUserModel.findUserAccount(null, defaultResponsible);

    if (responsibleUser == null) {
      LOGGER.warn(String.format("There is specified default user %s, but there is no hin in user model. Failed build #%s",
                                defaultResponsible, mySBuild.getBuildId()));
      return null;
    }

    return Pair.create(responsibleUser,
                       Constants.REASON_PREFIX + " you're the default responsible user for the build: " +
                       mySBuild.getFullName() + " #" + mySBuild.getBuildNumber());
  }
}
