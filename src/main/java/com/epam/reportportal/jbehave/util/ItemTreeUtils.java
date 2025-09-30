/*
 * Copyright 2021 EPAM Systems
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

package com.epam.reportportal.jbehave.util;

import com.epam.reportportal.service.tree.TestItemTree;
import org.jbehave.core.model.Story;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

import static com.epam.reportportal.jbehave.JBehaveUtils.formatExampleKey;

public class ItemTreeUtils {

	private ItemTreeUtils() {
		throw new AssertionError("No instances should exist for the class!");
	}

	public static TestItemTree.ItemTreeKey createKey(@Nullable final String key) {
		return TestItemTree.ItemTreeKey.of(key);
	}

	public static TestItemTree.ItemTreeKey createKey(@Nonnull final Story key) {
		return TestItemTree.ItemTreeKey.of(key.getPath());
	}

	public static TestItemTree.ItemTreeKey createKey(@Nonnull final Map<String, String> example) {
		return TestItemTree.ItemTreeKey.of(formatExampleKey(example));
	}
}
