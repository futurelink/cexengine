/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import ru.futurelink.cexengine.orm.TradeWallet;

/**
 * @author pavlov
 *
 */
public interface TransactionProcessorListener {
	public boolean QueueExecuteStarted(TradeWallet wallet);
	public void QueueExecuteComplete(TradeWallet wallet);
	public void QueueExecuteInterrupted(TradeWallet wallet);
}
