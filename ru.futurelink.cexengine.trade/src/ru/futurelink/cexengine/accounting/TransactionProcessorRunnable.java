/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;

import ru.futurelink.cexengine.orm.TradeTransaction;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public class TransactionProcessorRunnable implements Runnable {

	private EntityManagerFactory 		mEntityManagerFactory;
	private EntityManager				mEm;
	private Logger						mLogger;
	private TradeWallet					mWallet;
	private ConcurrentLinkedQueue<TradeTransaction> mTransactions;
	private TransactionProcessorListener mListener;

	@Override
	public void run() {
		if (mTransactions == null) return;
		if (mListener != null) mListener.QueueExecuteStarted(mWallet);
		
		EntityTransaction 	trans = null;
		TradeTransaction	transaction = null;
		try {			
			mEm = mEntityManagerFactory.createEntityManager();
			trans = mEm.getTransaction();
			trans.begin();
			while ((mTransactions.size() > 0)) {
				transaction = mTransactions.poll();	// Берем из очереди

				// If there is nothing in queue - quit
				if (transaction == null) break;

				// Calculate all transaction balance changes
				if (transaction.getType() == TradeTransaction.TRANSACTION_BLOCK) {
					mWallet.blockSum(transaction.getSum());
					transaction.setProcessed(true);
				} else if (transaction.getType() == TradeTransaction.TRANSACTION_UNBLOCK) {
					mWallet.unblockSum(transaction.getSum());
					transaction.setProcessed(true);
				} else if (transaction.getType() == TradeTransaction.TRANSACTION_MOVE) {
					// Transaction sum may be negative or positive, so just add sum to wallet
					mWallet.addSum(transaction.getSum());
					transaction.setProcessed(true);
				}

				mEm.merge(transaction);				
				mLogger.debug("Transaction {} processed", transaction.getId());
			}
			
			// Merge wallet updates data and returns new managesd object of wallet.
			mEm.merge(mWallet);
			trans.commit();
		} catch (Exception e) {
			e.printStackTrace();

			// Ooops!...
			if (trans != null && trans.isActive()) {
				trans.rollback();
			}

			// Clear queue, free memory,
			// everything is moving to next iteration...
			mTransactions.clear();

			if (mListener != null) mListener.QueueExecuteInterrupted(mWallet);
			
			return;			
		}
		
		if (mListener != null) mListener.QueueExecuteComplete(mWallet);
	}

	/**
	 * @param factory
	 */
	public void setEntityManagerFactory(
			EntityManagerFactory factory) {
		mEntityManagerFactory = factory;
	}

	/**
	 * @param wallet
	 */
	public void setWallet(TradeWallet wallet) {
		mWallet = wallet;
	}

	/**
	 * @param trans
	 */
	public void setTransactions(ConcurrentLinkedQueue<TradeTransaction> trans) {
		mTransactions = trans;
	}

	/**
	 * @param listener
	 */
	public void setExecutionListener(TransactionProcessorListener listener) {
		mListener = listener;
	}

	/**
	 * Sets logger.
	 * 
	 * @param l
	 */
	public void setLogger(Logger l) {
		mLogger = l;
	}	
}
