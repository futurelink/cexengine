/**
 * 
 */
package ru.futurelink.cexengine.orm;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.Index;

/**
 * @author pavlov
 *
 */
@Entity
public class TradeOrder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public TradeOrder() {
		mAmount = new BigDecimal(0.0);
		mExecuted = false;
		mAmountExecuted = new BigDecimal(0.0);
	}
	
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
	@JoinColumn(name="tool", referencedColumnName = "id")
	private TradeTool mTool;
	public void setTool(TradeTool tool) { mTool = tool; }
	public TradeTool getTool() { return mTool; }

	@Index
	@JoinColumn(name="account", referencedColumnName = "id")
	private TradeAccount mAccount;
	public void setAccount(TradeAccount account) { mAccount = account; }
	public TradeAccount getAccount() { return mAccount; }
	
	@Index
	@JoinColumn(name="walletCurrency1")
	private TradeWallet mWalletCurrency1;
	public void setWalletCurrency1(TradeWallet wallet) { mWalletCurrency1 = wallet; }
	public TradeWallet getWalletCurrency1() { return mWalletCurrency1; }

	@Index
	@JoinColumn(name="walletCurrency2")
	private TradeWallet mWalletCurrency2;
	public void setWalletCurrency2(TradeWallet wallet) { mWalletCurrency2 = wallet; }
	public TradeWallet getWalletCurrency2() { return mWalletCurrency2; }
	
	@Column(name="amount")
	private BigDecimal mAmount;
	public void setAmount(BigDecimal amount) { mAmount = amount; }
	public BigDecimal getAmount() { return mAmount; }

	@Column(name="amountExecuted")
	private BigDecimal mAmountExecuted;
	public void setAmountExecuted(BigDecimal amount) { mAmountExecuted = amount; }
	public BigDecimal getAmountExecuted() { return mAmountExecuted; }
	
	public BigDecimal getAmountFree() {
		return mAmount.subtract(mAmountExecuted);
	}
	
	@Index
	@Column(name="type")
	private Short mOrderType;
	public Short getType() { return mOrderType; }
	public void setType(Short type) { mOrderType = type; }  
	
	@Index
	@Column(name="price")
	private BigDecimal mPrice;
	public void setPrice(BigDecimal price) { mPrice = price; }
	public BigDecimal getPrice() { return mPrice; }

	@Index
	@Column(name="placedTime")
	@Temporal(TemporalType.TIMESTAMP)
	private Date mPlacedTime;
	public void setPlacedTime(Date time) { mPlacedTime = time; }

	@Index
	@Column(name="executionTime")
	@Temporal(TemporalType.TIMESTAMP)
	private Date mExecutionTime;

	@Index
	@Column(name="executed")
	private Boolean mExecuted;
	public void setExecuted() { 
		mExecuted = true;
		mExecutionTime = Calendar.getInstance().getTime();
	}
	
	@Index
	@Column(name="active")
	private Boolean mActive;
	public void setActive(Boolean active) { mActive = active; }
}
