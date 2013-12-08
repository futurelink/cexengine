/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.futurelink.cexengine.orm.TradeDeal;
import ru.futurelink.cexengine.orm.TradeTool;

/**
 * Процессор сделок по ордерам. Синглтон.
 * 
 * @author pavlov
 *
 */
public class AccountingProcessor {
	private Logger 					mLogger;
	private EntityManagerFactory	mEntityManagerFactory;
	private EntityManager			mEm;
	private Thread					mProcessorThread;
	
	// Блокировка очереди выполнения сделок
	private ConcurrentHashMap<String, Boolean> mProcessingThreadMutex; 
	
	private ArrayList<TradeTool>	mTools;
	
	// Сколько времени ждать между опросами, мс
	public static final int 		SLEEPTIME = 500;
	
	private AccountingProcessor() {
		mLogger = LoggerFactory.getLogger(getClass());
		mLogger.info("Accounting processor instantiated");
	}

	private static class AccountingProcessorHolder {
		public static final AccountingProcessor instance = new AccountingProcessor();
	}

	/**
	 * Получить экземпляр исполнителя.
	 */
	public static AccountingProcessor getInstance() {
		return AccountingProcessorHolder.instance;
	}
	
	public void init(EntityManagerFactory factory) {
		mEntityManagerFactory = factory;
		mEm = mEntityManagerFactory.createEntityManager();
		mProcessingThreadMutex = new ConcurrentHashMap<String, Boolean>();
		mTools = new ArrayList<TradeTool>();
	}
	
	public void startProcessing() {
		if (mProcessorThread == null) {
			// Загружаем инструменты
			mTools.clear();
			EntityManager em = mEntityManagerFactory.createEntityManager();
			TypedQuery<TradeTool> toolQuery = em.createQuery("select tool from TradeTool tool", TradeTool.class);
			if (!toolQuery.getResultList().isEmpty()) {
				mTools.addAll(toolQuery.getResultList());
			}
			
			// Создаем поток процессинга
			mProcessorThread = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {
							if (mTools.size() > 0) {
								// Получаем список необработаных сделок сгруппированный по инстирументу 
								ConcurrentHashMap<String, BlockingQueue<TradeDeal>> deals = getDealsToProcess();
							
								for (TradeTool tool : mTools) {
									// Пораждаем процесс обработки и начинаем все обрабатывать
									if (!processingThreadBlocked(tool.getId())) {
										runProcessingThread(deals.get(tool.getId()), tool);									
									} else {
										mLogger.debug("Accounting transaction thread is processing... skipping.");										
									}
								}
							}

							sleep(SLEEPTIME);
						} catch (InterruptedException e) {
							// Выполнение прервано...
							e.printStackTrace();
						}
					}
				}
			};

			mProcessorThread.start();
		}
	}
	
	public void stopProcessing() {
		mProcessorThread.interrupt();
	}
	

	private void runProcessingThread(BlockingQueue<TradeDeal> deals, TradeTool tool) {
		AccountingProcessorListener mExecutionListener = new AccountingProcessorListener() {
			@Override
			public void QueueExecuteStarted(TradeTool tool) {
				processingThreadBlock(tool.getId(), true);
				mLogger.debug("Accounting processing thread on tool = {} started...", tool.getTitle());
			}
			
			@Override
			public void QueueExecuteInterrupted(TradeTool tool) {		
				processingThreadBlock(tool.getId(), false);
				mLogger.info("Accounting processing thread interrupted on tool = {}...", tool.getTitle());
			}

			@Override
			public void QueueExecuteComplete(TradeTool tool) {
				processingThreadBlock(tool.getId(), false);
				mLogger.debug("Accounting processing on tool = {} completed...", tool.getTitle());
			}
		};

		mLogger.debug("Starting dela processing on tool '{}':", tool.getTitle());

		// Создаем поток исполнителя
		AccountingProcessorRunnnable runnable = new AccountingProcessorRunnnable();
		runnable.setLogger(mLogger);
		runnable.setEntityManagerFactory(mEntityManagerFactory);
		runnable.setDeals(deals);
		runnable.setTool(tool);
		runnable.setExecutionListener(mExecutionListener);

		// Запускаем обработку ордеров
		Thread thread = new Thread(runnable);
		thread.start();		
	}
	
	/**
	 * Заблокировать-разблокировать поток исполнения ордеров.
	 * Thread-Safe.
	 *
	 * @param tool
	 * @param price
	 * @param b
	 */
	protected void processingThreadBlock(String toolId, boolean blocked) {
		mProcessingThreadMutex.put(toolId, blocked);
	}

	/**
	 * Проверить, не заблокирована ли обработка данного набора "инструмент-цена"?
	 * Thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 */
	protected boolean processingThreadBlocked(String toolId) {
		if (mProcessingThreadMutex.containsKey(toolId))
				return mProcessingThreadMutex.get(toolId).booleanValue();
		return false;
	}
	
	private synchronized ConcurrentHashMap<String, BlockingQueue<TradeDeal>> 
		getDealsToProcess() {
		ConcurrentHashMap<String, BlockingQueue<TradeDeal>> deals = 
				new ConcurrentHashMap<String, BlockingQueue<TradeDeal>>();

		// Определяем по каким инструментам есть необработанные сделки
		TypedQuery<TradeTool> tq = mEm.createQuery("select deal.mTool from TradeDeal deal where "
				+ "deal.mProcessed = 0 group by deal.mTool having count(deal) > 0", TradeTool.class);
		if (!tq.getResultList().isEmpty()) {
			for (TradeTool tool : tq.getResultList()) {
				// Прорверяем на предмет блокировки обработки
				if (processingThreadBlocked(tool.getId())) continue;
				
				// Если очередь не заблокирована, то она пустая или обработана - и поэтому пустая.
				if (deals.get(tool.getId()) == null)
					deals.put(tool.getId(), new LinkedBlockingQueue<TradeDeal>());

				TypedQuery<TradeDeal> dq = mEm.createQuery("select deal from TradeDeal deal where "
						+ "deal.mTool = :tool and deal.mProcessed = 0 order by deal.mExecutionTime asc", TradeDeal.class);
				dq.setParameter("tool", tool);
				if (!dq.setMaxResults(300).getResultList().isEmpty()) {
					for (TradeDeal deal : dq.setMaxResults(300).getResultList()) {						
						// А вот тут уже добавить туда нужный ордер.
						deals.get(tool.getId()).add(deal);
					}					
				}
			}
		} 
		
		return deals;
	}
}
