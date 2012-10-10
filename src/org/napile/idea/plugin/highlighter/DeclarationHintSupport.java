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

import java.awt.Point;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import org.napile.compiler.NapileLanguage;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.psi.NapileFile;
import org.napile.compiler.resolve.DescriptorRenderer;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.LightweightHint;

/**
 * @author Evgeny Gerashchenko
 * @since 5/16/12
 */
public class DeclarationHintSupport extends AbstractProjectComponent
{
	private final MyListener listener = new MyListener();

	public DeclarationHintSupport(Project project)
	{
		super(project);
	}

	@Override
	public void initComponent()
	{
		EditorFactory.getInstance().getEventMulticaster().addEditorMouseMotionListener(listener, myProject);
	}

	private class MyListener extends EditorMouseMotionAdapter
	{
		@Override
		public void mouseMoved(EditorMouseEvent e)
		{
			if(e.isConsumed() || e.getArea() != EditorMouseEventArea.EDITING_AREA)
				return;

			Editor editor = e.getEditor();
			PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
			if(psiFile == null || psiFile.getLanguage() != NapileLanguage.INSTANCE)
				return;

			if(DumbService.getInstance(psiFile.getProject()).isDumb())
			{
				return;
			}

			NapileFile jetFile = (NapileFile) psiFile;

			int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(e.getMouseEvent().getPoint()));
			PsiElement elementAtCursor = psiFile.findElementAt(offset);
			NapileNamedDeclaration declaration = PsiTreeUtil.getParentOfType(elementAtCursor, NapileNamedDeclaration.class);
			if(declaration != null && declaration.getNameIdentifier() == elementAtCursor)
			{
				BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();

				DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

				if(descriptor == null)
				{
					return;
				}

				JLayeredPane layeredPane = editor.getComponent().getRootPane().getLayeredPane();
				Point point = SwingUtilities.convertPoint(e.getMouseEvent().getComponent(), e.getMouseEvent().getPoint(), layeredPane);
				((HintManagerImpl) HintManager.getInstance()).showEditorHint(new LightweightHint(HintUtil.createInformationLabel(DescriptorRenderer.HTML.render(descriptor))), editor, point, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false);
			}
		}
	}
}
