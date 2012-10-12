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

package org.napile.idea.plugin.presentation;

import java.util.Collection;

import javax.swing.Icon;

import org.apache.commons.lang.StringUtils;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.util.QualifiedNamesUtil;
import org.napile.idea.plugin.JetIconProvider;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;

/**
 * @author Nikolay Krasko
 */
public class JetFunctionPresenter implements ItemPresentationProvider<NapileNamedMethod>
{
	@Override
	public ItemPresentation getPresentation(final NapileNamedMethod function)
	{
		return new ColoredItemPresentation()
		{
			@Override
			public TextAttributesKey getTextAttributesKey()
			{
				return null;
			}

			@Override
			public String getPresentableText()
			{
				StringBuilder presentation = new StringBuilder(function.getName());

				Collection<String> paramsStrings = Collections2.transform(function.getValueParameters(), new Function<NapileElement, String>()
				{
					@Override
					public String apply(NapileElement parameter)
					{
						if(parameter instanceof NapilePropertyParameter)
						{
							NapileTypeReference reference = ((NapilePropertyParameter) parameter).getTypeReference();
							if(reference != null)
							{
								String text = reference.getText();
								if(text != null)
								{
									return text;
								}
							}
						}

						return "?";
					}
				});

				presentation.append("(").append(StringUtils.join(paramsStrings, ",")).append(")");
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
				return JetIconProvider.INSTANCE.getIcon(function, Iconable.ICON_FLAG_VISIBILITY);
			}
		};
	}
}
