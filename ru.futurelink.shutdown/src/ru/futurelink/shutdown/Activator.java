/**
 * 
 */
package ru.futurelink.shutdown;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pavlov
 *
 */
public class Activator implements BundleActivator {
	private Logger					mLogger;
	public final TerminationThread thread = new TerminationThread();
	public BundleContext			mContext;

	public void start(BundleContext context) throws Exception {
		mLogger = LoggerFactory.getLogger(getClass().getName());
		
		thread.setBundleContext(context);
		thread.setMainThread(Thread.currentThread());
		thread.setLogger(mLogger);
		Runtime.getRuntime().addShutdownHook(thread);

		//context.addBundleListener(new ShutdownLoggingListener());
	}

	public void stop(BundleContext arg0) throws Exception {}

	@SuppressWarnings("unused")
	private final class ShutdownLoggingListener implements SynchronousBundleListener {
		public void bundleChanged(BundleEvent event) {
            if (BundleEvent.STOPPING == event.getType() && event.getBundle() == mContext.getBundle()) {
    	    	mLogger.info("Shutting down, finishing bundle!");
	    		Runtime.getRuntime().removeShutdownHook(thread);
            }
        }
    }

	class TerminationThread extends Thread {
		BundleContext 	mContext;
		Thread			mMainThread;
		Logger			mLogger;
		
		public void setBundleContext(BundleContext ctx) {
			mContext = ctx; 
		}
		
		public void setMainThread(Thread t) {
			mMainThread = t;
		}

		public void setLogger(Logger l) {
			mLogger = l;
		}
		
		@Override
	    public void run() {	    	
	    	try {
	    		mLogger.info("Shutting down the system...");
	    		mContext.getBundle(0).stop();
	    		Thread.sleep(1000);
	    	} catch (BundleException e) {
	    		e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	}
}
