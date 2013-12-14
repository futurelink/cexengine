/**
 * 
 */
package ru.futurelink.cexengine.consistency;

import javax.persistence.EntityManagerFactory;

import ru.futurelink.cexengine.CEXService;

/**
 * Data consistency testing and fixing  service.
 * 
 * @author pavlov
 *
 */
public class ConsistencyService extends CEXService {

	public ConsistencyService(EntityManagerFactory entityManagerFactory) {
		super(entityManagerFactory);
	}

	public ConsistencyServiceInstance CreateInstance() {
		return new ConsistencyServiceInstance(mEntityManagerFactory);
	}
}
