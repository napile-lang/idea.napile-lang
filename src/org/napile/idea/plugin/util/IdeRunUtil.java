package org.napile.idea.plugin.util;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.psi.NapileClassLike;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.util.RunUtil;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;

/**
 * @author VISTALL
 * @date 14:23/08.10.12
 */
public class IdeRunUtil extends RunUtil
{
	public static boolean hasClassPoint(@NotNull NapileClassLike classLike)
	{
		AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(classLike.getContainingFile());
		MutableClassDescriptor descriptor = (MutableClassDescriptor) analyzeExhaust.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classLike);
		if(descriptor == null)
			return false;

		for(SimpleMethodDescriptor methodDescriptor : descriptor.getFunctions())
			if(isRunPoint(methodDescriptor))
				return true;
		return false;
	}
}