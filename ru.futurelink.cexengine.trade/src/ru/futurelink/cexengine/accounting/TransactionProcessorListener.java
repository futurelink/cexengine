/**
 * 
 */
package ru.futurelink.cexengine.accounting;

/**
 * @author pavlov
 *
 */
public interface TransactionProcessorListener {
	public boolean QueueExecuteStarted(String wallet);
	public void QueueExecuteComplete(String  wallet);
	public void QueueExecuteInterrupted(String wallet);
}
