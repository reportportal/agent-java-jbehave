/*
 * Copyright (C) 2021 EPAM Systems
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
package com.epam.reportportal.jbehave;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Set of useful utils related to JBehave -> ReportPortal integration
 *
 * @author Vadzim Hushchanskou
 */
public class JBehaveUtils {

	private JBehaveUtils() {
		// static utilities class
	}

	private static final String PARAMETER_ITEMS_START = "[";
	private static final String PARAMETER_ITEMS_END = "]";
	private static final String PARAMETER_ITEMS_DELIMITER = ";";
	private static final String KEY_VALUE_SEPARATOR = ":";

	public static String formatExampleKey(@Nonnull final Map<String, String> example) {
		return example.entrySet()
				.stream()
				.map(e -> e.getKey() + KEY_VALUE_SEPARATOR + e.getValue())
				.collect(Collectors.joining(PARAMETER_ITEMS_DELIMITER, PARAMETER_ITEMS_START, PARAMETER_ITEMS_END));
	}
}
