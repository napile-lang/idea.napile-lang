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

package org.napile.idea.plugin.structureView;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.psi.NapileFile;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.psi.PsiFile;

/**
 * @author yole
 */
public class JetStructureViewFactory implements PsiStructureViewFactory
{
	@Override
	public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile)
	{
		if(psiFile instanceof NapileFile)
		{
			final NapileFile file = (NapileFile) psiFile;

			return new TreeBasedStructureViewBuilder()
			{
				@NotNull
				@Override
				public StructureViewModel createStructureViewModel()
				{
					return new JetStructureViewModel(file);
				}
			};
		}

		return null;
	}
}
