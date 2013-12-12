/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;

import ru.futurelink.cexengine.orm.TradeDeal;
import ru.futurelink.cexengine.orm.TradeTransaction;

/**
 * @author pavlov
 *
 */
public class AccountingProcessorRunnnable implements Runnable {

	private EntityManagerFactory 		mEntityManagerFactory;
	private EntityManager				mEm;
	private Logger						mLogger;
	private BigDecimal					mPrice;
	private BlockingQueue<TradeDeal>	mDeals;
	private AccountingProcessorListener mListener;
	
	@Override
	public void run() {
		if (mDeals == null) return;
		if (mListener != null) mListener.QueueExecuteStarted(mPrice);
		
		EntityTransaction 	trans = null;
		TradeDeal			deal = null;

		try {			
			mEm = mEntityManagerFactory.createEntityManager();
			trans = mEm.getTransaction();			
			while ((mDeals.size() > 0)) {
				deal = mDeals.poll();	// Берем из очереди

				// Если в очереди ничего нет - выходим
				if (deal == null) break;
			
				// Списать с кошелька продавца сумму в первой валюте пары
				// и зачислить сумму во второй валюте пары умноженную на количество.
				BigDecimal income = deal.getAmount().multiply(deal.getSellOrder().getPrice());
				BigDecimal transactionFee = new BigDecimal(0);
				BigDecimal buyerIncome = deal.getAmount().subtract(transactionFee);
							
				// Process sell transactions and mark deal as sell processed
				if (!deal.getSellProcessed()) {
					// Зачисляем деньги
					TradeTransaction tr1 = new TradeTransaction();
					tr1.setWallet(deal.getSellerDebitWallet());
					tr1.setSum(income);
					tr1.setType(TradeTransaction.TRANSACTION_MOVE);
					tr1.setOrder(deal.getSellOrder());
					tr1.setDeal(deal);
					tr1.setProcessed(false);

					// Списываем деньги
					TradeTransaction tr2 = new TradeTransaction();
					tr2.setWallet(deal.getSellerCreditWallet());
					tr2.setSum(new BigDecimal(0).subtract(deal.getAmount()));
					tr2.setType(TradeTransaction.TRANSACTION_MOVE);
					tr2.setOrder(deal.getSellOrder());
					tr2.setDeal(deal);
					tr2.setProcessed(false);
					
					// Разблокируем сумму равную списанию
					TradeTransaction tr3 = new TradeTransaction();
					tr3.setWallet(deal.getSellerCreditWallet());
					tr3.setSum(deal.getAmount());
					tr3.setType(TradeTransaction.TRANSACTION_UNBLOCK);
					tr3.setOrder(deal.getSellOrder());
					tr3.setDeal(deal);
					tr3.setProcessed(false);

					trans.begin();
					deal.setSellProcessed(true);
					mEm.persist(tr1);
					mEm.persist(tr2);
					mEm.persist(tr3);
					mEm.merge(deal);
					trans.commit();
				}

				if (!deal.getBuyProcessed()) {
					// Зачисляем покупателю деньги
					TradeTransaction tr4 = new TradeTransaction();
					tr4.setWallet(deal.getBuyerDebitWallet());
					tr4.setSum(buyerIncome);
					tr4.setType(TradeTransaction.TRANSACTION_MOVE);
					tr4.setOrder(deal.getBuyOrder());
					tr4.setDeal(deal);
					tr4.setProcessed(false);

					// Разблокируем сумму для покупателя
					TradeTransaction tr5 = new TradeTransaction();
					tr5.setWallet(deal.getBuyerCreditWallet());
					tr5.setSum(income);
					tr5.setType(TradeTransaction.TRANSACTION_UNBLOCK);
					tr5.setOrder(deal.getBuyOrder());
					tr5.setDeal(deal);
					tr5.setProcessed(false);

					// Списываем сумму покупателя
					TradeTransaction tr6 = new TradeTransaction();
					tr6.setWallet(deal.getBuyerCreditWallet());
					tr6.setSum(new BigDecimal(0).subtract(income));
					tr6.setType(TradeTransaction.TRANSACTION_MOVE);
					tr6.setOrder(deal.getBuyOrder());
					tr6.setDeal(deal);
					tr6.setProcessed(false);

					trans.begin();
					deal.setBuyProcessed(true);
					mEm.persist(tr4);
					mEm.persist(tr5);
					mEm.persist(tr6);
					mEm.merge(deal);
					trans.commit();
				}
				
				mLogger.debug("Processed DEAL {} in {}: user '{}' got {} {}, user '{}' got {} {}",
						deal.getId(),
						deal.getTool().getTitle(),
						deal.getSellerAccount().getNumber(),
						income,
						deal.getTool().getCurrency2().getTitle(),
						deal.getBuyerAccount().getNumber(),
						buyerIncome,
						deal.getTool().getCurrency1().getTitle()
						);
			}
		} catch (Exception e) {
			e.printStackTrace();

			// Роллбэчим транзакцию
			if (trans != null && trans.isActive()) {
				trans.rollback();
			}

			// Очищаем очередь, потому что все неправильно.
			// Все переносится на следующую итерацию.
			mDeals.clear();

			if (mListener != null) mListener.QueueExecuteInterrupted(mPrice);
			
			return;
		}
		
		if (mListener != null) mListener.QueueExecuteComplete(mPrice);
	}

	/**
	 * @param mEntityManagerFactory
	 */
	public void setEntityManagerFactory(
			EntityManagerFactory factory) {
		mEntityManagerFactory = factory;
	}

	/**
	 * @param price
	 */
	public void setPrice(BigDecimal price) {
		mPrice = price;
	}

	/**
	 * @param deals
	 */
	public void setDeals(BlockingQueue<TradeDeal> deals) {
		mDeals = deals;
	}

	/**
	 * @param listener
	 */
	public void setExecutionListener(AccountingProcessorListener listener) {
		mListener = listener;
	}

	public void setLogger(Logger l) {
		mLogger = l;
	}
}
