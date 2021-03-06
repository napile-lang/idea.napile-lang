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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileBinaryExpressionWithTypeRHS;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.idea.plugin.NapileBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public abstract class ReplaceOperationInBinaryExpressionFix<T extends NapileExpression> extends NapileIntentionAction<T>
{
	private final String operation;

	public ReplaceOperationInBinaryExpressionFix(@NotNull T element, String operation)
	{
		super(element);
		this.operation = operation;
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return NapileBundle.message("replace.operation.in.binary.expression");
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		if(element instanceof NapileBinaryExpressionWithTypeRHS)
		{
			NapileExpression left = ((NapileBinaryExpressionWithTypeRHS) element).getLeft();
			NapileTypeReference right = ((NapileBinaryExpressionWithTypeRHS) element).getRight();
			if(right != null)
			{
				NapileExpression expression = NapilePsiFactory.createExpression(project, left.getText() + operation + right.getText());
				element.replace(expression);
			}
		}
	}

	public static NapileIntentionActionFactory createChangeCastToStaticAssertFactory()
	{
		return new NapileIntentionActionFactory()
		{
			@Override
			public NapileIntentionAction<NapileBinaryExpressionWithTypeRHS> createAction(Diagnostic diagnostic)
			{
				NapileBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, NapileBinaryExpressionWithTypeRHS.class);
				if(expression == null)
					return null;
				return new ReplaceOperationInBinaryExpressionFix<NapileBinaryExpressionWithTypeRHS>(expression, " : ")
				{
					@NotNull
					@Override
					public String getText()
					{
						return NapileBundle.message("replace.cast.with.static.assert");
					}
				};
			}
		};
	}
}
