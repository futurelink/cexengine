/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeCurrency;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public interface IAccountingService {

	/**
	 * 
	 * @param account
	 * @param currency
	 * @return 
	 */
	public TradeWallet FindWallet(TradeAccount account, TradeCurrency currency);
	
	/**
	 * Зачислить деньги в кошелек.
	 */
	public void PutIntoWallet(TradeWallet wallet, BigDecimal sum);
	
	/**
	 * Списать деньги с кошелька.
	 */
	public void GetFromWallet(TradeWallet wallet, BigDecimal sum);
	
	/**
	 * Переместить деньги между кошельками одного типа валюты.
	 */
	public void MoveBetweenWallets(TradeCurrency currency, TradeWallet senderWallet,
			TradeWallet recvWallet, BigDecimal sum);

	/**
	 * Получить баланс кошелька.
	 * 
	 * @param currency
	 * @param account
	 * @return
	 */
	public BigDecimal GetWalletBalance(TradeWallet w);
	
	public void StartProcessing();
	
	public void StopProcessing();
}
