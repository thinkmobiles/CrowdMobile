package com.kes;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TODO : weakhashmap to all worker theads and shut them down when service stops

public class NetworkService extends Service {
	private final String TAG = NetworkService.class.getSimpleName();
    private final int IDLE_CHECK_DELAY = 5000;


    /*
    public static abstract class ServiceRunnable <T extends ResultWrapper> {

        private T wrapper;

//        public ServiceRunnable(T holder)
//        {
//            this.wrapper = holder;
//        }

        protected void setWrapper(T wrapper)
        {
            this.wrapper = wrapper;
        }

        private void execute(Context context,Session session)
        {
            if (wrapper != null &&
                wrapper.exception != null &&
                wrapper.exception instanceof DataFetcher.KESNetworkException &&
                !wrapper.suppressError)
                session.networkError((DataFetcher.KESNetworkException)wrapper.exception);
            Log.d("CLASS",this.getClass().getSimpleName());
            run(context, session, wrapper);
        }

        public abstract void run(Context context,Session session, T holder);
    }
    */

    private Session session;
    private Handler mHandler;
	private int startId;
	private Map<Intent,WorkerThread> pending = new HashMap<Intent,WorkerThread>();

	public static void execute(Context context, Intent intent)
	{
		intent.setClass(context, NetworkService.class);
		context.startService(intent);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}	
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;
        Log.d(TAG, "OnStart()");

        for (Map.Entry<Intent,WorkerThread> entry : pending.entrySet()) {
            if (areEqual(entry.getKey(),intent))
            {
                Log.d(TAG, "Rejecting duplicate task" + intent.getAction());
                return START_NOT_STICKY;
            }
        }

        WorkerThread wt = new WorkerThread(intent);
        Log.d(TAG,"Starting action" + intent.getAction());
        pending.put(intent,wt);
        this.startId = startId;
        wt.start();
        return START_NOT_STICKY;
    }

    
    @Override
    public void onCreate()
    {
        session = Session.getInstance(this);

        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(checkIdle,IDLE_CHECK_DELAY);

    	Log.d(TAG, "Service created");
    }
    
    @Override
    public void onDestroy()
    {
    	Log.d(TAG, "Service destroyed");
        mHandler.removeCallbacksAndMessages(null);
        for (WorkerThread value : pending.values()) {
            value.interrupt();
        }
        session.serviceShutdown();
        session = null;
    	super.onDestroy();
    }

    class RunnableWrapper implements Runnable {

      private NetworkExecutable runnable;
      private Intent intent;

      public RunnableWrapper(NetworkExecutable runnable, Intent intent)
      {
          this.runnable = runnable;
          this.intent = intent;
      }

        @Override
        public void run() {
            if (mHandler != null && runnable != null)
                runnable.serviceExecuteOnUI(NetworkService.this, session);
            Log.d(TAG,"Action finished " + intent.getAction());
            pending.remove(intent);
        }
    };

    Runnable checkIdle = new Runnable() {
        @Override
        public void run() {
            if (pending.size() == 0)
                NetworkService.this.stopSelf(startId);
            else
                if (mHandler != null)
                    mHandler.postDelayed(this,IDLE_CHECK_DELAY);
        }
    };

	class WorkerThread extends Thread
	{
		private Intent intent;
		
		public WorkerThread(Intent intent)
		{
			this.intent = intent;
		}
		
		@Override
		public void run() {
            try {
                NetworkExecutable ne = (NetworkExecutable) Class.forName(intent.getAction()).newInstance();
                ne.serviceExecuteOnThread(NetworkService.this, intent);
                mHandler.post(new RunnableWrapper(ne, intent));
            } catch (InstantiationException e)
            {
                throw new RuntimeException((e));
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException((e));
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException((e));
            } catch (InterruptedException ignored) {
            }
        }
	}
	
	private boolean areEqual(Intent a, Intent b) {
        if (a.filterEquals(b)) {
            Bundle aExtras = a.getExtras();
            if (aExtras != null)
                Log.d("Tag","aExtras OK");
            Bundle bExtras = b.getExtras();
            Log.d("Tag","bExtras OK");

	        if (aExtras != null && bExtras != null) {
                Set<String> aKeySet = aExtras.keySet();
                Set<String> bKeySet = bExtras.keySet();

	            // check if the keysets are the same size
	            if (aKeySet.size() != bKeySet.size())
                    return false;
                if (aKeySet.size() == 0)
                    return true;    //there was a nullpointerexception
	            // compare all of a's extras to b
	            for (String key : aKeySet) {
	                if (!bExtras.containsKey(key))
	                    return false;
	                else
	                {
	                	Object o1 = aExtras.get(key);
	                	Object o2 = bExtras.get(key);
	                	if ((o1 == null && o2 != null) || (o2 == null && o1 != null))
                            return false;
                        if (o1 != null && o2 != null && !o1.equals(o2))
                            return false;
	                }
	            }
	            // compare all of b's extras to a
	            for (String key : bKeySet) {
	                if (!aExtras.containsKey(key))
	                    return false;
                    else
                    {
                        Object o1 = aExtras.get(key);
                        Object o2 = bExtras.get(key);
                        if ((o1 == null && o2 != null) || (o2 == null && o1 != null))
                            return false;
                        if (o1 != null && o2 != null && !o1.equals(o2))
                            return false;
                    }
	            }
                return true;
	        }
	        if (aExtras == null && bExtras == null)
                return true;
	        // either a has extras and b doesn't or b has extras and a doesn't
	        return false;
	    } else
	        return false;
	}
	
}
