/**
 * 
 */
package ru.futurelink.cexengine.trade;

import javax.persistence.EntityManagerFactory;

/**
 * @author pavlov
 *
 */
public class TradeService {
	private EntityManagerFactory mEntityManagerFactory;

	public TradeService(EntityManagerFactory entityManagerFactory) {
		mEntityManagerFactory = entityManagerFactory;
	}

	public TradeServiceInstance CreateInstance() {
		return new TradeServiceInstance(mEntityManagerFactory);
	}
}
