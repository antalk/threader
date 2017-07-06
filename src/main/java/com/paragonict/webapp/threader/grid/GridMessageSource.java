package com.paragonict.webapp.threader.grid;

import java.sql.Date;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IMailService;

public class GridMessageSource implements GridDataSource {
	
	private final IMailService _ms;
	private final String _folder;
	private List<LocalMessage> preparedResults;
	private final Logger _logger;
	
	private int _startIndex;
	private final int _nrOfRows;
	
	public GridMessageSource(final IMailService ms,final Logger logger, final String folder) throws MessagingException {
		_ms = ms;
		_folder = folder;
		_logger = logger;
		_nrOfRows = _ms.getNrOfMessages(folder);// fail fast..	
	}
	

	@Override
	public int getAvailableRows() {
		return _nrOfRows;
	}

	@Override
	public void prepare(int startIndex, int endIndex,List<SortConstraint> sortConstraints) {
		
		try {
			
			_startIndex = startIndex;
			
			System.err.println("GRID START MSGS: " + System.currentTimeMillis());
			
			System.err.println("START" + startIndex );
			System.err.println("END" + endIndex );
			System.err.println("NR OF ROWS  on server" + _nrOfRows);
			
			if (_nrOfRows < endIndex + 1) {
				System.err.println(MessageFormat.format("There are LESS messages {} on the server than requested {}, returning max. number of msgs on server", _nrOfRows,endIndex+1));
			}
			
			
			// can only be 1 sort !! (!)!??
			SortConstraint sc = null;
			if (!sortConstraints.isEmpty()) {
				sc = sortConstraints.get(0);
			}
			
			Long currentTime  = System.currentTimeMillis();
			_logger.debug("Starting getMessages at {}", new Date(currentTime));
			
			preparedResults = _ms.getMessages(_folder, startIndex, endIndex,_nrOfRows,sc);
			_logger.debug("Synced getMessages with remote, took {} ms", System.currentTimeMillis()-currentTime);
			
			
			System.err.println("GRID END MSGS: " + System.currentTimeMillis());
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			preparedResults = Collections.emptyList();
		}
	}

	@Override
	public LocalMessage getRowValue(int index) {
		return preparedResults.get(index-_startIndex);
	}

	@Override
	public Class<LocalMessage> getRowType() {
		return LocalMessage.class;
	}

}
