package ru.futurelink.cexengine;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ru.futurelink.cexengine.accounting.AccountingService;
import ru.futurelink.cexengine.accounting.IAccountingService;
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

	public Activator() {}

	public void start(BundleContext context) throws Exception {
		System.out.println("Initializing database engine...");
		mFactory = Persistence.createEntityManagerFactory("cex");

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
		
		// Создаем объект сервиса и регистрируем его
		mTradeService = new TradeService(mFactory);

		mTradeServiceInstance = mTradeService.CreateInstance();
		mTradeServiceInstance.StartExecution();
		
		if(mTradeServiceRegistration == null){
			mTradeServiceRegistration = context.registerService(
					TradeService.class.getName(), mTradeService, null);
		}
		
		// Создаем и регистируем сервис учета
		mAccountingService = new AccountingService(mFactory);

		mAccountingServiceInstance = mAccountingService.CreateInstance();
		mAccountingServiceInstance.StartProcessing();

		if(mAccountingServiceRegistration == null){
			mAccountingServiceRegistration = context.registerService(
					AccountingService.class.getName(), mAccountingService, null);
		}		
	}

	public void stop(BundleContext context) throws Exception {
		mTradeServiceRegistration.unregister();
		mTradeServiceInstance.StopExecution();
		mTradeServiceInstance = null;
		mTradeService = null;
		
		mAccountingServiceRegistration.unregister();
		mAccountingService = null;

		mFactory.close();
		mFactory = null;
	}

}
