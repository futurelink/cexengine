package ru.futurelink.cexengine;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.futurelink.cexengine.accounting.AccountingService;
import ru.futurelink.cexengine.accounting.IAccountingService;
import ru.futurelink.cexengine.consistency.ConsistencyService;
import ru.futurelink.cexengine.consistency.ConsistencyServiceInstance;
import ru.futurelink.cexengine.trade.ITradeService;
import ru.futurelink.cexengine.trade.TradeService;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("rawtypes")
public class Activator implements BundleActivator {	
	private EntityManagerFactory 	mFactory;
	private TradeService			mTradeService;
	private ServiceRegistration		mTradeServiceRegistration;
	private ITradeService			mTradeServiceInstance;

	private AccountingService		mAccountingService;
	private ServiceRegistration		mAccountingServiceRegistration;
	private IAccountingService		mAccountingServiceInstance;

	private ConsistencyService		mConsistencyService;
	private ServiceRegistration		mConsistencyServiceRegistration;
	
	private Logger					mLogger;
	public BundleContext			mContext;
	
	public Activator() {}

	public void start(BundleContext context) throws Exception {		
		mLogger = LoggerFactory.getLogger(getClass().getName());
		
		mLogger.info("Initializing CEX engine...");
		mFactory = Persistence.createEntityManagerFactory("cex");
		mContext = context;
		
		@SuppressWarnings("unused")
		EntityManager m = mFactory.createEntityManager();

		/*TradeCurrency c1 = new TradeCurrency();
		c1.setTitle("USD");

		TradeCurrency c2 = new TradeCurrency();
		c2.setTitle("RUR");

		TradeTool tool = new TradeTool();
		tool.setCurrency1(c1);
		tool.setCurrency2(c2);
		tool.setTitle("USD/RUR");

		m.getTransaction().begin();
		m.persist(c1);
		m.persist(c2);
		m.persist(tool);
		m.getTransaction().commit();*/
		
		// Создадим 10 акков
		/*for (int i = 1; i < 11; i++) {			
			TradeAccount acc = new TradeAccount();
			acc.setNumber(String.valueOf(i));
			
			m.getTransaction().begin();
			m.persist(acc);
			m.getTransaction().commit();			
		}*/

		// Create and register accounting service
		mConsistencyService = new ConsistencyService(mFactory);
		if(mConsistencyServiceRegistration == null){
			mConsistencyServiceRegistration = context.registerService(
					ConsistencyService.class.getName(), mConsistencyService, null);

			if (mConsistencyService != null) {
				ConsistencyServiceInstance instance = mConsistencyService.CreateInstance();
				if (!instance.RunTest())
					return;
			}
		}
				
		// Создаем объект сервиса и регистрируем его
		mTradeService = new TradeService(mFactory);
		mTradeServiceInstance = mTradeService.CreateInstance();
		mTradeServiceInstance.StartExecution();
		
		if(mTradeServiceRegistration == null){
			mTradeServiceRegistration = context.registerService(
					TradeService.class.getName(), mTradeService, null);
		}
		
		// Create and register accounting service
		mAccountingService = new AccountingService(mFactory);
		mAccountingServiceInstance = mAccountingService.CreateInstance();
		mAccountingServiceInstance.StartProcessing();

		if(mAccountingServiceRegistration == null){
			mAccountingServiceRegistration = context.registerService(
					AccountingService.class.getName(), mAccountingService, null);
		}				
	}

	public void stop(BundleContext context) throws Exception {
		mLogger.info("Stopping CEX engine...");
		
		mTradeServiceRegistration.unregister();
		mTradeServiceInstance.StopExecution();
		mTradeServiceInstance = null;
		mTradeService = null;
		
		mAccountingServiceRegistration.unregister();
		mAccountingServiceInstance.StopProcessing();
		mAccountingService = null;

		mConsistencyServiceRegistration.unregister();
		mConsistencyService = null;
		
		mFactory.close();
		mFactory = null;
	}
}
