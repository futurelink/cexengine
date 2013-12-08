/**
 * 
 */
package ru.futurelink.cexengine.trade;

import java.math.BigDecimal;

import ru.futurelink.cexengine.orm.TradeTool;

/**
 * @author pavlov
 *
 */
public interface OrderExecutorListener {
	public void QueueExecuteStarted(TradeTool tool, BigDecimal price);
	public void QueueExecuteComplete(TradeTool tool, BigDecimal price);
	public void QueueExecuteInterrupted(TradeTool tool, BigDecimal price);
}
