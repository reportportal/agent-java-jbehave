/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.util;

import com.epam.reportportal.service.tree.TestItemTree;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;

public class ItemTreeUtils {

	private ItemTreeUtils() {
		throw new AssertionError("No instances should exist for the class!");
	}

	public static TestItemTree.ItemTreeKey createKey(String key) {
		return TestItemTree.ItemTreeKey.of(key);
	}

	public static TestItemTree.ItemTreeKey createKey(Story key) {
		return TestItemTree.ItemTreeKey.of(key.getPath());
	}

	public static TestItemTree.ItemTreeKey createKey(Scenario key) {
		return TestItemTree.ItemTreeKey.of(key.getTitle());
	}
}
