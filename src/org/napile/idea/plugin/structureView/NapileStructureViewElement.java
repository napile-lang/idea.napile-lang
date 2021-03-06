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

import com.intellij.ide.IconDescriptorUpdaters;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;

import javax.swing.*;

/**
 * @author yole
 */
public class NapileStructureViewElement implements StructureViewTreeElement
{
	private final NavigatablePsiElement myElement;

	// For file context will be updated after each construction
	// For other tree sub-elements it's immutable.
	private BindingTrace context;

	private String elementText;

	public NapileStructureViewElement(NavigatablePsiElement element, BindingTrace context)
	{
		myElement = element;
		this.context = context;
	}

	public NapileStructureViewElement(NapileFile fileElement)
	{
		myElement = fileElement;
	}

	@Override
	public Object getValue()
	{
		return myElement;
	}

	@Override
	public void navigate(boolean requestFocus)
	{
		myElement.navigate(requestFocus);
	}

	@Override
	public boolean canNavigate()
	{
		return myElement.canNavigate();
	}

	@Override
	public boolean canNavigateToSource()
	{
		return myElement.canNavigateToSource();
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return new ItemPresentation()
		{
			@Override
			public String getPresentableText()
			{
				if(elementText == null)
				{
					elementText = getElementText();
				}

				return elementText;
			}

			@Override
			public String getLocationString()
			{
				return null;
			}

			@Override
			public Icon getIcon(boolean open)
			{
				if(myElement.isValid())
				{
					return IconDescriptorUpdaters.getIcon(myElement, 0);
				}

				return null;
			}
		};
	}

	@Override
	public TreeElement[] getChildren()
	{
		if(myElement instanceof NapileFile)
		{
			final NapileFile jetFile = (NapileFile) myElement;

			context = ModuleAnalyzerUtil.lastAnalyze(jetFile).getBindingTrace();

			return wrapDeclarations(jetFile.getDeclarations());
		}
		else if(myElement instanceof NapileClass)
		{
			NapileClass napileClass = (NapileClass) myElement;

			return wrapDeclarations(napileClass.getDeclarations());
		}
		else if(myElement instanceof NapileClassLike)
		{
			return wrapDeclarations(((NapileClassLike) myElement).getDeclarations());
		}

		return new TreeElement[0];
	}

	private String getElementText()
	{
		String text = "";

		// Try to find text in correspondent descriptor
		if(myElement instanceof NapileDeclaration)
		{
			NapileDeclaration declaration = (NapileDeclaration) myElement;

			final DeclarationDescriptor descriptor = context.get(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, declaration);
			if(descriptor != null)
			{
				text = getDescriptorTreeText(descriptor);
			}
		}

		if(StringUtil.isEmpty(text))
		{
			text = myElement.getName();
		}

		return text;
	}

	private TreeElement[] wrapDeclarations(NapileDeclaration[] declarations)
	{
		TreeElement[] result = new TreeElement[declarations.length];
		for(int i = 0; i < declarations.length; i++)
		{
			result[i] = new NapileStructureViewElement(declarations[i], context);
		}
		return result;
	}

	public static String getDescriptorTreeText(@NotNull DeclarationDescriptor descriptor)
	{
		StringBuilder textBuilder;

		if(descriptor instanceof MethodDescriptor)
		{
			textBuilder = new StringBuilder();

			MethodDescriptor methodDescriptor = (MethodDescriptor) descriptor;

			textBuilder.append(methodDescriptor.getName());

			String parametersString = StringUtil.join(methodDescriptor.getValueParameters(), new Function<CallParameterDescriptor, String>()
			{
				@Override
				public String fun(CallParameterDescriptor valueParameterDescriptor)
				{
					return valueParameterDescriptor.getName() + ":" +
							DescriptorRenderer.TEXT.renderType(valueParameterDescriptor.getType());
				}
			}, ",");

			textBuilder.append("(").append(parametersString).append(")");

			NapileType returnType = methodDescriptor.getReturnType();
			textBuilder.append(":").append(DescriptorRenderer.TEXT.renderType(returnType));
		}
		else if(descriptor instanceof VariableDescriptor)
		{
			NapileType outType = ((VariableDescriptor) descriptor).getType();

			textBuilder = new StringBuilder(descriptor.getName().getName());
			textBuilder.append(":").append(DescriptorRenderer.TEXT.renderType(outType));
		}
		else if(descriptor instanceof ClassDescriptor)
		{
			textBuilder = new StringBuilder(descriptor.getName().getName());
			textBuilder.append(" (").append(DescriptorUtils.getFQName(descriptor.getContainingDeclaration())).append(")");
		}
		else
		{
			return DescriptorRenderer.TEXT.render(descriptor);
		}

		return textBuilder.toString();
	}
}
