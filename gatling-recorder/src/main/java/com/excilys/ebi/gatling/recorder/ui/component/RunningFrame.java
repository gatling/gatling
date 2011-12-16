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
package com.excilys.ebi.gatling.recorder.ui.component;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;
import static com.excilys.ebi.gatling.recorder.ui.Constants.GATLING_REQUEST_BODIES_DIRECTORY_NAME;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.awt.Dimension;
import java.awt.EventQueue;
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
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.SelectorUtils;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.configuration.Pattern;
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy;
import com.excilys.ebi.gatling.recorder.http.event.PauseEvent;
import com.excilys.ebi.gatling.recorder.http.event.RequestReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ResponseReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowConfigurationFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.ShowRunningFrameEvent;
import com.excilys.ebi.gatling.recorder.http.event.TagEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterStrategy;
import com.excilys.ebi.gatling.recorder.ui.enumeration.PauseType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class RunningFrame extends JFrame {

	private static final Logger logger = LoggerFactory.getLogger(RunningFrame.class);

	private Configuration configuration;
	private GatlingHttpProxy proxy;
	private Date startDate;
	private Date lastRequest;
	private PauseEvent pause;

	private JTextField txtTag = new JTextField(15);
	private JButton btnTag = new JButton("Add");
	private DefaultListModel listElements = new DefaultListModel();
	private JList listExecutedRequests = new JList(listElements);
	private TextAreaPanel stringRequest = new TextAreaPanel("Request:");
	private TextAreaPanel stringResponse = new TextAreaPanel("Response:");
	private TextAreaPanel stringRequestBody = new TextAreaPanel("Request Body:");
	private int numberOfRequests = 0;

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
		setTitle("Gatling Recorder - Running...");
		setMinimumSize(new Dimension(800, 640));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		setIconImages(Commons.getIconList());

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
				if (!Configuration.getInstance().isConfigurationSkipped())
					getEventBus().post(new ShowConfigurationFrameEvent());
				else
					System.exit(0);
			}
		});
	}

	@Subscribe
	public void onShowConfigurationFrameEvent(ShowConfigurationFrameEvent event) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setVisible(false);
			}
		});
	}

	@Subscribe
	public void onShowRunningFrameEvent(ShowRunningFrameEvent event) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setVisible(true);
				clearOldRunning();
				configuration = Configuration.getInstance();
				startDate = new Date();
				proxy = new GatlingHttpProxy(configuration.getPort(), configuration.getSslPort(), configuration.getProxy());
				proxy.start();
			}
		});
	}

	@Subscribe
	public void onRequestReceivedEvent(final RequestReceivedEvent event) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				String header = event.getRequest().getHeader("Proxy-Authorization");
				if (header != null) {
					// Split on " " and take 2nd match (Basic
					// credentialsInBase64==)
					String credentials = new String(Base64.decodeBase64(header.split(" ")[1].getBytes()));
					configuration.getProxy().setUsername(credentials.split(":")[0]);
					configuration.getProxy().setPassword(credentials.split(":")[1]);
				}

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
						pause = new PauseEvent(pauseMin, pauseMax, pauseType);
					}
				}
			}
		});
	}

	@Subscribe
	public void onResponseReceivedEvent(final ResponseReceivedEvent event) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (addRequest(event.getRequest())) {
					if (pause != null) {
						listElements.addElement(pause.toString());
						listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);
						listEvents.add(pause);
					}
					lastRequest = new Date();
					processRequest(event);
				}
			}
		});
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
			logger.error("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
			// FIXME error handling
			return false;
		}

		if (configuration.getFilterStrategy() != FilterStrategy.NONE) {

			String p = EMPTY;
			boolean add = true;
			if (configuration.getFilterStrategy() == FilterStrategy.ONLY)
				add = true;
			else if (configuration.getFilterStrategy() == FilterStrategy.EXCEPT)
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
			logger.error("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
		}

		listElements.addElement(request.getMethod() + " | " + request.getUri());
		listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);

		int id = ++numberOfRequests;
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
			event.setWithUrlBase(true);
		else
			urls.put("url_" + id, requestUrlBase + uri.getPath());

		/* Headers */
		Map<String, String> requestHeaders = new TreeMap<String, String>();
		for (Entry<String, String> entry : request.getHeaders())
			requestHeaders.put(entry.getKey(), entry.getValue());
		requestHeaders.remove("Cookie");

		int bestChoice = 0;
		String headerKey = EMPTY;
		MapDifference<String, String> diff;
		Map<String, String> fullHeaders = new TreeMap<String, String>();
		boolean containsHeaders = false;

		if (headers.size() > 0) {
			for (Entry<String, Map<String, String>> header : headers.entrySet()) {

				fullHeaders = new TreeMap<String, String>(header.getValue());
				containsHeaders = false;

				if (header.getValue().containsKey("headers")) {
					fullHeaders.putAll(headers.get(header.getValue().get("headers")));
					fullHeaders.remove("headers");
					containsHeaders = true;
				}

				diff = Maps.difference(fullHeaders, requestHeaders);
				logger.debug(diff.toString());
				if (diff.areEqual()) {
					headerKey = header.getKey();
					bestChoice = 1;
					break;
				} else if (diff.entriesOnlyOnLeft().size() == 0 && diff.entriesDiffering().size() == 0 && !containsHeaders) {
					// header are included in requestHeaders
					headerKey = header.getKey();
					bestChoice = 2;
				} else if (bestChoice > 2 && diff.entriesOnlyOnRight().size() == 0 && diff.entriesDiffering().size() == 0 && !containsHeaders) {
					// requestHeaders are included in header
					headerKey = header.getKey();
					bestChoice = 3;
				}
			}
		}

		switch (bestChoice) {
		case 1:
			event.setHeadersId(headerKey);
			break;
		case 2:
			diff = Maps.difference(headers.get(headerKey), requestHeaders);
			TreeMap<String, String> tm2 = new TreeMap<String, String>(diff.entriesOnlyOnRight());
			headers.put("headers_" + id, tm2);
			headers.get("headers_" + id).put("headers", headerKey);
			event.setHeadersId("headers_" + id);
			break;
		case 3:
			diff = Maps.difference(headers.get(headerKey), requestHeaders);
			TreeMap<String, String> tm3 = new TreeMap<String, String>(diff.entriesInCommon());
			headers.put("headers_" + id, tm3);
			event.setHeadersId("headers_" + id);
			headers.remove(headerKey);
			tm3 = new TreeMap<String, String>(diff.entriesOnlyOnLeft());
			headers.put(headerKey, tm3);
			headers.get(headerKey).put("headers", "headers_" + id);
			break;
		default:
			headers.put("headers_" + id, requestHeaders);
			event.setHeadersId("headers_" + id);
		}

		/* Add check if status is not in 20X */
		if ((event.getResponse().getStatus().getCode() < 200) || (event.getResponse().getStatus().getCode() > 210))
			event.setWithCheck(true);

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
		File dir = null;
		if (configuration.getRequestBodiesFolder() == null)
			dir = new File(getOutputFolder(), ResultType.FORMAT.format(startDate) + "_" + GATLING_REQUEST_BODIES_DIRECTORY_NAME);
		else
			dir = getFolder("request bodies", configuration.getRequestBodiesFolder());

		if (!dir.exists())
			dir.mkdir();

		FileWriter fw = null;
		try {
			fw = new FileWriter(new File(dir, ResultType.FORMAT.format(startDate) + "_request_" + idEvent + ".txt"));
			fw.write(content);
		} catch (IOException ex) {
			logger.error("Error, while dumping request body... {}", ex.getStackTrace());
		} finally {
			closeQuietly(fw);
		}
	}

	private File getOutputFolder() {
		return getFolder("output", configuration.getOutputFolder());
	}

	private File getFolder(String folderName, String folderPath) {
		File folder = new File(folderPath);

		if (!folder.exists())
			if (!folder.mkdirs())
				throw new RuntimeException("Can't create " + folderName + " folder");

		return folder;
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
		context.put("proxy", configuration.getProxy());
		context.put("urls", urls);
		context.put("headers", headers);
		context.put("name", "Scenario name");
		context.put("events", listEvents);
		context.put("package", Configuration.getInstance().getEclipsePackage());
		context.put("date", ResultType.FORMAT.format(startDate));
		URI uri = URI.create("");
		context.put("URI", uri);

		Template template = null;
		FileWriter fileWriter = null;
		for (ResultType resultType : configuration.getResultTypes()) {
			try {
				template = ve.getTemplate(resultType.getTemplate());
				fileWriter = new FileWriter(new File(getOutputFolder(), resultType.getScenarioFileName(startDate)));
				template.merge(context, fileWriter);
				fileWriter.flush();

			} catch (IOException e) {
				logger.error("Error, while saving '" + resultType + "' scenario..." + e.getStackTrace());

			} finally {
				closeQuietly(fileWriter);
			}
		}
	}
}
