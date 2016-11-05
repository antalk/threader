package com.paragonict.webapp.threader.services.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.hibernate.HibernateSessionSource;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.ParallelExecutor;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailFetcher;
import com.paragonict.webapp.threader.services.IMailStore;
import com.paragonict.webapp.threader.services.impl.fetcher.AsynchronousMailFetcher;
import com.paragonict.webapp.threader.services.impl.fetcher.FetcherKey;

public class MailFetcherImpl implements IMailFetcher {
	
	@Inject
	private ParallelExecutor executor;
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private HibernateSessionSource hss;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private IMailStore store;
	
	@Inject
	private LoggerSource logSource;
	
	@Inject
	private TypeCoercer tc;
	
	// can we maintain a map of (completed) futures? this way we know when the executor is still running
	// and possibly wait for enough local messages 
	private Map<FetcherKey,Future<Boolean>> futureHolder;
	
	private Logger logger;
	
	@PostInjection
	public void init() {
		logger = logSource.getLogger(this.getClass());
		futureHolder = new ConcurrentHashMap<>();
	}
	
	

	@Override
	public void isInSync(final String folder, int end, int nrOfMsgsOnServer) {
		
		try {
    		final Session s = hss.create();
			final List<String> UIDs = s.getNamedQuery(LocalMessage.GET_ALL_UIDS).
    				setLong("accountid", as.getAccount().getId()).
    				setString("folder",folder).list();
        	
    		int nrofDBRows = UIDs.size();

			
	 		final FetcherKey fk = new FetcherKey(folder, as.getAccount().getId());
    		
    		Future<Boolean> futureFetcher = null;
    		
    		if (futureHolder.containsKey(fk)) {
    			futureFetcher = futureHolder.get(fk);
    			if (futureFetcher.isCancelled() || futureFetcher.isDone()) {
    				// possibly an old fetcher done with work.
    				futureHolder.remove(fk);
    				futureFetcher = null;
    			} else {
    				logger.info("Future {} still running!", futureFetcher);
    			}
    		}
    		if (futureFetcher == null) {
    			// create and start the fetching process
    			futureFetcher = executor.invoke(new AsynchronousMailFetcher(tc,hss.create(),store.getUnmanagedStore(),fk,UIDs,nrOfMsgsOnServer));
				futureHolder.put(fk, futureFetcher);
    		} else {
    			logger.warn("Returning (running) future fetcher for account {} ",as.getAccount().getId());
    		}
    		// if future is running. wait for it or query the number of dbrows to see if there are enough msgs
    		
    		// keep querying the DB for the total number of rows 
    		// TODO limit waiting for like 5 minutes or something.
    		// then kill the future, throw exception and let the system retry
    		
    			
    		while ( nrofDBRows < end+1 && !futureFetcher.isDone() && !futureFetcher.isCancelled() ) {
				Thread.sleep(100); // sleep 100 ms
    			// the db is updated in another session, this one need to be cleared so we do a real DB query and not fetch cached info.
				//s.clear();
				s.flush();
    			nrofDBRows = ((Long) s.getNamedQuery(LocalMessage.COUNT_ALL).
    					setLong("accountid", as.getAccount().getId()).
    					setString("folder",folder).uniqueResult()).intValue();
    		}
    		if (futureFetcher.isDone() || futureFetcher.isCancelled()) {
    			if (futureHolder.remove(fk) != null) {
    				logger.debug("Future removed for {}", fk);
    			};
    		} else {
    			logger.info("Nr of local messages sufficient, future {} is still running?!",futureFetcher);
    		}
    		s.close();
    		logger.debug("Fetcher is done, nr of local messages is now: {}",nrofDBRows);
    		
    		
		} catch (Exception e ) {
			System.err.println(" we are fucked... " + e.getMessage());
			e.printStackTrace();
		}
	}

	
}
