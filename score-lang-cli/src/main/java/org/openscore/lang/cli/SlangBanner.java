/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/
package org.openscore.lang.cli;

import org.apache.commons.io.IOUtils;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author lesant
 * @since 11/07/2014
 * @version $Id$
 */
@Component
@Order(Integer.MIN_VALUE)
public class SlangBanner extends SlangNamedProvider implements BannerProvider {

	private static final String BANNER = "slangBanner.txt";
	private static final String ASSISTANCE = "Welcome to Slang. For assistance type help.";

	@Override
	public String getBanner() {
		StringBuilder sb = new StringBuilder();
		try (InputStream in = ClassLoader.getSystemResourceAsStream(BANNER)) {
			sb.append(IOUtils.toString(in));
		} catch(IOException e) {
			sb.append("Slang");
		}
		sb.append(System.lineSeparator());
		sb.append(SlangCLI.getVersion());
		return sb.toString();
	}

	@Override
	public String getVersion() {
		return SlangCLI.getVersion();
	}

	@Override
	public String getWelcomeMessage() {
		return ASSISTANCE;
	}

}
