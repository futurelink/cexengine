/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;

import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.orm.TradeTool;
import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public interface ITradeService {
	
	public static boolean CANCELLED = false;
	public static boolean ACTIVE = true;
	
	public static short ORDER_BUY = 0;
	public static short ORDER_SELL = 1;
	
	/**
	 * Разместить ордер.Размещение ордера должно быть реализовано в виде
	 * атомарной операции.
	 * @throws OrderException 
	 */
	public void PlaceOrder(TradeTool tool, Short type, BigDecimal amount, BigDecimal price, TradeAccount account) throws OrderException;
	
	/**
	 * Отменить ордер. 
	 */
	public void CancelOrder(String orderId) throws OrderException;
	
	/**
	 * Изменить ордер.
	 */
	public void CorrectOrder(String orderId, BigDecimal amount, BigDecimal price) throws OrderException;
	
	/**
	 * Начать выполнение задач по осуществлению сделок.
	 */
	public void StartExecution();
	
	/**
	 * Остановить выполнение ордеров.
	 */
	public void StopExecution();
	
	/**
	 * Получить объект торгового инструмента.
	 * 
	 * @param toolName
	 * @return
	 */
	public TradeTool GetTool(String toolName);

	/**
	 * 
	 * @param number
	 * @return
	 */
	public TradeAccount GetAccount(String number);
	
	/**
	 * 
	 * @param account
	 * @return
	 */
	public TradeWallet[] GetWallets(TradeAccount account);
}
