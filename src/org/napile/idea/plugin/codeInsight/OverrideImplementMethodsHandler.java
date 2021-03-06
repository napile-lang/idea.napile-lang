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

package org.napile.idea.plugin.codeInsight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.psi.NapileClassBody;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;

/**
 * @author yole
 */
public abstract class OverrideImplementMethodsHandler implements LanguageCodeInsightActionHandler
{
	public static List<DescriptorClassMember> membersFromDescriptors(Iterable<CallableMemberDescriptor> missingImplementations)
	{
		List<DescriptorClassMember> members = new ArrayList<DescriptorClassMember>();
		for(CallableMemberDescriptor memberDescriptor : missingImplementations)
		{
			members.add(new DescriptorClassMember(memberDescriptor));
		}
		return members;
	}

	@NotNull
	public Set<CallableMemberDescriptor> collectMethodsToGenerate(@NotNull NapileClassLike classOrObject)
	{
		BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze((NapileFile) classOrObject.getContainingFile()).getBindingTrace();
		final DeclarationDescriptor descriptor = bindingContext.get(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, classOrObject);
		if(descriptor instanceof MutableClassDescriptor)
		{
			return collectMethodsToGenerate((MutableClassDescriptor) descriptor);
		}
		return Collections.emptySet();
	}

