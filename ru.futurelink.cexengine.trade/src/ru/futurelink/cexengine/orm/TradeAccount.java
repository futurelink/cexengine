/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.eclipse.persistence.annotations.Index;

/**
 * @author pavlov
 *
 */
@Entity
public class TradeAccount implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TradeAccount() {}
	
	/**
	 * ID объекта
	 */
	@Id
	@GeneratedValue(generator="system-uuid")
	@Column(name = "id", columnDefinition="VARCHAR(64)", nullable=false)
	private		String mId;	
	public 		String getId() {	return mId;	}
	public		void setId(String id) { mId = id; }
	
	@Index
	@Column(name="number")
	private String mNumber;
	public void setNumber(String number) { mNumber = number; 	}
	public 	String getNumber() {	return mNumber;	}
	
}
