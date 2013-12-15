/**
 * 
 */
package ru.futurelink.cexengine.consistency;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test set to check database consistency.
 * It conatins tests to check:
 * - orders processed correctly (sum of all executed amounts on orderes is a half of all sum on created deals) 
 * - there is no orders marked as executed and executed amount on them is not equals to order amount
 * - there is no orders marked as executed and no deals created on them
 * - 
 * 
 * @author pavlov
 *
 */
public class ConsistencyServiceInstance {
	private EntityManagerFactory	mEntityManagerFactory;
	private EntityManager			mEntityManager; 
	private Logger					mLogger;

	public ConsistencyServiceInstance(EntityManagerFactory factory) {
		mEntityManagerFactory = factory;
		mEntityManager = mEntityManagerFactory.createEntityManager();
		mLogger = LoggerFactory.getLogger(getClass().getName());
	}
	
	public boolean RunTest() {
		mLogger.info("Running tests for consistency checking...");
		
		/*
		 * Test 1
		 */
		// Total executed amount
		Query q = mEntityManager.createNativeQuery(
				"SELECT SUM(amountexecuted) FROM  `TRADEORDER` WHERE amountexecuted > 0");		
		// Deals amout
		Query q2 = mEntityManager.createNativeQuery("SELECT sum(amount) FROM `TRADEDEAL`");
		BigDecimal amountExecuted = (BigDecimal) q.getSingleResult();
		BigDecimal amountOnDeals = (BigDecimal) q2.getSingleResult();
		if ((amountOnDeals == null) || (amountExecuted == null) || amountOnDeals.equals(new BigDecimal(0))) {
			mLogger.info("No deals processed, skipping all tests. Database is empty.");
			return true;
		}
		if (!amountExecuted.divide(new BigDecimal(2)).equals(amountOnDeals)) {
			mLogger.error("Problem with test 1: deals sum is not a half of all processed orders!");
			return false; 
		} else {
			mLogger.info("Test 1 passed.");
		}
		
		/*
		 * Test 2
		 */		
		q = mEntityManager.createNativeQuery(
				"SELECT COUNT(*) FROM `TRADEORDER` WHERE amountexecuted <> amount AND executed = 1");
		Long test2Result = (Long) q.getSingleResult();
		if (test2Result != 0) {
			mLogger.error("Problem with test 2: there are orders marked as executed but executed amount is not equal to order amount!");
			return false;
		} else {
			mLogger.info("Test 2 passed.");
		}

		/*
		 * Test 4
		 */		
		q = mEntityManager.createNativeQuery(
				"SELECT COUNT(*) FROM `TRADETRANSACTION` WHERE transactionType = 0");
		Long blockingTransactionsCount = (Long) q.getSingleResult();

		q = mEntityManager.createNativeQuery(
				"SELECT COUNT(*) FROM `TRADEORDER`");
		Long ordersCount = (Long) q.getSingleResult();
		if (!ordersCount.equals(blockingTransactionsCount)) {
			mLogger.error("Problem with test 4: blocked transactions count ({}) is not equal to order count ({})", blockingTransactionsCount, ordersCount);
			return false;
		}

		q = mEntityManager.createNativeQuery(
				"SELECT COUNT(*) / 2 FROM `TRADETRANSACTION` WHERE transactionType = 1");
		BigDecimal unblockingTransactionsCount = (BigDecimal) q.getSingleResult();		

		q = mEntityManager.createNativeQuery(
				"SELECT COUNT(*) / 1 FROM `TRADEDEAL` WHERE buyProcessed = 1 OR sellProcessed = 1");
		BigDecimal dealsCount = (BigDecimal) q.getSingleResult();
		if (!dealsCount.equals(unblockingTransactionsCount)) {
			mLogger.error("Problem with test 4: unblocking transactions count / 2 ({}) is not equal to deal count ({})", unblockingTransactionsCount, dealsCount);
			return false;
		}

		mLogger.info("Test 4 passed.");
		
		return true;
	}
}