	protected abstract Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor);

	public static void generateMethods(Editor editor, NapileClassLike classOrObject, List<DescriptorClassMember> selectedElements)
	{
		final NapileClassBody body = classOrObject.getBody();
		if(body == null)
		{
			return;
		}

		PsiElement afterAnchor = findInsertAfterAnchor(editor, body);

		if(afterAnchor == null)
		{
			return;
		}

		List<NapileElement> elementsToCompact = new ArrayList<NapileElement>();
		final NapileFile file = (NapileFile) classOrObject.getContainingFile();
		for(NapileElement element : generateOverridingMembers(selectedElements, file))
		{
			PsiElement added = body.addAfter(element, afterAnchor);
			afterAnchor = added;
			elementsToCompact.add((NapileElement) added);
		}
		ReferenceToClassesShortening.compactReferenceToClasses(elementsToCompact);
	}


	@Nullable
	private static PsiElement findInsertAfterAnchor(Editor editor, final NapileClassBody body)
	{
		PsiElement afterAnchor = body.getLBrace();
		if(afterAnchor == null)
		{
			return null;
		}

		int offset = editor.getCaretModel().getOffset();
		PsiElement offsetCursorElement = PsiTreeUtil.findFirstParent(body.getContainingFile().findElementAt(offset), new Condition<PsiElement>()
		{
			@Override
			public boolean value(PsiElement element)
			{
				return element.getParent() == body;
			}
		});

		if(offsetCursorElement != null && offsetCursorElement != body.getRBrace())
		{
			afterAnchor = offsetCursorElement;
		}

		return afterAnchor;
	}

	private static List<NapileElement> generateOverridingMembers(List<DescriptorClassMember> selectedElements, NapileFile file)
	{
		List<NapileElement> overridingMembers = new ArrayList<NapileElement>();
		for(DescriptorClassMember selectedElement : selectedElements)
		{
			final DeclarationDescriptor descriptor = selectedElement.getDescriptor();
			if(descriptor instanceof SimpleMethodDescriptor)
			{
				overridingMembers.add(overrideMethod(file.getProject(), (SimpleMethodDescriptor) descriptor));
			}
			else if(descriptor instanceof VariableDescriptorImpl)
			{
				overridingMembers.add(overrideProperty(file.getProject(), (VariableDescriptorImpl) descriptor));
			}
		}
		return overridingMembers;
	}

	private static NapileElement overrideProperty(Project project, VariableDescriptorImpl descriptor)
	{
		StringBuilder bodyBuilder = new StringBuilder();
		bodyBuilder.append(displayableVisibility(descriptor)).append("override ");

		bodyBuilder.append("var ");

		bodyBuilder.append(descriptor.getName()).append(" : ").append(DescriptorRenderer.COMPACT_WITH_MODIFIERS.renderTypeWithShortNames(descriptor.getType()));
		String initializer = defaultInitializer(descriptor.getType());
		if(initializer != null)
		{
			bodyBuilder.append(" = ").append(initializer);
		}
		else
		{
			bodyBuilder.append(" = ?");
		}
		return NapilePsiFactory.createProperty(project, bodyBuilder.toString());
	}

	private static NapileElement overrideMethod(Project project, SimpleMethodDescriptor descriptor)
	{
		StringBuilder bodyBuilder = new StringBuilder();
		bodyBuilder.append(displayableVisibility(descriptor));
		bodyBuilder.append("override ");
		bodyBuilder.append(DescriptorRenderer.COMPACT.render(descriptor));

		if(descriptor.getModality() == Modality.ABSTRACT)
		{
			bodyBuilder.append("{").append("throw UnsupportedOperationException()").append("}");
		}
		else
		{
			bodyBuilder.append("{");
			if(!TypeUtils.isEqualFqName(descriptor.getReturnType(), NapileLangPackage.NULL))
				bodyBuilder.append("return ");
			bodyBuilder.append("super<").append(descriptor.getContainingDeclaration().getName());
			bodyBuilder.append(">.").append(descriptor.getName()).append("(");
			bodyBuilder.append(StringUtil.join(descriptor.getValueParameters(), new Function<CallParameterDescriptor, String>()
			{
				@Override
				public String fun(CallParameterDescriptor callParameterDescriptor)
				{
					return callParameterDescriptor.getName().getName();
				}
			}, ", "));
			bodyBuilder.append(")").append("}");
		}

		return NapilePsiFactory.createMethod(project, bodyBuilder.toString());
	}

	//TODO [VISTALL] get from @DefaultValue =
	private static String defaultInitializer(NapileType returnType)
	{
		if(returnType.isNullable() || TypeUtils.isEqualFqName(returnType, NapileLangPackage.NULL))
		{
			return "null";
		}
		else if(TypeUtils.isEqualFqName(returnType, NapileLangPackage.BYTE) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.SHORT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.INT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.LONG) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.FLOAT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.DOUBLE) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.CHAR))
		{
			return "0";
		}
		else if(TypeUtils.isEqualFqName(returnType, NapileLangPackage.BOOL))
		{
			return "false";
		}

		return null;
	}

	private static String displayableVisibility(DeclarationDescriptorWithVisibility descriptor)
	{
		Visibility visibility = descriptor.getVisibility();
		return visibility != Visibility.PUBLIC ? visibility.toString() + " " : "";
	}

	private MemberChooser<DescriptorClassMember> showOverrideImplementChooser(Project project, DescriptorClassMember[] members)
	{
		final MemberChooser<DescriptorClassMember> chooser = new MemberChooser<DescriptorClassMember>(members, true, true, project);
		chooser.setTitle(getChooserTitle());
		chooser.show();
		if(chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE)
			return null;
		return chooser;
	}

	protected abstract String getChooserTitle();

	@Override
	public boolean isValidFor(Editor editor, PsiFile file)
	{
		if(!(file instanceof NapileFile))
		{
			return false;
		}
		final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
		final NapileClassLike classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, NapileClassLike.class);
		return classOrObject != null;
	}

	protected abstract String getNoMethodsFoundHint();

	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file, boolean implementAll)
	{
		final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
		final NapileClassLike classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, NapileClassLike.class);

		assert classOrObject != null : "ClassObject should be checked in isValidFor method";

		Set<CallableMemberDescriptor> missingImplementations = collectMethodsToGenerate(classOrObject);
		if(missingImplementations.isEmpty() && !implementAll)
		{
			HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint());
			return;
		}
		List<DescriptorClassMember> members = membersFromDescriptors(missingImplementations);

		final List<DescriptorClassMember> selectedElements;
		if(implementAll)
		{
			selectedElements = members;
		}
		else
		{
			final MemberChooser<DescriptorClassMember> chooser = showOverrideImplementChooser(project, members.toArray(new DescriptorClassMember[members.size()]));

			if(chooser == null)
			{
				return;
			}

			selectedElements = chooser.getSelectedElements();
			if(selectedElements == null || selectedElements.isEmpty())
				return;
		}

		ApplicationManager.getApplication().runWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				generateMethods(editor, classOrObject, selectedElements);
			}
		});
	}

	@Override
	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file)
	{
		invoke(project, editor, file, false);
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}
}
