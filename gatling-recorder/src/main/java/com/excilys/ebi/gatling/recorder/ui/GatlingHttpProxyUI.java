/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.ui;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.awt.EventQueue;

import com.excilys.ebi.gatling.recorder.configuration.ConfigurationHelper;
import com.excilys.ebi.gatling.recorder.http.event.ShowConfigurationFrameEvent;
import com.excilys.ebi.gatling.recorder.ui.component.ConfigurationFrame;
import com.excilys.ebi.gatling.recorder.ui.component.RunningFrame;

public class GatlingHttpProxyUI {

	public static void main(String[] args) {

		ConfigurationHelper.initConfiguration(args);

		EventQueue.invokeLater(new Runnable() {

			public void run() {
				getEventBus().register(new RunningFrame());
				getEventBus().register(new ConfigurationFrame());
				getEventBus().post(new ShowConfigurationFrameEvent());
			}
		});
	}
}
