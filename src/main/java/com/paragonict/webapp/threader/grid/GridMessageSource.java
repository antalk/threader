package com.paragonict.webapp.threader.grid;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;

import com.paragonict.webapp.threader.beans.ClientMessage;
import com.paragonict.webapp.threader.services.IMailService;

public class GridMessageSource implements GridDataSource {
	
	private final IMailService _ms;
	private final String _folder;
	private List<ClientMessage> preparedResults;
	
	private int _startIndex;
	
	public GridMessageSource(final IMailService ms,final String folder) {
		_ms = ms;
		_folder = folder;
	}
	

	@Override
	public int getAvailableRows() {
		try {
			return _ms.getNrOfMessages(_folder);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public void prepare(int startIndex, int endIndex,
			List<SortConstraint> sortConstraints) {
		try {
			
			_startIndex = startIndex;
			
			System.err.println("GRID START MSGS" + System.currentTimeMillis());
			
			System.err.println("START" + startIndex );
			System.err.println("END" + endIndex );
			
			// can only be 1 sort !! (!)!??
			SortConstraint sc = null;
			if (!sortConstraints.isEmpty()) {
				sc = sortConstraints.get(0);
			}
			
			preparedResults = _ms.getMessages(_folder, startIndex, endIndex,sc);
			System.err.println("GRID END MSGS" + System.currentTimeMillis());
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ClientMessage getRowValue(int index) {
		return preparedResults.get(index-_startIndex);
	}

	@Override
	public Class<ClientMessage> getRowType() {
		return ClientMessage.class;
	}

}
