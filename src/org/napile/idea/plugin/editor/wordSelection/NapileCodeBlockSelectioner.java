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
package org.napile.idea.plugin.editor.wordSelection;

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileWhenExpression;
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

/**
 * Originally from IDEA platform: CodeBlockOrInitializerSelectioner
 */
public class NapileCodeBlockSelectioner extends ExtendWordSelectionHandlerBase
{
	@Override
	public boolean canSelect(PsiElement e)
	{
		return e instanceof NapileBlockExpression || e instanceof NapileWhenExpression;
	}

	@Override
	public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor)
	{
		List<TextRange> result = new ArrayList<TextRange>();

		ASTNode[] children = e.getNode().getChildren(null);

		int start = findOpeningBrace(children);
		int end = findClosingBrace(children, start);

		result.add(e.getTextRange());
		result.addAll(expandToWholeLine(editorText, new TextRange(start, end)));

		return result;
	}

	public static int findOpeningBrace(ASTNode[] children)
	{
		int start = children[children.length - 1].getTextRange().getStartOffset();
		for(int i = 0; i < children.length; i++)
		{
			PsiElement child = children[i].getPsi();

			if(child instanceof LeafPsiElement)
			{
				if(((LeafPsiElement) child).getElementType() == NapileTokens.LBRACE)
				{
					int j = i + 1;

					while(children[j] instanceof PsiWhiteSpace)
					{
						j++;
					}

					start = children[j].getTextRange().getStartOffset();
				}
			}
		}
		return start;
	}

	public static int findClosingBrace(ASTNode[] children, int startOffset)
	{
		int end = children[children.length - 1].getTextRange().getEndOffset();
		for(int i = 0; i < children.length; i++)
		{
			PsiElement child = children[i].getPsi();

			if(child instanceof LeafPsiElement)
			{
				if(((LeafPsiElement) child).getElementType() == NapileTokens.RBRACE)
				{
					int j = i - 1;

					while(children[j] instanceof PsiWhiteSpace && children[j].getTextRange().getStartOffset() > startOffset)
					{
						j--;
					}

					end = children[j].getTextRange().getEndOffset();
				}
			}
		}
		return end;
	}
}
