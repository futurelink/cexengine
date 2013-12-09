/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.futurelink.cexengine.orm.TradeOrder;
import ru.futurelink.cexengine.orm.TradeTool;

/**
 * Класс исполнителя заказов из очереди.
 * Этот класс должен быть синглтоном.
 * 
 * @author pavlov
 *
 */
public class OrderExecutor {
	private EntityManagerFactory	mEntityManagerFactory;
	private EntityManager			mEm;
	private Thread					mExecutorThread;
	private Logger					mLogger;

	// Блокировка на выборку, устанавливается тогда, когда становится
	// ясно, какой инструмент и по какой цене будет исполняться.
	private volatile ConcurrentHashMap<TradeTool, ConcurrentHashMap<BigDecimal, Boolean>> mOrderSelectionrMutex;
	
	// Блокировка на исполнение, устанавливается тогда, когда запускается
	// процесс обработки данного инструмента по данной цене.
	private volatile ConcurrentHashMap<String, ConcurrentHashMap<BigDecimal, Boolean>> mExecutionThreadMutex;

	private ArrayList<TradeTool> mTools;

	private OrderExecutor() {
		mLogger = LoggerFactory.getLogger(getClass());
		mLogger.info("Order executor instantiated");
	}

	private static class OrderExecutorHolder {
		public static final OrderExecutor instance = new OrderExecutor();
	}

	/**
	 * Получить экземпляр исполнителя.
	 */
	public static OrderExecutor getInstance() {
		return OrderExecutorHolder.instance;
	}

	/**
	 * Инициализация синглтона.
	 */
	public void init(Orderer orderer, EntityManagerFactory entityManagerFactory) {
		mEntityManagerFactory = entityManagerFactory;
		mEm = mEntityManagerFactory.createEntityManager();
		mExecutionThreadMutex = new ConcurrentHashMap<String, ConcurrentHashMap<BigDecimal, Boolean>>();
		mOrderSelectionrMutex = new ConcurrentHashMap<TradeTool, ConcurrentHashMap<BigDecimal, Boolean>>();
		mTools = new ArrayList<TradeTool>();
	}

