/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeTool;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public class TradeServiceInstance implements ITradeService {
	private volatile OrderExecutor		mExecutor;
	private ThreadLocal<Orderer>		mOrderer;
	
	private EntityManagerFactory		mEntityManagerFactory;
	private ThreadLocal<EntityManager>	mEm;	// Локальный менеджер
	private Logger						mLogger;
	
	public TradeServiceInstance(EntityManagerFactory entityManagerFactory) {
		mLogger = LoggerFactory.getLogger(getClass().getName());
		
		mEntityManagerFactory = entityManagerFactory;
		mEm =new ThreadLocal<EntityManager>();
		mEm.set(mEntityManagerFactory.createEntityManager());

		mOrderer = new ThreadLocal<Orderer>();
		mOrderer.set(new Orderer(mEntityManagerFactory));
	}

	@Override
	public void PlaceOrder(TradeTool tool, Short type, BigDecimal amount, BigDecimal price, TradeAccount account) throws OrderException {
		if (mOrderer != null)
			mOrderer.get().placeOrder(tool, type, price, amount, account);
	}

	@Override
	public void CancelOrder(String orderId) throws OrderException {
		if (mOrderer != null)
			mOrderer.get().modifyOrder(orderId, null, null, ITradeService.CANCELLED);
	}

	@Override
	public void CorrectOrder(String orderId, BigDecimal amount, BigDecimal price) throws OrderException {
		if (mOrderer != null)
			mOrderer.get().modifyOrder(orderId, amount, price, ITradeService.ACTIVE);
	}

	@Override
	public void StartExecution() {
		mExecutor = OrderExecutor.getInstance();
		mExecutor.init(mOrderer.get(), mEntityManagerFactory);
		mExecutor.startExecution();
	}

	@Override
	public void StopExecution() {
		mExecutor.stopExecution();
	}

	@Override
	public TradeTool GetTool(String toolName) {
		// Select tool by name? if doesn't exist create the new one
		TypedQuery<TradeTool> toolQuery = mEm.get().createQuery(
				"select tool from TradeTool tool where tool.mTitle = :title", TradeTool.class);
		toolQuery.setParameter("title", toolName);
		if (!toolQuery.getResultList().isEmpty()) {
			return toolQuery.getResultList().get(0);
		}
		return null;
	}

	@Override
	public TradeAccount GetAccount(String number) {
		TypedQuery<TradeAccount> accQuery = mEm.get().createQuery(
				"select a from TradeAccount a where a.mNumber = :accNumber", TradeAccount.class);
		accQuery.setParameter("accNumber", number);
		if (accQuery.getResultList().size() > 0) {
			TradeAccount acc = accQuery.getResultList().get(0);
			mLogger.debug("Got account by number {}: {}", number, acc.getId());
			return acc;
		}
		return null;
	}

	@Override
	public TradeWallet[] GetWallets(TradeAccount account) {
		TradeWallet[] wallets;
		TypedQuery<TradeWallet> wQuery = mEm.get().createQuery(
				"select w from TradeWallet w where w.mAccount = :account", TradeWallet.class);
		wQuery.setParameter("account", account);
		if(!wQuery.getResultList().isEmpty()) {
			wallets = new TradeWallet[wQuery.getResultList().size()];
			int i = 0;
			for (TradeWallet w : wQuery.getResultList()) {
				wallets[i] = w;
				i++;
			}
			return wallets;
		}

		return null;
	}
}
