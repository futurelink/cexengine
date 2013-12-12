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
import ru.futurelink.cexengine.orm.TradeTransaction;
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
	public synchronized void placeOrder(
			TradeTool 		tool, 
			Short 			type, 
			BigDecimal 		price,
			BigDecimal		amount, 
			TradeAccount 	account) throws OrderException {
		TradeWallet[] walletPair = getWalletPair(account, tool, type);

		// If wallet can not be found - throw exception
		if ((walletPair[0] == null) || (walletPair[1] == null)) {
			throw new OrderException();
		}
		
		// Calculate credit sum
		BigDecimal creditSum = null;
		if (type == ITradeService.ORDER_BUY) {
			creditSum = price.multiply(amount);
		} else {
			creditSum = amount;
		}

		// Check for requested sum is available, if there is not enough - throw exception
		if (walletPair[1].getFreeBalance().compareTo(creditSum) == -1) {
			throw new OrderException();
		
		}
		
		TradeOrder order = new TradeOrder();
		order.setAccount(account);
		order.setAmount(amount);
		order.setPrice(price);
		order.setTool(tool);
		order.setType(type);
		order.setPlacedTime(Calendar.getInstance().getTime());
		order.setActive(ITradeService.ACTIVE);

		// Assign credit and debit wallets to order
		order.setDebitWallet(walletPair[0]);
		order.setCreditWallet(walletPair[1]);

		// Create BLOCK transaction
		TradeTransaction tr1 = new TradeTransaction();
		tr1.setDeal(null);
		tr1.setOrder(order);
		tr1.setSum(creditSum);
		tr1.setWallet(order.getCreditWallet());
		tr1.setType(TradeTransaction.TRANSACTION_BLOCK);
		tr1.setProcessed(false);

		mEm.getTransaction().begin();
		mEm.persist(order);
		mEm.persist(tr1);
		mEm.getTransaction().commit();
	}

	/**
	 * Изменение ордера.
	 * 
	 * @param orderId
	 * @param newAmount
	 * @param newPrice
	 * @param active
	 * @return
	 * @throws OrderException 
	 */
	protected void modifyOrder(String orderId, BigDecimal newAmount, BigDecimal newPrice, boolean active) throws OrderException {
		TradeOrder order = mEm.find(TradeOrder.class, orderId);
		if (order != null) {
			// Если на цену и инструмент данного ордера есть очередь исполнения,
			// нельзя позволять изменение ордера, а если запущена очередь исполнения - тем более.
			if (OrderExecutor.getInstance().orderSelectionBlocked(order.getTool(), order.getPrice()) || 
				OrderExecutor.getInstance().orderExecutionThreadBlocked(order.getTool(), order.getPrice())) {
				throw new OrderException();
			}

			// Order amount can not be less then amountProcessed,
			// and price can not be changed and order cannot be 
			// deactivated if it has processed deals on it.
			if ((newAmount != null) && (order.getAmountExecuted().compareTo(newAmount) == 1)) {
				throw new OrderException();
			}
			if (((newPrice != null) || (active != false)) && (order.getAmountExecuted() != null) && (order.getAmountExecuted().compareTo(new BigDecimal(0)) != 0)) {
				throw new OrderException();
			}

			// Conditions passed - processing changes.
			if (newAmount != null) order.setAmount(newAmount);
			if (newPrice != null) order.setPrice(newPrice);
			order.setActive(active);

			mEm.getTransaction();
			mEm.merge(order);
			mEm.getTransaction().commit();
		} else {
			throw new OrderException();
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
	private TradeWallet getWallet(TradeAccount account, TradeTool tool, Short type) {
		TradeCurrency walletCurrency;
		if (type == 0)
			walletCurrency = tool.getCurrency1();
		else
			walletCurrency = tool.getCurrency2();		
		
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
