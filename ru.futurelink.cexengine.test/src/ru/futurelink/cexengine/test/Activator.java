package ru.futurelink.cexengine.test;

import java.util.ArrayList;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.trade.TradeService;
import ru.futurelink.cexengine.trade.TradeServiceInstance;

/*
 * 

SELECT TYPE , price, SUM( amountExecuted ) 
FROM  `TRADEORDER` 
GROUP BY TYPE , price
ORDER BY  `TRADEORDER`.`price` ASC 

 */

public class Activator implements BundleActivator {

	private TradeService	mTradeService;
	private ServiceTracker<Object, Object> mServiceTracker;

	public Activator() {}

	public void start(BundleContext context) throws Exception {
		System.out.println("Starting load test for cexengine (500 parallel threads)...");
		
		// Получаем объект сервиса из регистра
		mServiceTracker = new ServiceTracker<Object, Object>(context, TradeService.class.getName(), null);
		mServiceTracker.open();

		mTradeService = (TradeService) mServiceTracker.getService();
		if (mTradeService != null) {			
			TradeServiceInstance instance = mTradeService.CreateInstance(); 
			
			// Подождем... не знаю зачем, но если не ждать, что Eclipselink 
			// вываливает ошибку JPQL на ВЕРНЫЙ запрос...
			Thread.sleep(1000);
			
			ArrayList<TradeAccount> accounts = new ArrayList<TradeAccount>();
			for (int number = 1; number < 11; number++) {
				accounts.add(instance.GetAccount(String.valueOf(number)));
			}
			
		    // Начинаем тест, порождаем 500 параллельных потоков,
		    // в которых будут создаваться ордера. Ордера будут создаваться не просто
		    // так, хаотично, но суммы будут в определенном коридоре.
		    for (int i = 0; i < 10; i++) {
		    	TestThread sellThread = new TestThread((short) 0, mTradeService, accounts);	// На продажу
		    	TestThread buyThread = new TestThread((short) 1, mTradeService, accounts);		// На покупку
		    	sellThread.start();
		    	buyThread.start();
		    }
		}
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Stopping test for cexengine...");
	}
}