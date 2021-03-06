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

package org.napile.idea.plugin.quickfix;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author svtk
 */
public class QuickFixUtil
{
	private QuickFixUtil()
	{
	}

	public static boolean removePossiblyWhiteSpace(ASTDelegatePsiElement element, PsiElement possiblyWhiteSpace)
	{
		if(possiblyWhiteSpace instanceof PsiWhiteSpace)
		{
			element.deleteChildInternal(possiblyWhiteSpace.getNode());
			return true;
		}
		return false;
	}

	@Nullable
	public static <T extends PsiElement> T getParentElementOfType(Diagnostic diagnostic, Class<T> aClass)
	{
		return PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), aClass, false);
	}

	@Nullable
	public static NapileType getDeclarationReturnType(NapileNamedDeclaration declaration)
	{
		PsiFile file = declaration.getContainingFile();
		if(!(file instanceof NapileFile))
			return null;
		BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze((NapileFile) file).getBindingTrace();
		DeclarationDescriptor descriptor = bindingContext.get(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, declaration);
		if(!(descriptor instanceof CallableDescriptor))
			return null;
		NapileType type = ((CallableDescriptor) descriptor).getReturnType();
		if(type instanceof DeferredType)
		{
			type = ((DeferredType) type).getActualType();
		}
		return type;
	}
}
