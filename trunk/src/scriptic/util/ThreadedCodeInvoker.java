/*
 * Created on 13 feb 2009
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package scriptic.util;

public class ThreadedCodeInvoker implements scriptic.vm.CodeInvokerThreaded {

	private String name;
	private int priority = Thread.NORM_PRIORITY;
	private ThreadGroup threadGroup;
	private boolean isDeamon;
	private boolean isInterrupted;
	private boolean mustPropagateInterrupt = true;
	Thread thread;
	
	public ThreadedCodeInvoker name(String name) {
		this.name = name;
		return this;
	}
	public ThreadedCodeInvoker priority(int priority) {
		this.priority = priority;
		return this;
	}
	public ThreadedCodeInvoker inThreadGroup(ThreadGroup threadGroup) {
		this.threadGroup = threadGroup;
		return this;
	}
	public ThreadedCodeInvoker deamon() {
		return setDeamon(true);
	}
	public ThreadedCodeInvoker setDeamon(boolean isDeamon) {
		this.isDeamon = isDeamon;
		return this;
	}
	public ThreadedCodeInvoker dontPropagateInterrupt() {
		this.setPropagateInterrupt(false);
		return this;
	}
	public ThreadedCodeInvoker setPropagateInterrupt(boolean mustPropagateInterrupt) {
		this.mustPropagateInterrupt = mustPropagateInterrupt;
		return this;
	}
	public void invokeInThread(Runnable r) {
		thread = threadGroup==null? 
				  new Thread(r)
		         : new Thread(threadGroup,r);
		if (name != null)
		{
    		thread.setName(name);
		}
		thread.setPriority(priority);
		thread.setDaemon(isDeamon);
		thread.start();
	}
	public void interrupt() {
		isInterrupted = true;
		if (mustPropagateInterrupt)
		{
			thread.interrupt();
		}
	}
	public boolean interrupted() {
		return isInterrupted;
	}
	public boolean isAlive() {
		return thread.isAlive();
	}
	@Override
	public void join() throws InterruptedException {
		thread.join();
		
	}
}
