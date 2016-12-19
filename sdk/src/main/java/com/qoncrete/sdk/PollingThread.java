package com.qoncrete.sdk;

import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

class PollingThread extends Thread {
    private boolean isClose = false;
    private boolean isPause = false;
    private static int MIN_TIME = 1000;
    private int pollTime = MIN_TIME;
//    private static PollingThread single = null;

    private Listener listener;

//    public synchronized static PollingThread getInstance(int pollTime, Listener listener) {
//        if (single == null) {
//            single = new PollingThread(pollTime, listener);
//        }
//        return single;
//    }

    public PollingThread(int pollTime, Listener listener) {
        this.listener = listener;
        this.pollTime = Math.max(MIN_TIME, pollTime * 1000);
    }

    public synchronized void onThreadPause() {
        isPause = true;
    }

    /**
     * 线程等待,不提供给外部调用
     */
    private void onThreadWait() {
        Log.e(TAG, "onThreadWait: ====  wait  ===");
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onThreadResume() {
        isPause = false;
        this.notify();
    }

    public synchronized void notifyPoll() {
        //有数据要处理时唤醒线程
        this.notify();
    }

    public synchronized void closeThread() {
        try {
            notify();
            setClose(true);
            interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isClose() {
        return isClose;
    }

    public void setClose(boolean isClose) {
        this.isClose = isClose;
    }

    int count = 3;

    @Override
    public void run() {
        while (!isClose && !isInterrupted()) {
            System.out.println("running");
            // 循环发送
            if (listener.hasLog() && !isPause) {
                count--;
                System.out.println("running " + count);
                try {
                    Thread.sleep(pollTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isClose) {
                    listener.sendLog();
                }

            } else {
                onThreadWait();
            }
        }
    }

    interface Listener {
        boolean hasLog();

        void sendLog();
    }
}
