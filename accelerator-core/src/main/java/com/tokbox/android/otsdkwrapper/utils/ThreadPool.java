package com.tokbox.android.otsdkwrapper.utils;

import com.tokbox.android.otsdkwrapper.GlobalLogLevel;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by amac on 09/02/2017.
 * Implements a simple, dynamic, thread pool that can be used to execute runnables. The way
 * the pool works is:
 *   - At creation time the minimum number of threads is defined (by default, 5)
 */
public class ThreadPool {
  private final String LOG_TAG = this.getClass().getSimpleName();
  private static final short LOCAL_LOG_LEVEL = LogWrapper.LOG_ERROR | LogWrapper.LOG_WARN;
  private static final LogWrapper LOG =
    new LogWrapper((short)(GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));

  public static void setLogLevel(short logLevel) {
    LOG.setLogLevel(logLevel);
  }

  private class WorkerThread extends Thread {

    private ThreadPool mPool;

    public WorkerThread(ThreadPool pool) {
      mPool = pool;
    }

    private boolean mKeepRunning = true;

    @Override
    public void run() {
      while (mKeepRunning) {
        try {
          Runnable task = mPool.getTask();
          mPool.moveToBusy(this);
          if (task != null) {
            task.run();
          }
          mKeepRunning = mPool.moveToFree(this);
        } catch (InterruptedException e) {
        }
      }
      LOG.d(LOG_TAG, "WorkerThread: ", this, " exiting");
    }

    public void finish() {
      mKeepRunning = false;
      interrupt();
    }

  }

  private int mMinLiveThreads;

  private LinkedBlockingQueue<Runnable> mTasks;

  private ArrayList<WorkerThread> mFreeThreads;
  private ArrayList<WorkerThread> mBusyThreads;

  private synchronized void createNewFreeThread() {
    WorkerThread newThread = new WorkerThread(this);
    mFreeThreads.add(newThread);
    LOG.d(LOG_TAG, "Adding new thread: ", newThread, " to the free list. Total size: ",
          mFreeThreads.size() + mBusyThreads.size());
    newThread.start();
  }

  public Runnable getTask() throws InterruptedException {
    return mTasks.take();
  }

  public synchronized void moveToBusy(WorkerThread thread) {
    mFreeThreads.remove(thread);
    mBusyThreads.add(thread);
  }

  public synchronized boolean moveToFree(WorkerThread thread) {
    mBusyThreads.remove(thread);
    int numOfThreads = mFreeThreads.size() + mBusyThreads.size();
    LOG.d(LOG_TAG, "moveToFree: ", thread, " Number of live threads: ", numOfThreads,
          ". Minimum number: ", mMinLiveThreads);
    boolean shouldKeepRunning = numOfThreads < mMinLiveThreads;
    if (shouldKeepRunning) {
      mFreeThreads.add(thread);
    }
    return shouldKeepRunning;
  }

  public ThreadPool(int minLiveThreads) {
    mMinLiveThreads = minLiveThreads;
    mTasks = new LinkedBlockingQueue<>();
    mFreeThreads = new ArrayList<>();
    mBusyThreads = new ArrayList<>();
    for(int i = 0; i < mMinLiveThreads; i++) {
      createNewFreeThread();
    }
  }

  public ThreadPool() {
    this(5);
  }

  /**
   * Runs 'runnable' asynchronously. The runnable will be executed on one of the live threads
   * if there are any free, or a new thread will be created to process it otherwise.
   * @param runnable
   */
  public synchronized void runAsync(Runnable runnable) {
    mTasks.add(runnable);
    if (mFreeThreads.size() == 0) {
      createNewFreeThread();
    }
  }

  public synchronized void finish() {
    for(WorkerThread thread: mFreeThreads) {
      thread.finish();
    }
    for(WorkerThread thread: mBusyThreads) {
      thread.finish();
    }
    mFreeThreads = null;
    mBusyThreads = null;
  }

}
