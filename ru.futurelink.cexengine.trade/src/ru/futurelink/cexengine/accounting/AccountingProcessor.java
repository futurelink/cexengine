/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.futurelink.cexengine.orm.TradeDeal;
import ru.futurelink.cexengine.orm.TradeTool;
import ru.futurelink.cexengine.orm.TradeTransaction;
import ru.futurelink.cexengine.orm.TradeWallet;

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
	private Thread					mDealProcessorThread;
	private Thread					mTransactionProcessorThread;
	
	// Блокировка очереди выполнения сделок
	private ConcurrentHashMap<String, Boolean> mDealProcessingThreadMutex; 

	// Transaction execution blocking
	private ConcurrentHashMap<String, Boolean> mTransactionProcessingThreadMutex; 
	
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
		mDealProcessingThreadMutex = new ConcurrentHashMap<String, Boolean>();
		mTransactionProcessingThreadMutex = new ConcurrentHashMap<String, Boolean>();
		mTools = new ArrayList<TradeTool>();
	}

	public void startProcessing() {
		if (mDealProcessorThread == null) {
			// Загружаем инструменты
			mTools.clear();
			EntityManager em = mEntityManagerFactory.createEntityManager();
			TypedQuery<TradeTool> toolQuery = em.createQuery("select tool from TradeTool tool", TradeTool.class);
			if (!toolQuery.getResultList().isEmpty()) {
				mTools.addAll(toolQuery.getResultList());
			}
			
			mTransactionProcessorThread = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {
							// Получаем список необработаных сделок сгруппированный по инстирументу 
							ConcurrentHashMap<TradeWallet, ConcurrentLinkedQueue<TradeTransaction>> transactions = getTransactionsToProcess();
							
							for (TradeWallet wallet: transactions.keySet()) {
								// Пораждаем процесс обработки и начинаем все обрабатывать
								if (!transactionProcessingThreadBlocked(wallet.getId())) {
									runTransactionProcessingThread(transactions.get(wallet), wallet);									
								} else {
									mLogger.debug("Transaction thread is processing... skipping.");										
								}
							}

							sleep(SLEEPTIME);
						} catch (InterruptedException e) { 
							// Выполнение прервано... 
						} catch (Exception e) {
							// Damn something bad happened...
							e.printStackTrace();
						}
					}
				}				
			};
			
			// Создаем поток процессинга сделок
			mDealProcessorThread = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {
							if (mTools.size() > 0) {
								// Получаем список необработаных сделок сгруппированный по инстирументу 
								ConcurrentHashMap<String, BlockingQueue<TradeDeal>> deals = getDealsToProcess();
							
								for (TradeTool tool : mTools) {
									// Пораждаем процесс обработки и начинаем все обрабатывать
									if (!dealProcessingThreadBlocked(tool.getId())) {
										runDealProcessingThread(deals.get(tool.getId()), tool);									
									} else {
										mLogger.debug("Deal thread is processing... skipping.");										
									}
								}
							}

							sleep(SLEEPTIME);
						} catch (InterruptedException e) { 
							// Выполнение прервано... 
						} catch (Exception e) {
							// Damn something bad happened...
							e.printStackTrace();
						}
					}
				}
			};

			mDealProcessorThread.start();
			mTransactionProcessorThread.start();
		}
	}
	
	/**
	 * Stops processing loop.
	 */
	public void stopProcessing() {
		mDealProcessorThread.interrupt();
		mTransactionProcessorThread.interrupt();
	}
	

	/**
	 * Runs deal processor thread with specified tool.
	 * 
	 * @param deals a queue of deals to process
	 * @param tool a tool of that deals
	 */
	private void runDealProcessingThread(BlockingQueue<TradeDeal> deals, TradeTool tool) {
		AccountingProcessorListener mExecutionListener = new AccountingProcessorListener() {
			@Override
			public void QueueExecuteStarted(TradeTool tool) {
				dealProcessingThreadBlock(tool.getId(), true);
				mLogger.debug("Accounting processing thread on tool = {} started...", tool.getTitle());
			}
			
			@Override
			public void QueueExecuteInterrupted(TradeTool tool) {		
				dealProcessingThreadBlock(tool.getId(), false);
				mLogger.info("Accounting processing thread interrupted on tool = {}...", tool.getTitle());
			}

			@Override
			public void QueueExecuteComplete(TradeTool tool) {
				dealProcessingThreadBlock(tool.getId(), false);
				mLogger.debug("Accounting processing on tool = {} completed...", tool.getTitle());
			}
		};

		mLogger.debug("Starting deals processing on tool '{}':", tool.getTitle());

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
	 * Runs deal processor thread with specified tool.
	 * 
	 * @param deals a queue of deals to process
	 * @param tool a tool of that deals
	 */
	private void runTransactionProcessingThread(ConcurrentLinkedQueue<TradeTransaction> transactions, TradeWallet wallet) {
		TransactionProcessorListener mExecutionListener = new TransactionProcessorListener() {
			@Override
			public void QueueExecuteStarted(TradeWallet wallet) {
				transactionProcessingThreadBlock(wallet.getId(), true);
				mLogger.debug("Transaction processing thread on wallet = {} started...", wallet.getId());
			}
			
			@Override
			public void QueueExecuteInterrupted(TradeWallet wallet) {		
				transactionProcessingThreadBlock(wallet.getId(), false);
				mLogger.info("Transaction processing thread interrupted on tool = {}...", wallet.getId());
			}

			@Override
			public void QueueExecuteComplete(TradeWallet wallet) {
				transactionProcessingThreadBlock(wallet.getId(), false);
				mLogger.debug("Transaction processing on tool = {} completed...", wallet.getId());
			}
		};

		mLogger.debug("Starting transactions processing on wallet '{}':", wallet.getId());

		// Create execution thread
		TransactionProcessorRunnable runnable = new TransactionProcessorRunnable();
		runnable.setLogger(mLogger);
		runnable.setEntityManagerFactory(mEntityManagerFactory);
		runnable.setTransactions(transactions);
		runnable.setWallet(wallet);
		runnable.setExecutionListener(mExecutionListener);

		// Запускаем обработку ордеров
		Thread thread = new Thread(runnable);
		thread.start();		
	}

	/**
	 * Blocks and unblocks deal processing thread.
	 * Method is thread-Safe.
	 *
	 * @param tool
	 * @param price
	 * @param b
	 */
	protected void dealProcessingThreadBlock(String toolId, boolean blocked) {
		mDealProcessingThreadMutex.put(toolId, blocked);
	}

	/**
	 * Checks whether a deal processing thread is blocked.
	 * Method is thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 */
	protected boolean dealProcessingThreadBlocked(String toolId) {
		if (mDealProcessingThreadMutex.containsKey(toolId))
				return mDealProcessingThreadMutex.get(toolId).booleanValue();
		return false;
	}
	
	/**
	 * Blocks and unblocks transaction processing thread.
	 * Method is thread-Safe.
	 *
	 * @param tool
	 * @param price
	 * @param b
	 */
	protected void transactionProcessingThreadBlock(String walletId, boolean blocked) {
		mTransactionProcessingThreadMutex.put(walletId, blocked);
	}

	/**
	 * Checks whether a transaction processing thread is blocked.
	 * Method is thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 */
	protected boolean transactionProcessingThreadBlocked(String walletId) {
		if (mTransactionProcessingThreadMutex.containsKey(walletId))
				return mTransactionProcessingThreadMutex.get(walletId).booleanValue();
		return false;
	}
		
	/**
	 * Select unprocessed transactions separated by wallet into queues to process.
	 * 
	 * @return
	 */
	private synchronized ConcurrentHashMap<TradeWallet, ConcurrentLinkedQueue<TradeTransaction>> getTransactionsToProcess() {
		ConcurrentHashMap<TradeWallet, ConcurrentLinkedQueue<TradeTransaction>> transactions = 
				new ConcurrentHashMap<TradeWallet, ConcurrentLinkedQueue<TradeTransaction>>();

		// Select all wallets having unprocessed transactions
		TypedQuery<TradeWallet> tq = mEm.createQuery("select trans.mWallet from TradeTransaction trans where "
				+ "trans.mProcessed = 0 group by trans.mWallet having count(trans) > 0", TradeWallet.class);
		if (!tq.getResultList().isEmpty()) {
			for (TradeWallet wallet : tq.getResultList()) {
				// Check whether wallet processing is blocked
				if (transactionProcessingThreadBlocked(wallet.getId())) continue;
				if (transactions.get(wallet) == null)
					transactions.put(wallet, new ConcurrentLinkedQueue<TradeTransaction>());
								
				TypedQuery<TradeTransaction> dq = mEm.createQuery("select trans from TradeTransaction trans where "
						+ "trans.mWallet = :wallet and trans.mProcessed = 0 order by trans.mId asc", TradeTransaction.class);
				dq.setParameter("wallet", wallet);
				
				// Assume result list is not empty, if empty - that's error
				transactions.get(wallet).addAll(dq.setMaxResults(300).getResultList());
			}
		}

		return transactions;
	}
	
	/**
	 * Select unprocessed deals and separates them into queues to process.
	 * 
	 * @return
	 */
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
				if (dealProcessingThreadBlocked(tool.getId())) continue;
				
				if (deals.get(tool.getId()) == null)
					deals.put(tool.getId(), new LinkedBlockingQueue<TradeDeal>());

				TypedQuery<TradeDeal> dq = mEm.createQuery("select deal from TradeDeal deal where "
						+ "deal.mTool = :tool and deal.mProcessed = 0 order by deal.mExecutionTime asc", TradeDeal.class);
				dq.setParameter("tool", tool);
				deals.get(tool.getId()).addAll(dq.setMaxResults(300).getResultList());
			}
		} 
		
		return deals;
	}
}
