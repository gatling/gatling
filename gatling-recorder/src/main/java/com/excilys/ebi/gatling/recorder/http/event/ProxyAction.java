package com.excilys.ebi.gatling.recorder.http.event;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public abstract class ProxyAction {

	private final Collection<ProxyListener> listeners = new ArrayList<ProxyListener>();
	private final ChannelGroup group = new DefaultChannelGroup("Gatling_Recorder");

	public ChannelGroup getGroup() {
		return group;
	}

	public void addProxyListener(ProxyListener listener) {
		listeners.add(listener);
	}

	public void removeProxyListener(ProxyListener listener) {
		listeners.remove(listener);
	}

	public void clearProxyListeners() {
		listeners.clear();
	}

	public void notifyListeners(ProxyEvent event) {

		if (event == null || event.getSource() == null)
			throw new IllegalArgumentException();

		for (ProxyListener listener : listeners) {
			if (event.getSource() instanceof HttpRequest)
				listener.onHttpRequest(event);
			else if (event.getSource() instanceof HttpResponse)
				listener.onHttpResponse(event);
		}
	}
}
