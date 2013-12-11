/**
 * 
 */
package ru.futurelink.cexengine.accounting;

import java.math.BigDecimal;

/**
 * @author pavlov
 *
 */
public interface AccountingProcessorListener {
	public void QueueExecuteStarted(BigDecimal price);
	public void QueueExecuteComplete(BigDecimal price);
	public void QueueExecuteInterrupted(BigDecimal price);
}
