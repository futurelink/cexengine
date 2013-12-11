/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;

import ru.futurelink.cexengine.orm.TradeTool;

/**
 * @author pavlov
 *
 */
public interface AccountingProcessorListener {
	public void QueueExecuteStarted(BigDecimal price);
	public void QueueExecuteComplete(BigDecimal price);
	public void QueueExecuteInterrupted(BigDecimal price);
}
