/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import javax.persistence.EntityManagerFactory;

/**
 * @author pavlov
 *
 */
public class AccountingService {
	private EntityManagerFactory mEntityManagerFactory;

	public AccountingService(EntityManagerFactory entityManagerFactory) {
		mEntityManagerFactory = entityManagerFactory;
	}

	public AccountingServiceInstance CreateInstance() {
		return new AccountingServiceInstance(mEntityManagerFactory);
	}
}
