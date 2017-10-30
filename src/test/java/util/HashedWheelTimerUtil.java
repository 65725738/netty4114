package util;


import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

public class HashedWheelTimerUtil {
	
	public static void main(String[] args) {
		
		 final Timer timer = new HashedWheelTimer();
	        timer.newTimeout(new TimerTask() {
	            public void run(Timeout timeout) throws Exception {
	                System.out.println("timeout 5");
	            }
	        }, 5, TimeUnit.SECONDS);
	}

}
