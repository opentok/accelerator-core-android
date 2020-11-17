package com.opentok.otsdkwrapper.utils;

import android.util.Log;
import android.util.SparseArray;

/**
 * Created by amac on 01/02/2017.
 */

public class LogWrapper {

  public static final short LOG_ERROR = 0x1;
  public static final short LOG_WARN = 0x2;
  public static final short LOG_INFO = 0x4;
  public static final short LOG_DEBUG = 0x8;
  public static final short LOG_VERBOSE = 0x10;

  protected interface RunnableLogger {
    void log(String tag, String message);
    void log(String tag, String message, Exception e);
  }

  /**
   * Protected and not final to allow modification by subclasses
   */
  protected static SparseArray<RunnableLogger> sLoggers = new SparseArray<RunnableLogger>() {
    {
      append(LOG_ERROR, new RunnableLogger() {
        @Override
        public void log(String tag, String message) {
          Log.e(tag, message);
        }

        @Override
        public void log(String tag, String message, Exception e) {
          Log.e(tag, message, e);
        }
      });

      append(LOG_WARN, new RunnableLogger() {
        @Override
        public void log(String tag, String message) {
          Log.w(tag, message);
        }

        @Override
        public void log(String tag, String message, Exception e) {
          Log.w(tag, message, e);
        }
      });

      append(LOG_INFO, new RunnableLogger() {
        @Override
        public void log(String tag, String message) {
          Log.i(tag, message);
        }

        @Override
        public void log(String tag, String message, Exception e) {
          Log.i(tag, message, e);
        }
      });

      append(LOG_DEBUG, new RunnableLogger() {
        @Override
        public void log(String tag, String message) {
          Log.d(tag, message);
        }

        @Override
        public void log(String tag, String message, Exception e) {
          Log.d(tag, message, e);
        }
      });

      append(LOG_VERBOSE, new RunnableLogger() {
        @Override
        public void log(String tag, String message) {
          Log.v(tag, message);
        }

        @Override
        public void log(String tag, String message, Exception e) {
          Log.v(tag, message, e);
        }
      });
    }
  };

  private short mCurrentLogLevel;

  public void setLogLevel(short logLevel) {
    mCurrentLogLevel = logLevel;
  }

  public void enableLevels(short levels) {
    mCurrentLogLevel |= levels;
  }

  public void disableLevels(short levels) {
    mCurrentLogLevel |= (0xFF ^ levels);
  }

  public LogWrapper(short logLevel) {
    setLogLevel(logLevel);
  }

  private void log(short level, Object... messages) {
    RunnableLogger logger;
    if ((mCurrentLogLevel & level) == 0 || (logger = sLoggers.get(level)) == null) {
      return;
    }
    String logTag = null;
    StringBuilder message = new StringBuilder();
    Exception e = null;
    for(Object element: messages) {
      if (element instanceof Exception) {
        e = (Exception) element;
      } else {
        if (element == null) {
          element = "null";
        }
        if (logTag == null) {
          logTag = element.toString();
        } else {
          message.append(element.toString());
        }
      }
    }
    if (e != null) {
      logger.log(logTag, message.toString(), e);
    } else {
      logger.log(logTag, message.toString());
    }
  }

  public void e(Object... messages) {
    log(LOG_ERROR, messages);
  }

  public void d(Object... messages) {
    log(LOG_DEBUG, messages);
  }

  public void w(Object... messages) {
    log(LOG_WARN, messages);
  }

  public void i(Object... messages) {
    log(LOG_INFO, messages);
  }
  public void v(Object... messages) {
    log(LOG_VERBOSE, messages);
  }

}
