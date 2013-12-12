/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.config.ResultSetType;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.Cursor;
import org.slf4j.Logger;

import com.mysql.jdbc.exceptions.MySQLTransactionRollbackException;

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
	private String						mWalletId;
	private TransactionProcessorListener mListener;

	static final int MAX_TRANSACTIONS = 100;
	
	@Override
	public void run() {
		if (mListener != null) {
			if (!mListener.QueueExecuteStarted(mWalletId)) return;
		}
		
		mEm = mEntityManagerFactory.createEntityManager();
		mWallet = mEm.find(TradeWallet.class, mWalletId);
		
		mLogger.debug("Getting transaction for wallet: {}", mWallet.getId());

		TypedQuery<TradeTransaction> dq = mEm.createQuery(
				"select trans from TradeTransaction trans where "
				+ "trans.mWallet = :wallet and trans.mProcessed = 0 "
				+ "order by trans.mId asc", TradeTransaction.class);
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
		
		mLogger.debug("Processing transaction for wallet: {}", mWallet.getId());
				
		int 				counter = 0;
		EntityTransaction 	trans = null;
		try {
			int retries = 0;
			int errCode = -1;
			while ((retries < 3) && (errCode != 0)) {
				try {
					trans = mEm.getTransaction();
					trans.begin();

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
						if (counter > MAX_TRANSACTIONS) break;
					}
			
					// Merge wallet updates data and returns new managesd object of wallet.
					mEm.persist(mWallet);

					trans.commit();
					errCode = 0;
				} catch(RollbackException e) {
					// MySQL INNODB deadlock found...
					Throwable cause = e.getCause();
					if (cause instanceof DatabaseException) {
						errCode = ((DatabaseException)cause).getDatabaseErrorCode();
						if (errCode == 1213) {
							mLogger.warn("INNODB detected deadlock. Retrying transaction.");
							Thread.sleep(10);
						}
						retries++;
					} else {
						// DO NOT REPEAT!
						retries = 3;
					}
				}
			}

			((Cursor)iterator.iterator()).close();
				
			mLogger.debug("Processed transaction for wallet ({} transactions processed): {}", counter, mWallet.getId());
		} catch (Exception e) {
			e.printStackTrace();

			// Ooops!...
			if (trans != null && trans.isActive()) {
				trans.rollback();
			}

			if (mListener != null) mListener.QueueExecuteInterrupted(mWalletId);
			
			return;			
		}
		
		mEm.close();
		
		if (mListener != null) mListener.QueueExecuteComplete(mWalletId);
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

	public void setWalletId(String walletId) {
		mWalletId = walletId;
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
