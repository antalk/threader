package com.paragonict.webapp.threader.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * An abstract base class for entities with an auto-generated ID column of type long
 * 
 * Maps to columns of type bigint(20)
 * 
 * 
 * @author avankalleveen
 *
 */
@MappedSuperclass
public abstract class AbstractIdEntity implements Serializable {
    
	private static final long serialVersionUID = -7258735135627913098L;
	private Long id;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
    public Long getId() {
        return id;
    }
    
    public void setId(final Long id) {
        this.id = id;
    }

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	
	    StringBuilder retValue = new StringBuilder();
	    
	    retValue.append("AbstractIdEntity ( ")
	        .append(super.toString()).append(TAB)
	        .append("id = ").append(this.id).append(TAB)
	        .append(" )");
	    
	    return retValue.toString();
	}
    
}