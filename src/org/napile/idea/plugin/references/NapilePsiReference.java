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

package org.napile.idea.plugin.references;

import static org.napile.compiler.lang.resolve.BindingTraceKeys.AMBIGUOUS_REFERENCE_TARGET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTraceUtil;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.idea.plugin.editor.completion.lookup.DescriptionLookupBuilder;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;

public abstract class NapilePsiReference implements PsiPolyVariantReference
{

	private static final Logger LOG = Logger.getInstance(NapilePsiReference.class);

	@NotNull
	protected final NapileReferenceExpression myExpression;

	protected NapilePsiReference(@NotNull NapileReferenceExpression expression)
	{
		this.myExpression = expression;
	}

	@NotNull
	@Override
	public PsiElement getElement()
	{
		return myExpression;
	}

	@NotNull
	@Override
	public ResolveResult[] multiResolve(boolean incompleteCode)
	{
		return doMultiResolve();
	}

	@Override
	public PsiElement resolve()
	{
		return doResolve();
	}

	@NotNull
	@Override
	public String getCanonicalText()
	{
		return "<TBD>";
	}

	@Override
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@NotNull
	@Override
	public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(PsiElement element)
	{
		PsiElement target = resolve();
		return target == element || target != null && target.getNavigationElement() == element;
	}

	@NotNull
	@Override
	public Object[] getVariants()
	{
		NapileFile file = (NapileFile) getElement().getContainingFile();
		BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze(file).getBindingTrace();

		NapileScope scope = bindingContext.get(BindingTraceKeys.RESOLUTION_SCOPE, myExpression);
		if(scope == null)
			return ArrayUtil.EMPTY_OBJECT_ARRAY;
		else
		{
			List<LookupElementBuilder> builders = new ArrayList<LookupElementBuilder>();
			for(DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors())
			{
				if(declarationDescriptor instanceof PackageDescriptor)
					continue;

				builders.add(DescriptionLookupBuilder.buildElement(declarationDescriptor));
			}

			return builders.toArray();
		}
	}

	@Override
	public boolean isSoft()
	{
		return false;
	}

	@Nullable
	protected PsiElement doResolve()
	{
		NapileFile file = (NapileFile) getElement().getContainingFile();
		BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze(file).getBindingTrace();
		List<PsiElement> psiElement = BindingTraceUtil.resolveToDeclarationPsiElements(bindingContext, myExpression);
		if(psiElement.size() == 1)
		{
			return psiElement.iterator().next();
		}
		if(psiElement.size() > 1)
		{
			return null;
		}

		Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, myExpression);
		if(declarationDescriptors != null)
			return null;

		return file;
	}

	protected ResolveResult[] doMultiResolve()
	{
		NapileFile file = (NapileFile) getElement().getContainingFile();
		BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze(file).getBindingTrace();
		Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, myExpression);
		if(declarationDescriptors == null)
			return ResolveResult.EMPTY_ARRAY;

		ArrayList<ResolveResult> results = new ArrayList<ResolveResult>(declarationDescriptors.size());

		for(DeclarationDescriptor descriptor : declarationDescriptors)
		{
			List<PsiElement> elements = BindingTraceUtil.descriptorToDeclarations(bindingContext, descriptor);
			if(elements.isEmpty())
			{
				results.add(new PsiElementResolveResult(file, true));
			}
			else
			{
				for(PsiElement element : elements)
				{
					results.add(new PsiElementResolveResult(element, true));
				}
			}
		}

		return results.toArray(new ResolveResult[results.size()]);
	}
}
