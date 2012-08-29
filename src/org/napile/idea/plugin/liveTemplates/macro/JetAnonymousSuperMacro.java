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

package org.napile.idea.plugin.liveTemplates.macro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.idea.plugin.JetBundle;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Evgeny Gerashchenko
 * @since 2/2/12
 */
public class JetAnonymousSuperMacro extends Macro
{
	@Override
	public String getName()
	{
		return "anonymousSuper";
	}

	@Override
	public String getPresentableName()
	{
		return JetBundle.message("macro.fun.anonymousSuper");
	}

	@Override
	public Result calculateResult(@NotNull Expression[] params, final ExpressionContext context)
	{
		AnonymousTemplateEditingListener.registerListener(context.getEditor(), context.getProject());

		PsiNamedElement[] vars = getSupertypes(params, context);
		if(vars == null || vars.length == 0)
			return null;
		return new JetPsiElementResult(vars[0]);
	}

	@Override
	public LookupElement[] calculateLookupItems(@NotNull Expression[] params, ExpressionContext context)
	{
		final PsiNamedElement[] vars = getSupertypes(params, context);
		if(vars == null || vars.length < 2)
			return null;
		final Set<LookupElement> set = new LinkedHashSet<LookupElement>();
		for(PsiNamedElement var : vars)
		{
			set.add(LookupElementBuilder.create(var));
		}
		return set.toArray(new LookupElement[set.size()]);
	}

	@Nullable
	private static PsiNamedElement[] getSupertypes(Expression[] params, ExpressionContext context)
	{
		if(params.length != 0)
			return null;

		Project project = context.getProject();
		PsiDocumentManager.getInstance(project).commitAllDocuments();

		PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
		if(!(psiFile instanceof NapileFile))
			return null;

		BindingContext bc = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) psiFile).getBindingContext();
		NapileExpression expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(context.getStartOffset()), NapileExpression.class);
		JetScope scope = bc.get(BindingContext.RESOLUTION_SCOPE, expression);
		if(scope == null)
		{
			return null;
		}

		List<PsiNamedElement> result = new ArrayList<PsiNamedElement>();

		for(DeclarationDescriptor descriptor : scope.getAllDescriptors())
		{
			if(!(descriptor instanceof ClassDescriptor))
				continue;
			ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
			if(!classDescriptor.getModality().isOverridable())
				continue;
			ClassKind kind = classDescriptor.getKind();
			if(kind == ClassKind.CLASS)
			{
				PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bc, descriptor);
				if(declaration != null)
				{
					result.add((PsiNamedElement) declaration);
				}
			}
		}

		return result.toArray(new PsiNamedElement[result.size()]);
	}
}