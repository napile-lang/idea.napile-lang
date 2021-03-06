/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.run;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.idea.plugin.caches.NapileClassResolver;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author VISTALL
 * @since 11:40/08.01.13
 */
public class NapileProgramRunner extends DefaultProgramRunner
{
	@NotNull
	@Override
	public String getRunnerId()
	{
		return "Run";
	}

	@Override
	public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile)
	{
		if(!(profile instanceof NapileRunConfiguration))
		{
			return false;
		}

		NapileRunConfiguration runConfiguration = (NapileRunConfiguration) profile;

		final Module module = runConfiguration.getConfigurationModule().getModule();
		if(module == null)
		{
			return false;
		}

		NapileClass[] classesByName = NapileClassResolver.getInstance(runConfiguration.getProject()).getClassesByFqName(runConfiguration.mainClass, GlobalSearchScope.moduleWithDependenciesScope(module));
		if(classesByName.length != 1)
		{
			return false;
		}

		return runConfiguration.getConfigurationModule().getModule() != null;
	}
}
