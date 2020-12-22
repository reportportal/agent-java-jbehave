/*
 * Copyright (C) 2019 EPAM Systems
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

import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.jbehave.core.model.Meta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * Set of usefull utils related to JBehave -> ReportPortal integration
 *
 * @author Andrei Varabyeu
 */
public class JBehaveUtils {

	private JBehaveUtils() {
		// static utilities class
	}

	private static final String PARAMETER_ITEMS_START = "[";
	private static final String PARAMETER_ITEMS_END = "]";
	private static final String PARAMETER_ITEMS_DELIMITER = ";";
	private static final String KEY_VALUE_SEPARATOR = ":";

	private static final String META_PARAMETER_SEPARATOR = " ";

	private static String joinMeta(Meta meta) {

		if (null == meta) {
			return StringUtils.EMPTY;
		}
		Iterator<String> metaParametersIterator = meta.getPropertyNames().iterator();

		if (metaParametersIterator.hasNext()) {
			StringBuilder appendable = new StringBuilder();
			String firstParameter = metaParametersIterator.next();
			appendable.append(joinMeta(firstParameter, meta.getProperty(firstParameter)));
			while (metaParametersIterator.hasNext()) {
				String nextParameter = metaParametersIterator.next();
				appendable.append(META_PARAMETER_SEPARATOR);
				appendable.append(joinMeta(nextParameter, meta.getProperty(nextParameter)));
			}
			return appendable.toString();
		}
		return StringUtils.EMPTY;

	}

	private static Map<String, String> metaToMap(Meta meta) {
		if (null == meta) {
			return Collections.emptyMap();
		}
		Map<String, String> metaMap = new HashMap<>(meta.getPropertyNames().size());
		for (String name : meta.getPropertyNames()) {
			metaMap.put(name, meta.getProperty(name));
		}
		return metaMap;

	}

	// TODO rename as join metas
	public static Map<String, String> metasToMap(Meta... metas) {
		if (null != metas && metas.length > 0) {
			Map<String, String> metaMap = new HashMap<>();
			for (Meta meta : metas) {
				metaMap.putAll(metaToMap(meta));
			}
			return metaMap;
		} else {
			return Collections.emptyMap();
		}
	}

	public static String joinMeta(Map<String, String> metaParameters) {
		Iterator<Entry<String, String>> metaParametersIterator = metaParameters.entrySet().iterator();

		if (metaParametersIterator.hasNext()) {
			StringBuilder appendable = new StringBuilder();
			Entry<String, String> firstParameter = metaParametersIterator.next();
			appendable.append(joinMeta(firstParameter.getKey(), firstParameter.getValue()));
			while (metaParametersIterator.hasNext()) {
				Entry<String, String> nextParameter = metaParametersIterator.next();
				appendable.append(META_PARAMETER_SEPARATOR);
				appendable.append(joinMeta(nextParameter.getKey(), nextParameter.getValue()));
			}
			return appendable.toString();
		}
		return StringUtils.EMPTY;

	}

	public static String joinMeta(String key, String value) {
		if (null == value) {
			return key;
		}
		if (value.toLowerCase().startsWith("http")) {
			String text;
			if (value.toLowerCase().contains("jira")) {
				text = key + KEY_VALUE_SEPARATOR + StringUtils.substringAfterLast(value, "/");
			} else {
				text = key;
			}
			return wrapAsLink(value, text);
		} else {
			return key + KEY_VALUE_SEPARATOR + value;
		}

	}

	// TODO should be removed since RP doesn't render HTML in the description
	@Deprecated
	private static String wrapAsLink(String href, String text) {
		return new StringBuilder("<a href=\"").append(href).append("\">").append(text).append("</a>").toString();
	}

	public static String formatExampleKey(@Nonnull final Map<String, String> example) {
		return example.entrySet()
				.stream()
				.map(e -> e.getKey() + KEY_VALUE_SEPARATOR + e.getValue())
				.collect(Collectors.joining(PARAMETER_ITEMS_DELIMITER, PARAMETER_ITEMS_START, PARAMETER_ITEMS_END));
	}
}
