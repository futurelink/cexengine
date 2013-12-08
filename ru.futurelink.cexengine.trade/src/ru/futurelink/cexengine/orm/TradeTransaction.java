/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.eclipse.persistence.annotations.Index;

/**
 * @author pavlov
 *
 */
@Entity
public class TradeTransaction {

	/**
	 * ID объекта
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", nullable=false)
	private		BigInteger mId;	
	public 		BigInteger getId() {	return mId;	}
	public		void setId(BigInteger id) { mId = id; }

	@Index
	@JoinColumn(name="wallet")
	private TradeWallet mWallet;
	public void setWallet(TradeWallet wallet) { mWallet = wallet; } 
	
	@Column(name="sum")
	private BigDecimal mSum;
	public void setSum(BigDecimal sum) { mSum = sum; }
	
	@Index
	@JoinColumn(name="deal")
	private TradeDeal mDeal;
	public void setDeal(TradeDeal deal) { mDeal = deal; }

	@Column(name="currencyTitle")
	private String mCurrencyTitle;
	public void setCurrencyTitle(String title) { mCurrencyTitle = title; }
	
	@Column(name="remark")
	private String mRemark;
	public void setRemark(String remark) { mRemark = remark; }
	
	@Index
	@Column(name="processed")
	private Boolean mProcessed;
	public void setProcessed(Boolean processed) { mProcessed = processed; }
}
