/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.util;

import com.epam.reportportal.service.tree.TestItemTree;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

	public static TestItemTree.ItemTreeKey createKey(@Nonnull final Scenario key) {
		return TestItemTree.ItemTreeKey.of(key.getTitle());
	}

	public static TestItemTree.ItemTreeKey createKey(@Nonnull final Map<String, String> example) {
		return TestItemTree.ItemTreeKey.of(formatExampleKey(example));
	}
}