	/**
	 * Запустить выполнеие ордеров.
	 */
	public void startExecution() {
		if (mExecutorThread == null) {
			// Загружаем инструменты
			mTools.clear();
			EntityManager em = mEntityManagerFactory.createEntityManager();
			TypedQuery<TradeTool> toolQuery = em.createQuery("select tool from TradeTool tool", TradeTool.class);
			if (!toolQuery.getResultList().isEmpty()) {
				mTools.addAll(toolQuery.getResultList());
			}

			mExecutorThread = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {
							if (mTools.size() > 0) {
								for (TradeTool tool : mTools) {
									// Получаем очереди по инстирументу, очереди сгруппированы по цене,
									// которую мы получим, чтобы все обработать.
									ConcurrentHashMap<BigDecimal, ArrayList<ConcurrentLinkedQueue<TradeOrder>>> queues = getPairedOrders(tool);
									for (BigDecimal price : queues.keySet()) {
										// Пораждаем процесс обработки и начинаем все обрабатывать
										if (!orderExecutionThreadBlocked(tool, price)) {
											runExecutionThread(queues.get(price).get(0), queues.get(price).get(1), tool, price);
										}
									}
								}
							}
							
							sleep(500);
						} catch (InterruptedException e) {
							// Выполнение прервано... 
						}
					}
				}
			};

			mExecutorThread.start();
		}
	}

	/*
	 * Остановить выполнение ордеров.
	 */
	public void stopExecution() {
		if (mExecutorThread != null)
			mExecutorThread.interrupt();
	}
	
	/**
	 * Проверить, не заблокирована ли обработка данного набора "инструмент-цена"?
	 * Thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 */
	protected boolean orderExecutionThreadBlocked(TradeTool tool, BigDecimal price) {
		if (mExecutionThreadMutex.containsKey(tool.getId()) && 
			(mExecutionThreadMutex.get(tool.getId()).get(price) != null)) 
			return mExecutionThreadMutex.get(tool.getId()).get(price).booleanValue();
		return false;
	}
	
	/**
	 * Заблокировать-разблокировать поток исполнения ордеров.
	 * Thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 * @param blocked
	 */
	private void executionThreadBlock(TradeTool tool, BigDecimal price, boolean blocked) {
		if (!mExecutionThreadMutex.containsKey(tool.getId())) {
			mExecutionThreadMutex.put(tool.getId(), new ConcurrentHashMap<BigDecimal, Boolean>());
		}
		mExecutionThreadMutex.get(tool.getId()).put(price, blocked);
	}

	/**
	 * Проверить, не заблокирована ли обработка данного набора "инструмент-цена"?
	 * Thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 */
	protected boolean orderSelectionBlocked(TradeTool tool, BigDecimal price) {
		if (mOrderSelectionrMutex.containsKey(tool) && 
			(mOrderSelectionrMutex.get(tool).get(price) != null)) 
			return mOrderSelectionrMutex.get(tool).get(price).booleanValue();
		return false;
	}
	
	/**
	 * Заблокировать-разблокировать поток исполнения ордеров.
	 * Thread-Safe.
	 * 
	 * @param tool
	 * @param price
	 * @param blocked
	 */
	private void orderSelectionBlock(TradeTool tool, BigDecimal price, boolean blocked) {
		if (!mOrderSelectionrMutex.containsKey(tool)) {
			mOrderSelectionrMutex.put(tool, new ConcurrentHashMap<BigDecimal, Boolean>());
		}
		mOrderSelectionrMutex.get(tool).put(price, blocked);
	}
	
	private void runExecutionThread(
			ConcurrentLinkedQueue<TradeOrder> sellQueue, 
			ConcurrentLinkedQueue<TradeOrder> buyQueue,
			TradeTool tool,
			BigDecimal price) {
		OrderExecutorListener mExecutionListener = new OrderExecutorListener() {
			@Override
			public void QueueExecuteStarted(TradeTool tool, BigDecimal price) {
				executionThreadBlock(tool, price, true);
				mLogger.debug("Starting execution thread on price = {}...", price);
			}
			
			@Override
			public void QueueExecuteInterrupted(TradeTool tool, BigDecimal price) {		
				executionThreadBlock(tool, price, false); // Разблокируем инструмент+цену
				mLogger.debug("Queue execution thread interrupted on price = {}...", price);
			}
			
			@Override
			public void QueueExecuteComplete(TradeTool tool, BigDecimal price) {
				executionThreadBlock(tool, price, false); // Разблокируем инструмент+цену
				mLogger.debug("Queue iteration on price = {} completed...", price);
			}
		};

		mLogger.debug("Running queue to process orders by price {}, quantity for SELL/BUY is {}/{}", price, sellQueue.size(), buyQueue.size());
		
		// Создаем поток исполнителя
		ExecutorRunnable runnable = new ExecutorRunnable();
		runnable.setLogger(mLogger);
		runnable.setEntityManagerFactory(mEntityManagerFactory);
		runnable.setSellQueue(buyQueue);
		runnable.setBuyQueue(sellQueue);
		runnable.setPrice(price);
		runnable.setTool(tool);
		runnable.setExecutionListener(mExecutionListener);

		// Running processor thread
		Thread thread = new Thread(runnable);
		thread.start();
	}

	/**
	 * Получить список заказов на указанную цену по указанному инструменту.
	 * Метод возвращает объект очереди, который должен быть обработан исполнителем
	 * в отдельном потоке.
	 * 
	 * Данный метод должен вызываться при достижении цены. Если цену мы перескочили, то
	 * надо вернуть все, что находится в пределах.
	 */
	public synchronized ConcurrentHashMap<BigDecimal, ArrayList<ConcurrentLinkedQueue<TradeOrder>>> 
		getPairedOrders(TradeTool tool) {
		ConcurrentHashMap<BigDecimal, ArrayList<ConcurrentLinkedQueue<TradeOrder>>> queues = 
				new ConcurrentHashMap<BigDecimal, ArrayList<ConcurrentLinkedQueue<TradeOrder>>>();

		// Напихаем в очереди ордеров из базы,
		// Хак тут в том, что мы выбираем те ордера,
		// сумма по типу которых больше нуля - а значит есть
		// хоть один ордер на покупку, и меньше количества строк
		// значит есть хоть один ордер на продажу.
		
		// Получить цены тех ордеров, по которым есть пары
		TypedQuery<BigDecimal> priceQuery = mEm.createQuery(
				"select ord.mPrice from TradeOrder ord "
				+ "where ord.mExecuted = 0 and ord.mTool = :tool "
				+ "group by ord.mPrice "
				+ "having sum(ord.mOrderType) > 0 "
				+ "and sum(ord.mOrderType) < count(ord)"
				, BigDecimal.class);
		priceQuery.setParameter("tool", tool);				

		// Разблокируем все выборки
		mOrderSelectionrMutex.clear();

		if (!priceQuery.getResultList().isEmpty()) {
			for (BigDecimal price : priceQuery.getResultList()) {
				
				// Вот тут надо смотреть не заблокирована ли очередь,
				// и если заблокирована - не делать запрос и не добавлять
				// элементы.
				if (!orderExecutionThreadBlocked(tool, price)) {
					
					// Тут мы блокируем инструмент+цену на изменение,
					// ордер имеет пару, поэтому будет исполнен => изменить
					// его нельзя.
					orderSelectionBlock(tool, price, true);
					
					TypedQuery<TradeOrder> orderQuery = mEm.createQuery(
						"select ord from TradeOrder ord "
						+ "where ord.mPrice = :price "
						+ "and ord.mExecuted = 0 "
						+ "and ord.mTool = :tool order by ord.mAmount", TradeOrder.class);
					orderQuery.setParameter("price", price);
					orderQuery.setParameter("tool", tool);
				
					mLogger.debug("Получили и добавили в очередь {} ордеров по цене {}", orderQuery.getResultList().size(), price);
				
					for (TradeOrder order : orderQuery.getResultList()) {
						if (queues.get(order.getPrice()) == null) {
							// Если по этой цене еще не было добавлено ордеров, надо создать
							// массив очередей.
							queues.put(order.getPrice(), new ArrayList<ConcurrentLinkedQueue<TradeOrder>>());
							queues.get(order.getPrice()).add(new ConcurrentLinkedQueue<TradeOrder>());
							queues.get(order.getPrice()).add(new ConcurrentLinkedQueue<TradeOrder>());
						}
						// А вот тут уже добавить туда нужный ордер.
						queues.get(order.getPrice()).get(order.getType()).add(order);
					}
				}
			}
		}

		return queues;
	}

}
