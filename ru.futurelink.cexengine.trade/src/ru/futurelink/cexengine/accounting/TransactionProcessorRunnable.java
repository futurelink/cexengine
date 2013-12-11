/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.lang.management.MemoryType;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.config.ResultSetType;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.Cursor;
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
	private TransactionProcessorListener mListener;

	@Override
	public void run() {
		if (mListener != null) {
			if (!mListener.QueueExecuteStarted(mWallet)) return;
		}
		
		mEm = mEntityManagerFactory.createEntityManager();			
		mLogger.info("Getting transaction for wallet: {}", mWallet.getId());
	
		TypedQuery<TradeTransaction> dq = mEm.createQuery("select trans from TradeTransaction trans where "
				+ "trans.mWallet = :wallet and trans.mProcessed = 0 order by trans.mId asc", TradeTransaction.class);
		dq.setParameter("wallet", mWallet);

		// Iterate through result list
		JpaQuery<TradeTransaction> jQuery = (JpaQuery<TradeTransaction>) dq;
	    jQuery.setHint(QueryHints.RESULT_SET_TYPE, ResultSetType.ForwardOnly)
	       .setHint(QueryHints.SCROLLABLE_CURSOR, true);
	    final Cursor cursor = jQuery.getResultCursor();
	    Iterable<TradeTransaction> iterator = new Iterable<TradeTransaction>() {			
			@SuppressWarnings("unchecked")
			@Override
			public Iterator<TradeTransaction> iterator() {
				return cursor;
			}
		};
		
		mLogger.info("Processing transaction for wallet: {}", mWallet.getId());
		
		//if ((mTransactions != null) && !mTransactions.isEmpty()) { 			
			EntityTransaction 	trans = null;
			int counter = 0;
			try {
				trans = mEm.getTransaction();
				trans.begin();
				//mWallet = mEm.merge(mWallet);
				//mEm.lock(mWallet, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
				for (TradeTransaction transaction : iterator) {
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

					counter++;					
					if (counter > 1000) break;
				}

				// Merge wallet updates data and returns new managesd object of wallet.
				mEm.merge(mWallet);
				trans.commit();
				
				((Cursor)iterator.iterator()).close();
				
				mLogger.info("Processed transaction for wallet ({} transactions processed): {}", counter, mWallet.getId());
			} catch (Exception e) {
				e.printStackTrace();

				// Ooops!...
				if (trans != null && trans.isActive()) {
					trans.rollback();
				}

				// Clear queue, free memory,
				// everything is moving to next iteration...
				//mTransactions.clear();

				if (mListener != null) mListener.QueueExecuteInterrupted(mWallet);
			
				return;			
			}
		//}
		
		mEm.close();
		
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
