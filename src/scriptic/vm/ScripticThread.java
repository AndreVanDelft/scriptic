/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.vm;

/**
 * Thread subclass with provisions for stopping
 */
public class ScripticThread extends Thread {
  Object mutex = new Object();
  public ScripticThread(String name) {super(name);}
  public ScripticThread(Runnable target, String name) {super(target,name);}
  private boolean mustStop, stopped;
  /** mark this thread to be stopped */
  public  void setStop(boolean b) {mustStop=b; interrupt();}
  public  void setStopped(boolean b) {stopped=b;}
  
  /** answer whether the current thread must stop, if it is a ScripticThread
   * else answer false
   */
  public static boolean mustStop() {return mustStop(Thread.currentThread());}
  /** answer whether the given thread must stop, if it is a ScripticThread
   * else answer false
   */
  static boolean mustStop(Thread t) {
    if (t instanceof ScripticThread) {
      return ((ScripticThread)t).mustStop;
    }
    return false;
  }
  public boolean mustStop1() {return mustStop;}

  /**
   * wait until this thread says it has been stopped,
   * either by the boolean stopped or by the isAlive function
   * Poll using the given polling time
   * @param pollingMillis the polling time
   */
  void waitStop(int pollingMillis) {
    for (;;) {
       if (!isAlive()) return;
       if (stopped) return;
       try {
         //mutex.wait(pollingMillis); yields 
         //  java.lang.IllegalMonitorStateException: current thread not owner
         sleep(pollingMillis);
       }
       catch (InterruptedException e) {}
    }
  }
  /**
   * if the current thread is a Scriptic thread: 
   * publish that it will be stopped by setting the boolean stopped
   * //and by calling notify()
   */
  public static void notifyStop() {
    Thread t = Thread.currentThread();
    if (t instanceof ScripticThread) {
       ((ScripticThread)t).stopped = true;
       //((ScripticThread)t).mutex.notify();
    }
  }
}
