/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;

import ru.futurelink.cexengine.orm.TradeDeal;
import ru.futurelink.cexengine.orm.TradeOrder;
import ru.futurelink.cexengine.orm.TradeTool;

/**
 * Класс потока исполнителя ордеров.
 * 
 * @author pavlov
 *
 */
class ExecutorRunnable implements Runnable {
	private ConcurrentLinkedQueue<TradeOrder> 	mSellQueue;
	private ConcurrentLinkedQueue<TradeOrder> 	mBuyQueue;
	private TradeTool					mTool;
	private BigDecimal 					mPrice;
	private OrderExecutorListener 		mListener;
	private EntityManagerFactory		mEmFactory;
	private EntityManager				mEm;
	private Logger						mLogger;

	@Override
	public void run() {
		if (mListener != null) mListener.QueueExecuteStarted(mTool, mPrice);

		// Цикл обработки ордеров
		TradeOrder 			currentSellOrder = null;
		TradeOrder 			currentBuyOrder = null;
		EntityTransaction 	trans = null;
		try {
			int cmp = 0;
			mEm = mEmFactory.createEntityManager();
			trans= mEm.getTransaction();
			while ((mSellQueue.size() > 0) && (mBuyQueue.size() > 0)) {
				// Берем следующий ордер из нужной очереди, если есть доступные.
				if (cmp == 0) {
					// Если у нас умолчание - берем оба ордера
					currentSellOrder = mSellQueue.poll();
					currentBuyOrder = mBuyQueue.poll();
				} else if (cmp == 1) {
					//Если в процессе кончился ордер на покупку - берем только его
					currentBuyOrder = mBuyQueue.poll();
				} else {
					// Если коничлся оржер на продажу - берем его
					currentSellOrder = mSellQueue.poll();
				}

				if ((currentBuyOrder == null) || (currentSellOrder == null)) break;
								
				if (mLogger != null)
					mLogger.debug("Processing order pair {} ({}) and {} ({})",
						 currentSellOrder.getId(),
						 currentSellOrder.getAmountFree(),
						 currentBuyOrder.getId(),
						 currentBuyOrder.getAmountFree());

				// Создаем сделку
				BigDecimal dealSum = null;
				cmp = currentSellOrder.getAmountFree().compareTo(currentBuyOrder.getAmountFree());

				if (cmp == 0) {
					dealSum = currentBuyOrder.getAmountFree();
					currentBuyOrder.setAmountExecuted(dealSum.add(currentBuyOrder.getAmountExecuted()));
					currentSellOrder.setAmountExecuted(dealSum.add(currentSellOrder.getAmountExecuted()));				
					currentBuyOrder.setExecuted();
					currentSellOrder.setExecuted();
					if (mLogger != null)
						mLogger.debug("BUY order {} and SELL order {} were completely executed.", currentBuyOrder.getId(), currentSellOrder.getId());
				} else if (cmp == 1) {
					// В ордере на продажу меньше свободных средств чем в ордере на покупку
					dealSum = currentBuyOrder.getAmountFree();
					currentSellOrder.setAmountExecuted(dealSum.add(currentSellOrder.getAmountExecuted()));
					currentBuyOrder.setAmountExecuted(dealSum.add(currentBuyOrder.getAmountExecuted()));
					currentBuyOrder.setExecuted();
					if (mLogger != null)
						mLogger.debug("BUY order {} was completely executed.", currentBuyOrder.getId());
				} else if (cmp == -1) {
					// В ордере на покупку больше свободных средств
					dealSum = currentSellOrder.getAmountFree();
					currentBuyOrder.setAmountExecuted(dealSum.add(currentBuyOrder.getAmountExecuted()));
					currentSellOrder.setAmountExecuted(dealSum.add(currentSellOrder.getAmountExecuted()));
					currentSellOrder.setExecuted();
					if (mLogger != null)
						mLogger.debug("SELL order {} was completely executed.", currentBuyOrder.getId());
				}

				TradeDeal deal = TradeDeal.createByOrders(currentSellOrder, currentBuyOrder, dealSum);

				// Процессим ордер, одна транзакция на ордер
				trans.begin();	
				mEm.persist(deal);
				mEm.merge(currentSellOrder);
				mEm.merge(currentBuyOrder);
				trans.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			// Роллбэчим транзакцию
			if (trans != null) {
				trans.rollback();
			}

			// Выполнение треда прервано, обработать.
			if (mListener != null) mListener.QueueExecuteInterrupted(mTool, mPrice);
		}

		// Очищаем обе очереди, потому что все неправильно.
		// Все переносится на следующую итерацию.
		mSellQueue.clear();
		mBuyQueue.clear();

		if (mListener != null) mListener.QueueExecuteComplete(mTool, mPrice);
	}

	public void setLogger(Logger l) {
		mLogger = l;
	}
	
	/**
	 * @param tool
	 */
	public void setTool(TradeTool tool) { mTool = tool; }

	/**
	 * @param createEntityManagerFactory
	 */
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) { mEmFactory = entityManagerFactory; }

	/**
	 * @param mExecutionListener
	 */
	public void setExecutionListener(OrderExecutorListener executionListener) {
		mListener = executionListener;
	}

	protected void setPrice(BigDecimal price) { mPrice = price; } 		
	protected void setSellQueue(ConcurrentLinkedQueue<TradeOrder> queue) { mSellQueue = queue; }
	protected void setBuyQueue(ConcurrentLinkedQueue<TradeOrder> queue) { mBuyQueue = queue; }
}
