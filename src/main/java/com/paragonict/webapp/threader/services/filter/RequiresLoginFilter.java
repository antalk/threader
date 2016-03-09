package com.paragonict.webapp.threader.services.filter;

import java.io.IOException;

import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.ComponentRequestHandler;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.PageRenderRequestParameters;
import org.apache.tapestry5.services.Response;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.pages.Index;
import com.paragonict.webapp.threader.services.IAccountService;

public class RequiresLoginFilter implements ComponentRequestFilter {

	
	private IAccountService _acs;
	private ComponentSource _cs;
	private Response _rs;
	private PageRenderLinkSource _prls;
	
	public RequiresLoginFilter(IAccountService acs, ComponentSource componentSource,Response response,PageRenderLinkSource prls) {
		_acs = acs;
		_cs = componentSource;
		_rs = response;
		_prls = prls;
	}
	
	@Override
	public void handleComponentEvent(
			ComponentEventRequestParameters parameters,
			ComponentRequestHandler handler) throws IOException {
		
		System.err.println("handle comp event "+parameters.getEventType());
		
		if (_acs.isLoggedIn()) {
			handler.handleComponentEvent(parameters);	
		} else {
			System.err.println("component id" + parameters.getNestedComponentId());
			
			Component comp  = _cs.getComponent(parameters.getActivePageName()+":"+parameters.getNestedComponentId());
			if (comp != null) {
				if (! comp.getClass().isAnnotationPresent(RequiresLogin.class)) {
			    	handler.handleComponentEvent(parameters);
			    } else {
			    	_rs.getOutputStream("text/html").write("<html>ja daag</html>".getBytes());
			    	_rs.setStatus(401);
			    	//_rs.sendError(401, "Action requires login");
			    }
			} else {
				// just continue
				handler.handleComponentEvent(parameters);
			}
		}
	}

	@Override
	public void handlePageRender(PageRenderRequestParameters parameters,
			ComponentRequestHandler handler) throws IOException {
		
		System.err.println("handle page render for page " + parameters.getLogicalPageName());
		
		if (_acs.isLoggedIn()) {
			handler.handlePageRender(parameters);
		} else {
			// not logged in. check page for annotation
			Component page = _cs.getPage(parameters.getLogicalPageName());
		    if (! page.getClass().isAnnotationPresent(RequiresLogin.class)) {
		    	handler.handlePageRender(parameters);
		    } else {
		    	_rs.sendRedirect(_prls.createPageRenderLink(Index.class));
		    }
		}
	}

	
	
}
