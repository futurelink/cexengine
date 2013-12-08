/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.eclipse.persistence.annotations.Index;

/**
 * @author pavlov
 *
 *
 */
@Entity
public class TradeWallet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * ID объекта
	 */
	@Id
	@GeneratedValue(generator="system-uuid")
	@Column(name = "id", columnDefinition="VARCHAR(64)", nullable=false)
	private		String mId;	
	public 		String getId() {	return mId;	}
	public		void setId(String id) { mId = id; }
	
	@Index
	@JoinColumn(name="account")
	private TradeAccount mAccount;
	public void setAccount(TradeAccount acc) { mAccount = acc; }
	
	@Index
	@JoinColumn(name="currency")
	private TradeCurrency mCurrency;
	public void setCurrency(TradeCurrency cur) { mCurrency = cur; mCurrencyTitle = cur.getTitle(); }

	@Column(name="currencyTitle")
	private String mCurrencyTitle;
	public String getCurrencyTitle() { return mCurrencyTitle; }
	
	@Column(name="balance")
	private BigDecimal mBalance;
	public BigDecimal getBalance() {
		if (mBalance == null) mBalance = new BigDecimal(0);
		return mBalance; 
	}  

	@Column(name="blockedBalance")
	private BigDecimal mBlockedBalance;
	public BigDecimal getBlockedBalance()  {
		if (mBlockedBalance == null) mBlockedBalance = new BigDecimal(0);
		return mBlockedBalance; 
	}

	@Column(name="unblockedBalance")
	private BigDecimal mUnblockedBalance;
	public BigDecimal getUnblockedBalance()  {
		if (mUnblockedBalance == null) mUnblockedBalance = new BigDecimal(0);
		return mUnblockedBalance; 
	}
	
	public BigDecimal getFreeBalance() { return mBalance.subtract(mBlockedBalance); } 
	
	/**
	 * @param sum
	 */
	public void addSum(BigDecimal sum) {
		mBalance = getBalance().add(sum);
	}
	/**
	 * @param sum
	 */
	public void subtractSum(BigDecimal sum) {
		mBalance = getBalance().subtract(sum);
	}
	
	/**
	 * Заблокировать сумму на кошельке.
	 * 
	 * @param sum
	 */
	public void blockSum(BigDecimal sum) {
		mBlockedBalance = getBlockedBalance().add(sum);
	}
	
	/**
	 * Разблокировать сумму на кошельке.
	 * 
	 * @param sum
	 */
	public void unblockSum(BigDecimal sum) {
		mUnblockedBalance = getUnblockedBalance().add(sum);
	}
}
