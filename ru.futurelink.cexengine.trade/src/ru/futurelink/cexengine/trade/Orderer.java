/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeCurrency;
import ru.futurelink.cexengine.orm.TradeTool;
import ru.futurelink.cexengine.orm.TradeOrder;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * Класс, создающий заказы в базе и управляющий очередью заказов.
 * Работает thread-safe.
 * 
 * @author pavlov
 *
 */
public class Orderer {
	private EntityManager mEm;

	public Orderer(EntityManagerFactory entityManagerFactory) {
		mEm = entityManagerFactory.createEntityManager();
	}

	/**
	 * Разместить заказ.
	 * 
	 * @param tool
	 * @param type
	 * @param price
	 * @param amount
	 * @return
	 */
	public synchronized boolean placeOrder(TradeTool tool, Short type, BigDecimal price, BigDecimal amount, TradeAccount account) {
		TradeWallet[] walletPair = getWalletPair(account, tool, type);
		TradeOrder order = new TradeOrder();
		order.setAccount(account);
		order.setAmount(amount);
		order.setPrice(price);
		order.setTool(tool);
		order.setType(type);
		order.setPlacedTime(Calendar.getInstance().getTime());
		order.setActive(ITradeService.ACTIVE);
		
		// Привязываем кошелек сразу к ордеру
		// и блокируем сумму в кошельке.
		order.setWalletCurrency1(walletPair[0]);
		order.setWalletCurrency2(walletPair[1]);
		/*if (type == ITradeService.ORDER_BUY)
			walletPair[1].blockSum(price.multiply(amount));
		else
			walletPair[1].blockSum(amount);*/

		mEm.getTransaction().begin();
		mEm.persist(order);
		//mEm.merge(walletPair[1]);
		mEm.getTransaction().commit();
		
		return true;
	}

	/**
	 * Изменение ордера.
	 * 
	 * @param orderId
	 * @param newAmount
	 * @param newPrice
	 * @param active
	 * @return
	 */
	protected boolean modifyOrder(String orderId, BigDecimal newAmount, BigDecimal newPrice, Boolean active) {
		TradeOrder order = mEm.find(TradeOrder.class, orderId);
		if (order != null) {
			// Если на цену и инструмент данного ордера есть очередь исполнения,
			// нельзя позволять изменение ордера, а если запущена очередь исполнения - тем более.
			if (OrderExecutor.getInstance().orderSelectionBlocked(order.getTool(), order.getPrice()) || 
				OrderExecutor.getInstance().orderExecutionThreadBlocked(order.getTool(), order.getPrice())) return false;

			// Установить блокировку на ордер - как изменяемый.
			// Изменить, сохранить, снять блокировку.
			if (newAmount != null) order.setAmount(newAmount);
			if (newPrice != null) order.setPrice(newPrice);
			order.setActive(active);

			mEm.getTransaction();
			mEm.merge(order);
			mEm.getTransaction().commit();
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * Получить кошелек на основании аккаунта, пары и типа сделки.
	 * 
	 * @param account
	 * @param tool
	 * @param type
	 * @return
	 */
	private TradeWallet getWallet(TradeAccount account, TradeTool tool, Short type) {
		TradeCurrency walletCurrency;
		if (type == 0)
			walletCurrency = tool.getCurrency2();
		else
			walletCurrency = tool.getCurrency1();		
		
		TypedQuery<TradeWallet> wq = mEm.createQuery("select w from TradeWallet w where "
				+ "w.mAccount = :account and w.mCurrency = :currency", TradeWallet.class);
		wq.setParameter("currency", walletCurrency);
		wq.setParameter("account", account);
		if (wq.getResultList().size() == 0) {
			// Кошелек не найден, вероятно не создан, надо создать.
			TradeWallet w = new TradeWallet();
			w.setCurrency(walletCurrency);
			w.setAccount(account);
			
			mEm.getTransaction().begin();
			mEm.persist(w);
			mEm.getTransaction().commit();

			return w;
		} else {
			return wq.getResultList().get(0);
		}
	}
	
	/**
	 * Получить кошелек на основании аккаунта, пары и типа сделки.
	 * 
	 * @param account
	 * @param tool
	 * @param type
	 * @return
	 */
	private TradeWallet[] getWalletPair(TradeAccount account, TradeTool tool, Short type) {
		TradeWallet[] walletPair = new TradeWallet[2];
		if (type == ITradeService.ORDER_BUY) {
			walletPair[0] = getWallet(account, tool, (short)0);
			walletPair[1] = getWallet(account, tool, (short)1);
		} else {
			walletPair[0] = getWallet(account, tool, (short)1);
			walletPair[1] = getWallet(account, tool, (short)0);			
		}
		
		return walletPair;
	}	
}
