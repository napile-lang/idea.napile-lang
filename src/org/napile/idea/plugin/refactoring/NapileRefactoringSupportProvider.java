/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.idea.plugin.refactoring;

import org.napile.idea.plugin.refactoring.introduceVariable.NapileIntroduceVariableHandler;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.refactoring.RefactoringActionHandler;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class NapileRefactoringSupportProvider extends RefactoringSupportProvider
{
	@Override
	public RefactoringActionHandler getIntroduceVariableHandler()
	{
		return new NapileIntroduceVariableHandler();
	}
}
