/*
 * Created on 6 apr 2009
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package scriptic.vm;

public class DefaultCodeInvokerThreaded implements CodeInvokerThreaded {

	Thread thread;
	
	DefaultCodeInvokerThreaded() {}
	
	@Override
	public void interrupt() {
		thread.interrupt();
	}

	@Override
	public void invokeInThread(Runnable r) {
		thread = new Thread(r);
		thread.start();
	}

	@Override
	public boolean isAlive() {
		return thread!=null && thread.isAlive();
	}

	@Override
	public void join() throws InterruptedException {
		thread.join();
	}

	@Override
	public boolean isInterrupted() {
		return thread.isInterrupted();
	}

}
