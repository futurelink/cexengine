/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import javax.persistence.EntityManagerFactory;

import ru.futurelink.cexengine.CEXService;

/**
 * @author pavlov
 *
 */
public class AccountingService extends CEXService {
	public AccountingService(EntityManagerFactory entityManagerFactory) {
		super(entityManagerFactory);
	}

	public AccountingServiceInstance CreateInstance() {
		return new AccountingServiceInstance(mEntityManagerFactory);
	}
}
