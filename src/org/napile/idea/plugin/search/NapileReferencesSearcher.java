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

package org.napile.idea.plugin.search;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassBody;
import org.napile.compiler.lang.psi.NapileMethod;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;

/**
 * @author yole
 */
public class NapileReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
{
	@Override
	public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<PsiReference> consumer)
	{
		PsiElement element = queryParameters.getElementToSearch();
		if(element instanceof NapileClass)
		{
			String className = ((NapileClass) element).getName();
			if(className != null)
			{
				queryParameters.getOptimizer().searchWord(className, queryParameters.getEffectiveSearchScope(), true, element);
			}
		}
		else if(element instanceof NapileMethod)
		{
			final NapileMethod function = (NapileMethod) element;
			final String name = function.getName();
			if(function.getParent() instanceof NapileClassBody && name != null)
			{
				queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, element);
			}
		}
	}
}
