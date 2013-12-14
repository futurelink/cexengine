package ru.futurelink.cexengine.test;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import ru.futurelink.cexengine.accounting.AccountingService;
import ru.futurelink.cexengine.accounting.IAccountingService;
import ru.futurelink.cexengine.consistency.ConsistencyService;
import ru.futurelink.cexengine.consistency.ConsistencyServiceInstance;
import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeWallet;
import ru.futurelink.cexengine.trade.ITradeService;
import ru.futurelink.cexengine.trade.TradeService;

/*
 * 

SELECT TYPE , price, SUM( amountExecuted ) 
FROM  `TRADEORDER` 
GROUP BY TYPE , price
ORDER BY  `TRADEORDER`.`price` ASC 

 */

public class Activator implements BundleActivator {

	private TradeService		mTradeService;
	private AccountingService 	mAccountingService;
	private ConsistencyService mConsistencyService;

	private ServiceTracker<Object, Object> mServiceTracker;

	public Activator() {}

	public void start(BundleContext context) throws Exception {
		System.out.println("Starting load test for cexengine (100 parallel threads)...");
		
		// Get TradeService from registry
		mServiceTracker = new ServiceTracker<Object, Object>(context, TradeService.class.getName(), null);
		mServiceTracker.open();
		mTradeService = (TradeService) mServiceTracker.getService();
		mServiceTracker.close();

		// Get AccountingService from registry
		mServiceTracker = new ServiceTracker<Object, Object>(context, AccountingService.class.getName(), null);
		mServiceTracker.open();
		mAccountingService = (AccountingService) mServiceTracker.getService();
		mServiceTracker.close();
	
		// Consistency checker service
		mServiceTracker = new ServiceTracker<Object, Object>(context, ConsistencyService.class.getName(), null);
		mServiceTracker.open();
		mConsistencyService = (ConsistencyService) mServiceTracker.getService();
		mServiceTracker.close();

		if (mConsistencyService != null) {
			ConsistencyServiceInstance instance = mConsistencyService.CreateInstance();
			if (!instance.RunTest())
				return;
		}
	
		if ((mTradeService != null) && (mAccountingService != null)) {			
			ITradeService instance = mTradeService.CreateInstance();
			IAccountingService accountingInstance = mAccountingService.CreateInstance(); 
			
			// Подождем... не знаю зачем, но если не ждать, что Eclipselink 
			// вываливает ошибку JPQL на ВЕРНЫЙ запрос...
			Thread.sleep(1000);
			
			ArrayList<TradeAccount> accounts = new ArrayList<TradeAccount>();
			for (int number = 1; number < 11; number++) {
				TradeAccount acc = instance.GetAccount(String.valueOf(number));
				accounts.add(acc);

				// Зачислим на все кошельки всех акков по 1000000 единиц валюты
				/*TradeWallet wallets[] = instance.GetWallets(acc);
				for (TradeWallet w : wallets) {
					accountingInstance.PutIntoWallet(w, new BigDecimal(1000000));
				}*/
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
	
	/**
	 * Test state of financial data in database.
	 */
	public void testDataState() {
		
	}
}