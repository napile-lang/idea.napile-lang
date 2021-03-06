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

package org.napile.idea.plugin.editor;

import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * @author ignatov
 */
@State(
		name = "NapileEditorOptions",
		storages = {
				@Storage(
						file = "$APP_CONFIG$/editor.xml")
		})
public class NapileEditorOptions implements PersistentStateComponent<NapileEditorOptions>
{
	private boolean donTShowConversionDialog = false;
	private boolean enableJavaToNapileConversion = true;

	public boolean isDonTShowConversionDialog()
	{
		return donTShowConversionDialog;
	}

	public void setDonTShowConversionDialog(boolean donTShowConversionDialog)
	{
		this.donTShowConversionDialog = donTShowConversionDialog;
	}

	public boolean isEnableJavaToNapileConversion()
	{
		return enableJavaToNapileConversion;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEnableJavaToNapileConversion(boolean enableJavaToNapileConversion)
	{
		this.enableJavaToNapileConversion = enableJavaToNapileConversion;
	}

	@Override
	public NapileEditorOptions getState()
	{
		return this;
	}

	@Override
	public void loadState(NapileEditorOptions state)
	{
		XmlSerializerUtil.copyBean(state, this);
	}

	public static NapileEditorOptions getInstance()
	{
		return ServiceManager.getService(NapileEditorOptions.class);
	}

	@Override
	@Nullable
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			return null;
		}
	}
}
