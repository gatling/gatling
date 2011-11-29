/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.ui.component;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;
import static com.excilys.ebi.gatling.recorder.ui.Constants.GATLING_REQUEST_BODIES_DIRECTORY_NAME;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.SelectorUtils;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.Pattern;
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy;
import com.excilys.ebi.gatling.recorder.http.event.PauseEvent;
import com.excilys.ebi.gatling.recorder.http.event.RequestReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ResponseReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowConfigurationFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.TagEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.PauseType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class RunningFrame extends JFrame {

	private Configuration configuration;
	private GatlingHttpProxy proxy;
	private Date startDate;
	private Date lastRequest;

	private JTextField txtTag = new JTextField(10);
	private JButton btnTag = new JButton("Set");
	private DefaultListModel listElements = new DefaultListModel();
	private JList listExecutedRequests = new JList(listElements);
	private TextAreaPanel stringRequest = new TextAreaPanel("Request:");
	private TextAreaPanel stringResponse = new TextAreaPanel("Response:");
	private TextAreaPanel stringRequestBody = new TextAreaPanel("Request Body:");

	private List<Object> listEvents = new ArrayList<Object>();
	private String protocol;
	private String host;
	private int port;
	private String urlBase = null;
	private String urlBaseString = null;
	private LinkedHashMap<String, String> urls = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, Map<String, String>> headers = new LinkedHashMap<String, Map<String, String>>();

	public RunningFrame() {

		/* Initialization of the frame */
		setTitle("Recorder running...");
		setMinimumSize(new Dimension(660, 480));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		/* Declaration & initialization of components */
		JButton btnClear = new JButton("Clear");
		final JButton btnStop = new JButton("Stop !");
		btnStop.setSize(120, 30);

		JScrollPane panelFilters = new JScrollPane(listExecutedRequests);
		panelFilters.setPreferredSize(new Dimension(300, 100));

		stringRequest.setPreferredSize(new Dimension(330, 100));
		JSplitPane requestResponsePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(stringRequest), new JScrollPane(stringResponse));
		final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestResponsePane, stringRequestBody);

		/* Layout */
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 5, 0, 0);

		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridy = 0;
		add(new JLabel("Tag :"), gbc);

		gbc.gridx = 1;
		add(txtTag, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.5;
		add(btnTag, gbc);

		gbc.gridx = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 0.25;
		add(btnClear, gbc);

		gbc.gridx = 4;
		gbc.anchor = GridBagConstraints.LINE_END;
		add(btnStop, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.weighty = 0.25;
		gbc.fill = GridBagConstraints.BOTH;
		add(panelFilters, gbc);

		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0.75;
		gbc.fill = GridBagConstraints.BOTH;
		add(sp, gbc);

		/* Listeners */
		btnTag.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (!txtTag.getText().equals(EMPTY)) {
					TagEvent tag = new TagEvent(txtTag.getText());
					listElements.addElement(tag.toString());
					listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);
					listEvents.add(tag);
					txtTag.setText(EMPTY);
				}
			}
		});

		listExecutedRequests.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (listExecutedRequests.getSelectedIndex() >= 0) {
					Object obj = listEvents.get(listExecutedRequests.getSelectedIndex());
					if (obj instanceof ResponseReceivedEvent) {
						ResponseReceivedEvent event = (ResponseReceivedEvent) obj;
						stringRequest.txt.setText(event.getRequest().toString());
						stringResponse.txt.setText(event.getResponse().toString());
						stringRequestBody.txt.setText(new String(event.getRequest().getContent().array()));
					} else {
						stringRequest.txt.setText(EMPTY);
						stringResponse.txt.setText(EMPTY);
						stringRequestBody.txt.setText(EMPTY);
					}
				}
			}
		});

		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearOldRunning();
			}
		});

		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveScenario();
				proxy.shutdown();
				proxy = null;
				getEventBus().post(new ShowConfigurationFrameEvent());

			}
		});
	}

	@Subscribe
	public void onShowConfigurationFrameEvent(ShowConfigurationFrameEvent event) {
		setVisible(false);
	}

	@Subscribe
	public void onShowRunningFrameEvent(ShowRunningFrameEvent event) {
		setVisible(true);
		clearOldRunning();
		configuration = event.getConfiguration();
		startDate = new Date();
		proxy = new GatlingHttpProxy(configuration.getPort(), configuration.getSslPort(), configuration.getProxy());
		proxy.start();
	}

	@Subscribe
	public void onRequestReceivedEvent(RequestReceivedEvent event) {
		if (addRequest(event.getRequest())) {
			if (lastRequest != null) {
				Date newRequest = new Date();
				long diff = newRequest.getTime() - lastRequest.getTime();
				long pauseMin, pauseMax;
				PauseType pauseType;
				if (diff < 1000) {
					pauseMin = (diff / 100l) * 100;
					pauseMax = pauseMin + 100;
					pauseType = PauseType.MILLISECONDS;
				} else {
					pauseMin = diff / 1000l;
					pauseMax = pauseMin + 1;
					pauseType = PauseType.SECONDS;
				}
				lastRequest = newRequest;
				PauseEvent pause = new PauseEvent(pauseMin, pauseMax, pauseType);
				listElements.addElement(pause.toString());
				listEvents.add(pause);
			}
		}
	}

	@Subscribe
	public void onResponseReceivedEvent(ResponseReceivedEvent event) {
		if (addRequest(event.getRequest())) {
			lastRequest = new Date();
			processRequest(event);
		}
	}

	private void clearOldRunning() {
		listElements.removeAllElements();
		stringRequest.txt.setText(EMPTY);
		stringRequestBody.txt.setText(EMPTY);
		stringResponse.txt.setText(EMPTY);
		listEvents.clear();
		urls.clear();
		headers.clear();
		protocol = null;
		host = null;
		port = -1;
		urlBase = null;
		urlBaseString = null;
		lastRequest = null;
	}

	private boolean addRequest(HttpRequest request) {
		URI uri = null;
		try {
			uri = new URI(request.getUri());
		} catch (URISyntaxException ex) {
			System.err.println("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
			// FIXME error handling
			return false;
		}

		if (configuration.getFilterType() != FilterType.ALL) {

			String p = EMPTY;
			boolean add = true;
			if (configuration.getFilterType() == FilterType.ONLY)
				add = true;
			else if (configuration.getFilterType() == FilterType.EXCEPT)
				add = false;

			for (Pattern pattern : configuration.getPatterns()) {
				switch (pattern.getPatternType()) {
				case ANT:
					p = SelectorUtils.ANT_HANDLER_PREFIX;
					break;
				case JAVA:
					p = SelectorUtils.REGEX_HANDLER_PREFIX;
					break;
				}
				p += pattern.getPattern() + SelectorUtils.PATTERN_HANDLER_SUFFIX;
				if (SelectorUtils.matchPath(p, uri.getPath()))
					return add;
			}
			return !add;
		}
		return true;
	}

	private void processRequest(ResponseReceivedEvent event) {

		HttpRequest request = event.getRequest();

		URI uri = null;
		try {
			uri = new URI(request.getUri());
		} catch (URISyntaxException ex) {
			System.err.println("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
		}

		listElements.addElement(request.getMethod() + " | " + request.getUri());
		listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);

		int id = listEvents.size() + 1;
		event.setId(id);

		/* URLs */
		if (urlBase == null) {
			protocol = uri.getScheme();
			host = uri.getHost();
			port = uri.getPort();
			urlBase = protocol + "://" + host;
			urlBaseString = "PROTOCOL + \"://\" + HOST";
			if (port != -1) {
				urlBase += ":" + port;
				urlBaseString += " + \":\" + PORT";
			}
		}

		String requestUrlBase = uri.getScheme() + "://" + uri.getHost();
		if (uri.getPort() != -1)
			requestUrlBase += ":" + uri.getPort();
		if (requestUrlBase.equals(urlBase))
			urls.put("url_" + id, uri.getPath());
		else
			urls.put("url_" + id, uri.toString());

		/* Headers */
		TreeMap<String, String> hm = new TreeMap<String, String>();
		for (Entry<String, String> entry : request.getHeaders()) {
			hm.put(entry.getKey(), entry.getValue());
		}
		headers.put("headers_" + id, hm);

		/* Params */
		QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
		event.getRequestParams().putAll((decoder.getParameters()));

		/* Content */
		if (request.getContent().capacity() > 0) {
			String content = new String(request.getContent().array());
			// We check if it's a form validation and so we extract post params
			if ("application/x-www-form-urlencoded".equals(request.getHeader("Content-Type"))) {
				decoder = new QueryStringDecoder("http://localhost/?" + content);
				event.getRequestParams().putAll(decoder.getParameters());
			} else {
				event.setWithBody(true);
				dumpRequestBody(id, content);
			}
		}
		listEvents.add(event);
	}

	private void dumpRequestBody(int idEvent, String content) {
		// Dump request body
		File dir = new File(configuration.getResultPath(), ResultType.FORMAT.format(startDate) + "_" + GATLING_REQUEST_BODIES_DIRECTORY_NAME);
		if (!dir.exists())
			dir.mkdir();

		FileWriter fw = null;
		try {
			fw = new FileWriter(new File(dir, "request_" + idEvent + ".txt"));
			fw.write(content);

		} catch (IOException ex) {
			System.err.println("Error, while dumping request body..." + ex.getStackTrace());

		} finally {
			closeQuietly(fw);
		}
	}

	private void saveScenario() {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();

		VelocityContext context = new VelocityContext();
		context.put("protocol", protocol);
		context.put("host", host);
		context.put("port", port);
		context.put("urlBase", urlBaseString);
		context.put("urls", urls);
		context.put("headers", headers);
		context.put("name", "Scenario name");
		context.put("events", listEvents);

		Template template = null;
		FileWriter fileWriter = null;
		for (ResultType resultType : configuration.getResultTypes()) {

			try {
				template = ve.getTemplate(resultType.getTemplate());
				fileWriter = new FileWriter(new File(configuration.getResultPath(), resultType.getScenarioFileName(startDate)));
				template.merge(context, fileWriter);
				fileWriter.flush();

			} catch (IOException e) {
				System.err.println("Error, while saving '" + resultType + "' scenario..." + e.getStackTrace());

			} finally {
				closeQuietly(fileWriter);
			}
		}
	}
}
