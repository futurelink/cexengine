/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import ru.futurelink.cexengine.orm.TradeTool;

/**
 * @author pavlov
 *
 */
public interface AccountingProcessorListener {
	public void QueueExecuteStarted(TradeTool tool);
	public void QueueExecuteComplete(TradeTool tool);
	public void QueueExecuteInterrupted(TradeTool tool);
}
