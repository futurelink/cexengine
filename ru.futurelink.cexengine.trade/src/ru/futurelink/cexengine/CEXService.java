/**
 * 
 */
package ru.futurelink.cexengine;

import javax.persistence.EntityManagerFactory;

/**
 * @author pavlov
 *
 */
public abstract class CEXService {
	protected EntityManagerFactory mEntityManagerFactory;

	public CEXService(EntityManagerFactory entityManagerFactory) {
		mEntityManagerFactory = entityManagerFactory;
	}
}
