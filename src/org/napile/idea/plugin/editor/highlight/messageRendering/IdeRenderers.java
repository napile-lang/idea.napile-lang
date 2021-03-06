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

package org.napile.idea.plugin.editor.highlight.messageRendering;

import static org.napile.compiler.lang.diagnostics.rendering.Renderers.renderConflictingSubstitutionsInferenceError;
import static org.napile.compiler.lang.diagnostics.rendering.Renderers.renderNoInformationForParameterError;
import static org.napile.compiler.lang.diagnostics.rendering.Renderers.renderTypeConstructorMismatchError;
import static org.napile.compiler.lang.diagnostics.rendering.Renderers.renderUpperBoundViolatedInferenceError;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.DiagnosticWithParameters1;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.diagnostics.rendering.Renderer;
import org.napile.compiler.lang.psi.NapileValueArgument;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedCallImpl;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.calls.inference.InferenceErrorData;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.render.DescriptorRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author svtk
 */
public class IdeRenderers
{
	private static final String RED_TEMPLATE = "<font color=red><b>%s</b></font>";
	private static final String STRONG_TEMPLATE = "<b>%s</b>";

	public static String strong(Object o)
	{
		return String.format(STRONG_TEMPLATE, o);
	}

	public static String error(Object o)
	{
		return String.format(RED_TEMPLATE, o);
	}

	public static String strong(Object o, boolean error)
	{
		return String.format(error ? RED_TEMPLATE : STRONG_TEMPLATE, o);
	}

	public static final Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> HTML_AMBIGUOUS_CALLS = new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>()
	{
		@NotNull
		@Override
		public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> calls)
		{
			StringBuilder stringBuilder = new StringBuilder("");
			for(ResolvedCall<? extends CallableDescriptor> call : calls)
			{
				stringBuilder.append("<li>");
				stringBuilder.append(DescriptorRenderer.HTML.render(call.getResultingDescriptor())).append("\n");
				stringBuilder.append("</li>");
			}
			return stringBuilder.toString();
		}
	};

	public static final Renderer<NapileType> HTML_RENDER_TYPE = new Renderer<NapileType>()
	{
		@NotNull
		@Override
		public String render(@NotNull NapileType type)
		{
			return DescriptorRenderer.HTML.renderType(type);
		}
	};


	public static class NoneApplicableCallsRenderer implements Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>
	{
		@Nullable
		private static CallParameterDescriptor findParameterByArgumentExpression(ResolvedCall<? extends CallableDescriptor> call, NapileValueArgument argument)
		{
			for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet())
			{
				for(ValueArgument va : entry.getValue().getArguments())
				{
					if(va == argument)
					{
						return entry.getKey();
					}
				}
			}
			return null;
		}

		private static Set<CallParameterDescriptor> getParametersToHighlight(ResolvedCall<? extends CallableDescriptor> call)
		{
			Set<CallParameterDescriptor> parameters = new HashSet<CallParameterDescriptor>();
			if(call instanceof ResolvedCallImpl)
			{
				Collection<Diagnostic> diagnostics = ((ResolvedCallImpl) call).getTrace().getDiagnostics();
				for(Diagnostic diagnostic : diagnostics)
				{
					if(diagnostic.getFactory() == Errors.TOO_MANY_ARGUMENTS)
					{
						parameters.add(null);
					}
					else if(diagnostic.getFactory() == Errors.NO_VALUE_FOR_PARAMETER)
					{
						CallParameterDescriptor parameter = ((DiagnosticWithParameters1<PsiElement, CallParameterDescriptor>) diagnostic).getA();
						parameters.add(parameter);
					}
					else
					{
						NapileValueArgument argument = PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), NapileValueArgument.class, false);
						if(argument != null)
						{
							CallParameterDescriptor parameter = findParameterByArgumentExpression(call, argument);
							if(parameter != null)
							{
								parameters.add(parameter);
							}
						}
					}
				}
			}
			return parameters;
		}

		@NotNull
		@Override
		public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> calls)
		{
			StringBuilder stringBuilder = new StringBuilder("");
			for(ResolvedCall<? extends CallableDescriptor> call : calls)
			{
				stringBuilder.append("<li>");
				CallableDescriptor funDescriptor = call.getResultingDescriptor();
				Set<CallParameterDescriptor> parametersToHighlight = getParametersToHighlight(call);

				DescriptorRenderer htmlRenderer = DescriptorRenderer.HTML;
				stringBuilder.append(funDescriptor.getName()).append("(");
				boolean first = true;
				for(CallParameterDescriptor parameter : funDescriptor.getValueParameters())
				{
					if(!first)
					{
						stringBuilder.append(", ");
					}
					NapileType type = parameter.getType();

					String paramString = htmlRenderer.renderType(type);
					if(parameter.hasDefaultValue())
					{
						paramString += " = ...";
					}
					if(parametersToHighlight.contains(parameter))
					{
						paramString = String.format(RED_TEMPLATE, paramString);
					}
					stringBuilder.append(paramString);

					first = false;
				}
				stringBuilder.append(parametersToHighlight.contains(null) ? String.format(RED_TEMPLATE, ")") : ")");
				stringBuilder.append(" ").append(htmlRenderer.renderMessage("defined in")).append(" ");
				DeclarationDescriptor containingDeclaration = funDescriptor.getContainingDeclaration();
				if(containingDeclaration != null)
				{
					FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
					stringBuilder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : fqName.getFqName());
				}
				stringBuilder.append("</li>");
			}
			return stringBuilder.toString();
		}
	}

	public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER = new Renderer<InferenceErrorData>()
	{
		@NotNull
		@Override
		public String render(@NotNull InferenceErrorData inferenceErrorData)
		{
			return renderConflictingSubstitutionsInferenceError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
		}
	};

	public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER = new Renderer<InferenceErrorData>()
	{
		@NotNull
		@Override
		public String render(@NotNull InferenceErrorData inferenceErrorData)
		{
			return renderTypeConstructorMismatchError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
		}
	};

	public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER = new Renderer<InferenceErrorData>()
	{
		@NotNull
		@Override
		public String render(@NotNull InferenceErrorData inferenceErrorData)
		{
			return renderNoInformationForParameterError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
		}
	};

	public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER = new Renderer<InferenceErrorData>()
	{
		@NotNull
		@Override
		public String render(@NotNull InferenceErrorData inferenceErrorData)
		{
			return renderUpperBoundViolatedInferenceError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
		}
	};
}
