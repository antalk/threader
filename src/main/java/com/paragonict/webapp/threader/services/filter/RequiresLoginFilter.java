package com.paragonict.webapp.threader.services.filter;

import java.io.IOException;

import org.apache.tapestry5.internal.EmptyEventContext;
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

	
	private final IAccountService _acs;
	private final ComponentSource _cs;
	private final Response _rs;
	private final PageRenderLinkSource _prls;
	
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
 			System.err.println("(not logged) in component " + parameters.getNestedComponentId());
 			
 			Component comp  = _cs.getComponent(parameters.getActivePageName()+":"+parameters.getNestedComponentId());
 			
 			while (comp.getComponentResources().getContainer() != null && 
 					!comp.getClass().isAnnotationPresent(RequiresLogin.class)) {
 				comp = comp.getComponentResources().getContainer();
 			}
 			if (comp.getClass().isAnnotationPresent(RequiresLogin.class)) {
					// rewrite the event to 'sessionexpired'
 			    	parameters = new ComponentEventRequestParameters(parameters.getActivePageName(), 
			    			parameters.getContainingPageName(),
			    			parameters.getNestedComponentId(),
			    			"sessionexpired", 
			    			new EmptyEventContext(),
			    			parameters.getEventContext());
			}
			// continue normally
			handler.handleComponentEvent(parameters);
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
