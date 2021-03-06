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

package org.napile.idea.plugin.editor.presentation;

import com.intellij.ide.IconDescriptorUpdaters;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.QualifiedNamesUtil;
import org.napile.idea.plugin.util.IdePsiUtil;

import javax.swing.*;

/**
 * @author Nikolay Krasko
 */
public class NapileMethodOrMacroPresenter implements ItemPresentationProvider<NapileNamedMethodOrMacro>
{
	@Override
	public ItemPresentation getPresentation(final NapileNamedMethodOrMacro function)
	{
		return new ColoredItemPresentation()
		{
			@Override
			public TextAttributesKey getTextAttributesKey()
			{
				if(IdePsiUtil.isDeprecated(function))
					return CodeInsightColors.DEPRECATED_ATTRIBUTES;
				return null;
			}

			@Override
			public String getPresentableText()
			{
				StringBuilder presentation = new StringBuilder(function.getName());
				presentation.append("(");
				presentation.append(StringUtil.join(function.getCallParameters(), new Function<NapileElement, String>()
				{
					@Override
					public String fun(NapileElement element)
					{
						if(element instanceof NapileCallParameterAsVariable)
						{
							NapileTypeReference reference = ((NapileCallParameterAsVariable) element).getTypeReference();
							if(reference != null)
							{
								String text = reference.getText();
								if(text != null)
									return text;
							}
						}
						else if(element instanceof NapileCallParameterAsReference)
						{
							NapileSimpleNameExpression ref = ((NapileCallParameterAsReference) element).getReferenceExpression();
							if(ref != null)
								return ref.getText();
						}

						return "?";
					}
				}, ", "));

				presentation.append(")");
				return presentation.toString();
			}

			@Override
			public String getLocationString()
			{
				FqName name = NapilePsiUtil.getFQName(function);
				if(name != null)
				{
					return String.format("(in %s)", QualifiedNamesUtil.withoutLastSegment(name));
				}

				return "";
			}

			@Override
			public Icon getIcon(boolean open)
			{
				return IconDescriptorUpdaters.getIcon(function, Iconable.ICON_FLAG_VISIBILITY);
			}
		};
	}
}
