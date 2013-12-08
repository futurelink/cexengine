/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeCurrency;

/**
 * @author pavlov
 *
 */
public interface IAccountingService {
	/**
	 * Зачислить деньги в кошелек.
	 */
	public void PutIntoWallet(TradeCurrency currency, TradeAccount account, BigDecimal sum);
	
	/**
	 * Списать деньги с кошелька.
	 */
	public void GetFromWallet(TradeCurrency currency, TradeAccount account, BigDecimal sum);
	
	/**
	 * Переместить деньги между кошельками одного типа валюты.
	 */
	public void MoveBetweenWallets(TradeCurrency currency, TradeAccount sender, TradeAccount reciever, BigDecimal sum);

	/**
	 * Получить баланс кошелька.
	 * 
	 * @param currency
	 * @param account
	 * @return
	 */
	public BigDecimal GetWalletBalance(TradeCurrency currency, TradeAccount account);
	
	public void StartProcessing();
	
	public void StopProcessing();
}
