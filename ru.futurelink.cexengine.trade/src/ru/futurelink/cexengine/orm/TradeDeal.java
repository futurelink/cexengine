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
public class TradeDeal implements Serializable {
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
	@JoinColumn(name="sellerAccount", referencedColumnName = "id")
	private TradeAccount mSellerAccount;
	public TradeAccount getSellerAccount() { return mSellerAccount; }
	
	@Index
	@JoinColumn(name="buyerAccount", referencedColumnName = "id")
	private TradeAccount mBuyerAccount;
	public TradeAccount getBuyerAccount() { return mBuyerAccount; }

	@JoinColumn(name="sellerWalletCurrency1")
	private  TradeWallet mSellerWalletCurrency1;
	public void setSellerDebitWallet(TradeWallet wallet) { mSellerWalletCurrency1 = wallet; }
	public TradeWallet getSellerDebitWallet() { return mSellerWalletCurrency1; }
	
	@JoinColumn(name="buyerWalletCurrency1")
	private TradeWallet	 mBuyerWalletCurrency1;
	public void setBuyerDebitWallet(TradeWallet wallet) { mBuyerWalletCurrency1 = wallet; }
	public TradeWallet getBuyerDebitWallet() { return mBuyerWalletCurrency1; }

	@JoinColumn(name="sellerWalletCurrency2")
	private  TradeWallet mSellerWalletCurrency2;
	public void setSellerCreditWallet(TradeWallet wallet) { mSellerWalletCurrency2 = wallet; }
	public TradeWallet getSellerCreditWallet() { return mSellerWalletCurrency2; }
	
	@JoinColumn(name="buyerWalletCurrency2")
	private TradeWallet	 mBuyerWalletCurrency2;
	public void setBuyerCreditWallet(TradeWallet wallet) { mBuyerWalletCurrency2 = wallet; }
	public TradeWallet getBuyerCreditWallet() { return mBuyerWalletCurrency2; }
	
	@Index
	@JoinColumn(name="sellOrder", referencedColumnName = "id")
	private TradeOrder mSellOrder;
	public TradeOrder getSellOrder() { return mSellOrder; } 
	
	@Index
	@JoinColumn(name="buyOrder", referencedColumnName = "id")
	private TradeOrder mBuyOrder; 
	public TradeOrder getBuyOrder() { return mBuyOrder; }	

	@Index
	@JoinColumn(name="tool")
	private TradeTool mTool;
	public void setTool(TradeTool tool) { mTool = tool; }
	public TradeTool getTool() { return mTool; }

	@Column(name="amount")
	private BigDecimal mAmount;
	public BigDecimal getAmount() { return mAmount; }

	@Index
	@Column(name="price")
	private BigDecimal mPrice;
	public void setPrice(BigDecimal price) { mPrice = price; }
	public BigDecimal getPrice() { return mPrice; }

	@Index
	@Column(name="executionTime")
	@Temporal(TemporalType.TIMESTAMP)
	private Date mExecutionTime;

	@Index
	@Column(name="processed")
	private Boolean mProcessed;
	public void setProcessed(Boolean processed) { mProcessed = processed; }
	public Boolean getProcessed() { return mProcessed; }  

	@Index
	@Column(name="sellProcessed")
	private Boolean mSellProcessed;
	public void setSellProcessed(Boolean processed) { mSellProcessed = processed; if (mSellProcessed&&getBuyProcessed()) mProcessed = true; else mProcessed = false; }
	public Boolean getSellProcessed() { if (mSellProcessed == null) mSellProcessed = false; return mSellProcessed; }

	@Index
	@Column(name="buyProcessed")
	private Boolean mBuyProcessed;
	public void setBuyProcessed(Boolean processed) { mBuyProcessed = processed; if (mBuyProcessed&&getSellProcessed()) mProcessed = true; else mProcessed = false; }
	public Boolean getBuyProcessed()  { if (mBuyProcessed == null) mBuyProcessed = false; return mBuyProcessed; }
	
	@Column(name="processTime")
	@Temporal(TemporalType.TIMESTAMP)
	private Date mProcessTime;
	public void setProcessTim(Date processTime) { mProcessTime = processTime; }
	
	/**
	 * @param order
	 * @return
	 */
	public static TradeDeal createByOrders(TradeOrder sellOrder, TradeOrder buyOrder, BigDecimal amount) {
		
		// Если вдруг на ордерах разные инструменты - сделаку создать нельзя!
		if (!buyOrder.getTool().getId().equals(sellOrder.getTool().getId())) return null;
		
		TradeDeal deal = new TradeDeal();
		deal.mExecutionTime = Calendar.getInstance().getTime();
		deal.mTool = buyOrder.getTool();
		deal.mPrice = buyOrder.getPrice();

		deal.mBuyerAccount = buyOrder.getAccount();
		deal.mBuyerWalletCurrency1 = buyOrder.getDebitWallet();
		deal.mBuyerWalletCurrency2 = buyOrder.getCreditWallet();
		deal.mBuyOrder = buyOrder;

		deal.mSellerAccount = sellOrder.getAccount();
		deal.mSellerWalletCurrency1 = sellOrder.getDebitWallet();
		deal.mSellerWalletCurrency2 = sellOrder.getCreditWallet();
		deal.mSellOrder = sellOrder;

		deal.mAmount = amount;
		deal.mProcessed = false;

		return deal;
	}
}
