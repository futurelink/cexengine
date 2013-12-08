/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author pavlov
 *
 */
@Entity
public class TradeCurrency implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * ID объекта
	 */
	@Id
	@GeneratedValue(generator="system-uuid")
	@Column(name = "id", columnDefinition="VARCHAR(64)", nullable=false)
	private		String mId;	
	public 		String getId() {	return mId;	}
	public		void setId(String id) { mId = id; }

	@Column(name = "title")
	private String mTitle;
	public void setTitle(String title) { mTitle = title; }
	public String getTitle() { return mTitle; }
}
