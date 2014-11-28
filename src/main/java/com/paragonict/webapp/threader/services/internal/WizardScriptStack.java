package com.paragonict.webapp.threader.services.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.javascript.JavaScriptStack;
import org.apache.tapestry5.services.javascript.StylesheetLink;

public class WizardScriptStack implements JavaScriptStack {


	@Inject
	private AssetSource as;
	
	
	@Override
	public List<String> getStacks() {
		final List<String> requiredStacks = new ArrayList<String>(1);
		requiredStacks.add("bootstrap");
		return requiredStacks;
	}

	@Override
	public List<Asset> getJavaScriptLibraries() {
		final List<Asset> assets = new ArrayList<Asset>(1);
		assets.add(as.getUnlocalizedAsset("com/paragonict/webapp/threader/stack/jquery.bootstrap.wizard.js"));
		return assets;
	}

	@Override
	public List<StylesheetLink> getStylesheets() {
		return Collections.emptyList();
	}

	@Override
	public String getInitialization() {
		
		return null;
	}

}
