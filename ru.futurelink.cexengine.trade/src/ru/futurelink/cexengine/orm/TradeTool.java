/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Торговый инструмент.
 * 
 * @author pavlov
 *
 */
@Entity
public class TradeTool implements Serializable {
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
	
	@JoinColumn(name = "currency1")
	private TradeCurrency mCurrency1;
	public TradeCurrency getCurrency1() { return mCurrency1; }
	public void setCurrency1(TradeCurrency c) { mCurrency1 = c; }

	@JoinColumn(name = "curreccy2")
	private TradeCurrency mCurrency2;
	public TradeCurrency getCurrency2() { return mCurrency2; }
	public void setCurrency2(TradeCurrency c) { mCurrency2 = c; }
}
