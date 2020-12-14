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

import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.apache.commons.lang3.StringUtils;
import org.jbehave.core.model.Meta;
import rp.com.google.common.annotations.VisibleForTesting;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
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

    private static final int MAX_NAME_LENGTH = 256;

    private static final String KEY_VALUE_SEPARATOR = ":";

    private static final String META_PARAMETER_SEPARATOR = " ";

    @VisibleForTesting
    static final Pattern STEP_NAME_PATTERN = Pattern.compile("<(.*?)>");

    /**
     * Send stacktrace to ReportPortal
     *
     * @param cause
     */
    public static void sendStackTraceToRP(final Throwable cause) {

        ReportPortal.emitLog(itemUuid -> {
            SaveLogRQ rq = new SaveLogRQ();
            rq.setItemUuid(itemUuid);
            rq.setLevel("ERROR");
            rq.setLogTime(Calendar.getInstance().getTime());
            if (cause != null) {
                rq.setMessage(getStackTraceAsString(cause));
            } else {
                rq.setMessage("Test has failed without exception");
            }
            rq.setLogTime(Calendar.getInstance().getTime());

            return rq;
        });
    }

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

    public static String joinMetas(Meta... metas) {
        return ofNullable(metas).map(Arrays::asList).orElse(Collections.emptyList()).stream()
                .map(JBehaveUtils::joinMeta).collect(Collectors.joining(META_PARAMETER_SEPARATOR));
    }

    @VisibleForTesting
    static String expandParameters(String stepName, Map<String, String> parameters, List<ParameterResource> parameterResources) {
        Matcher m = STEP_NAME_PATTERN.matcher(stepName);
        StringBuffer buffer = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            if (parameters.containsKey(key)) {
                String value = parameters.get(key);
                m.appendReplacement(buffer, Matcher.quoteReplacement(value));
                ParameterResource param = buildParameter(key, value);
                parameterResources.add(param);
            }
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    private static ParameterResource buildParameter(String key, String value) {
        ParameterResource parameterResource = new ParameterResource();
        parameterResource.setKey(key);
        parameterResource.setValue(value);
        return parameterResource;
    }

    public static String normalizeName(String string) {
        String name;
        if (isNullOrEmpty(string)) {
            name = "UNKNOWN";
        } else if (string.length() > MAX_NAME_LENGTH) {
            name = string.substring(0, MAX_NAME_LENGTH - 1);
        } else {
            name = string;
        }
        return name;

    }
}
