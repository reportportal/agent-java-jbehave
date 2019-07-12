/*
 * Copyright 2019 EPAM Systems
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

import org.jbehave.core.Embeddable;
import org.jbehave.core.embedder.Embedder;

/**
 * {@link Embeddable} decorator to be able to start and finish execution in
 * ReportPortal
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalEmbeddable implements Embeddable {

	private Embeddable embeddable;

	public ReportPortalEmbeddable(Embeddable embeddable) {
		this.embeddable = embeddable;
	}

	@Override
	public void useEmbedder(Embedder embedder) {
		embeddable.useEmbedder(embedder);
	}

	@Override
	public void run() {
		try {
			embeddable.run();
		} finally {
			JBehaveUtils.finishLaunch();
		}
	}

}
