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

package org.napile.idea.plugin.highlighter;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

/**
 * @author Evgeny Gerashchenko
 * @since 3/29/12
 */
class TypeKindHighlightingVisitor extends AfterAnalysisHighlightingVisitor
{
	TypeKindHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext)
	{
		super(holder, bindingContext);
	}

	@Override
	public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
	{
		PsiReference ref = expression.getReference();
		if(ref == null)
			return;
		if(JetPsiChecker.isNamesHighlightingEnabled())
		{
			if(NapileTokens.KEYWORDS.contains(expression.getReferencedNameElementType()))
				return;
			DeclarationDescriptor referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
			if(referenceTarget instanceof ConstructorDescriptor)
			{
				referenceTarget = referenceTarget.getContainingDeclaration();
			}

			if(referenceTarget instanceof ClassDescriptor)
			{
				highlightClassByKind((ClassDescriptor) referenceTarget, expression);
			}
			else if(referenceTarget instanceof TypeParameterDescriptor)
			{
				JetPsiChecker.highlightName(holder, expression, NapileHighlightingColors.TYPE_PARAMETER, referenceTarget);
			}
		}
	}

	@Override
	public void visitTypeParameter(NapileTypeParameter parameter)
	{
		PsiElement identifier = parameter.getNameIdentifier();
		if(identifier != null)
		{
			JetPsiChecker.highlightName(holder, identifier, NapileHighlightingColors.TYPE_PARAMETER, null);
		}
		super.visitTypeParameter(parameter);
	}

	@Override
	public void visitClass(NapileClass klass)
	{
		PsiElement identifier = klass.getNameIdentifier();
		ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, klass);
		if(identifier != null && classDescriptor != null)
		{
			highlightClassByKind(classDescriptor, identifier);
		}
		super.visitClass(klass);
	}

	private void highlightClassByKind(@NotNull ClassDescriptor classDescriptor, @NotNull PsiElement whatToHighlight)
	{
		TextAttributesKey textAttributes;
		if(AnnotationUtils.hasAnnotation(classDescriptor, NapileAnnotationPackage.ANNOTATION))
			textAttributes = NapileHighlightingColors.ANNOTATION;
		else
			textAttributes = classDescriptor.getModality() == Modality.ABSTRACT ? NapileHighlightingColors.ABSTRACT_CLASS : NapileHighlightingColors.CLASS;

		JetPsiChecker.highlightName(holder, whatToHighlight, textAttributes, classDescriptor);
	}
}
