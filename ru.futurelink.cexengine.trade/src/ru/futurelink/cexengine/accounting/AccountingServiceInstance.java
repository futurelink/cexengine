/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeCurrency;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public class AccountingServiceInstance implements IAccountingService {

	private EntityManagerFactory	mEntityManagerFactory;
	private EntityManager			mEm;

	/**
	 * 
	 */
	public AccountingServiceInstance(EntityManagerFactory factory) {
		mEntityManagerFactory = factory;
		mEm = mEntityManagerFactory.createEntityManager();
	}

	@Override
	public TradeWallet FindWallet(TradeAccount account, TradeCurrency currency) {
		TypedQuery<TradeWallet> wq = mEm.createQuery("select w from Wallet w where w.mAccount = :account and w.mCurrency = :currency", TradeWallet.class);
		wq.setParameter("account", account);
		wq.setParameter("currency", currency);
		if (!wq.getResultList().isEmpty()) {
			return wq.getSingleResult();
		} else {
			return null;
		}		
	}
	
	@Override
	public void PutIntoWallet(TradeWallet w,	BigDecimal sum) {
		if (w != null) {
			w.addSum(sum);		
		
			mEm.getTransaction().begin();
			mEm.merge(w);
			mEm.getTransaction().commit();
		}
	}

	@Override
	public void GetFromWallet(TradeWallet w,	BigDecimal sum) {
		if (w != null) {
			w.subtractSum(sum);		

			mEm.getTransaction().begin();
			mEm.merge(w);
			mEm.getTransaction().commit();
		}
	}

	@Override
	public void MoveBetweenWallets(TradeCurrency currency, TradeWallet senderWallet,
			TradeWallet recvWallet, BigDecimal sum) {
		
		// Если какой-то из кошельков - null, выходим.
		if (senderWallet == null || recvWallet == null) {
			return;
		}
		
		if (senderWallet.getBalance().compareTo(sum) >= 0) {
			senderWallet.subtractSum(sum);
			recvWallet.addSum(sum);

			mEm.getTransaction().begin();
			mEm.merge(senderWallet);
			mEm.merge(recvWallet);
			mEm.getTransaction().commit();			
		} else {
			// Недостаточно средств!
		}
	}

	@Override
	public BigDecimal GetWalletBalance(TradeWallet w) {
		if (w != null) {
			return w.getBalance();
		}
		return null;
	}

	@Override
	public void StartProcessing() {
		AccountingProcessor mProcessor = AccountingProcessor.getInstance();
		mProcessor.init(mEntityManagerFactory);
		mProcessor.startProcessing();	
	}

	@Override
	public void StopProcessing() {
		AccountingProcessor.getInstance().stopProcessing();
	}

}
