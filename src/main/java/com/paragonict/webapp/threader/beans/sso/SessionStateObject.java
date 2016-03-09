package com.paragonict.webapp.threader.beans.sso;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience object that is contained in the Session as permanent storage in {@link AbstractBasicPage}
 * 
 * And contains ALL variables that should be available throughout the whole session for the user in a Hashmap.
 * 
 * keep it as small as possible , coz these can be real memory hoggers
 * 
 */
public final class SessionStateObject implements Serializable {

	private static final long serialVersionUID = -4921901719328401682L;
	
	public enum SESSION_ATTRS {
		USER_ID(Long.class),
		ADMIN_ID(Long.class),
		SELECTED_FOLDER(String.class),
		DRAFT_UID(String.class),
		SELECTED_MSG_UID(String.class);   // String, UUID of the message
		
		private final Class clazz;
		
		private SESSION_ATTRS(Class clazz) {
			this.clazz = clazz;
		}
		
		public Class getClazz() {
			return clazz;
		}
	}

	private  Map<String,Object> _internalMap;
	
	// public constructor used for creation of the object by Tapestry
	public SessionStateObject() {
		_internalMap = new HashMap<String, Object>(4); // not too big..
	}
	
	public boolean hasValue(final SESSION_ATTRS key) {
		return _internalMap.get(key.name()) != null;
	}
	
	public String getStringValue(final SESSION_ATTRS key) {
		return getValue(key,String.class);
	}
	
	public Integer getIntegerValue(final SESSION_ATTRS key) {
		return getValue(key,Integer.class);
	}
	
	public Long getLongValue(final SESSION_ATTRS key) {
		return getValue(key,Long.class);
	}
	

	@SuppressWarnings("unchecked")
	private <T> T getValue(final SESSION_ATTRS key,Class<T> clazz) {
		if (!(key.getClazz().equals(clazz))) {
			throw new IllegalArgumentException("Session variable ["+key+"] is not a(n) : " + clazz + ". But :"+ key.getClazz());
		}
		return (T) _internalMap.get(key.name());
	}

	/**
	 * Put a value for the given key in the session, caution!: will override!
	 * be sure to use a unique key for your member name
	 * 
	 * @param key
	 * @param value
	 */
	public void putValue(final SESSION_ATTRS key,final Object value) {
		if (!(value.getClass().equals(key.getClazz()))) {
			throw new IllegalArgumentException("Session value ["+value+"] is not a(n) : " + key.getClazz());

		}
		_internalMap.put(key.name(), value);
	}
	
	public void clearValue(final SESSION_ATTRS... key) {
		for (SESSION_ATTRS k: key) {
			_internalMap.remove(k.name());
		}
	}
	
	public void clearAll() {
		_internalMap.clear();
	}
	
	@Override
	public String toString() {
		final StringBuilder build = new StringBuilder();
		build.append("SessionStateObject:\n");
		for (String key:_internalMap.keySet()) {
			build.append("Key: ["+key+"] has value ["+_internalMap.get(key)+"] \n");
		}
		return build.toString();
	}
}

