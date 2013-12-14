/**
 * 
 */
package ru.futurelink.cexengine.trade;

import javax.persistence.EntityManagerFactory;

import ru.futurelink.cexengine.CEXService;

/**
 * @author pavlov
 *
 */
public class TradeService extends CEXService {
	public TradeService(EntityManagerFactory entityManagerFactory) {
		super(entityManagerFactory);
	}

	public TradeServiceInstance CreateInstance() {
		return new TradeServiceInstance(mEntityManagerFactory);
	}
}
