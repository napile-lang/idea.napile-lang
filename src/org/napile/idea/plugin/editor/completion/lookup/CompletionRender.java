/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.editor.completion.lookup;

import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.render.DescriptorRenderer;

/**
 * @author VISTALL
 * @since 12:13/07.02.13
 */
public class CompletionRender extends DescriptorRenderer
{
	public static final CompletionRender INSTANCE = new CompletionRender();

	@Override
	public String renderType(NapileType type)
	{
		return renderType(type, true);
	}
}
