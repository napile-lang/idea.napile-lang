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

import java.util.Arrays;
import java.util.List;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileTypeArgumentList;
import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.NapileValueArgumentList;
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;

/**
 * @author Evgeny Gerashchenko
 * @since 4/23/12
 */
public class NapileListSelectioner extends ExtendWordSelectionHandlerBase
{
	@Override
	public boolean canSelect(PsiElement e)
	{
		return e instanceof NapileCallParameterList || e instanceof NapileValueArgumentList ||
				e instanceof NapileTypeParameterList || e instanceof NapileTypeArgumentList;
	}

	@Override
	public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor)
	{
		ASTNode node = e.getNode();
		ASTNode startNode = node.findChildByType(TokenSet.create(NapileTokens.LPAR, NapileTokens.LT));
		ASTNode endNode = node.findChildByType(TokenSet.create(NapileTokens.RPAR, NapileTokens.GT));
		if(startNode != null && endNode != null)
		{
			return Arrays.asList(new TextRange(startNode.getStartOffset() + 1, endNode.getStartOffset()));
		}
		else
		{
			return Arrays.asList();
		}
	}
}
