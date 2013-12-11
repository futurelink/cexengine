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
	public boolean QueueExecuteStarted(String wallet);
	public void QueueExecuteComplete(String  wallet);
	public void QueueExecuteInterrupted(String wallet);
}
