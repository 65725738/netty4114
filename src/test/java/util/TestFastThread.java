package util;

import java.nio.ByteBuffer;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;

public class TestFastThread {
	
	public static void main(String[] args) throws InterruptedException {
		FastThreadLocalThread thread1 = new FastThreadLocalThread(()->{
			 final FastThreadLocal<String> tmp = new FastThreadLocal<String>() {
			        @Override
			        protected String initialValue() throws Exception {
			            return new String("nihao1");
			        }
			    };
			    
			    while(true){
			    	try {
						Thread.sleep(1000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	System.out.println(tmp.get());
			    }
		});
		    
		FastThreadLocalThread thread2 = new FastThreadLocalThread(()->{
			 final FastThreadLocal<String> tmp = new FastThreadLocal<String>() {
			        @Override
			        protected String initialValue() throws Exception {
			            return new String("nihao2");
			        }
			    };
			    
			    while(true){
			    	try {
						Thread.sleep(1000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	System.out.println(tmp.get());
			    }
		});

		FastThreadLocalThread thread3  = new FastThreadLocalThread(()->{
			 final FastThreadLocal<String> tmp = new FastThreadLocal<String>() {
			        @Override
			        protected String initialValue() throws Exception {
			            return new String("nihao3");
			        }
			    };
			    
			    final FastThreadLocal<String> tmp1 = new FastThreadLocal<String>() {
			        @Override
			        protected String initialValue() throws Exception {
			            return new String("nihao4");
			        }
			    };
			    while(true){
			    	try {
						Thread.sleep(1000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	System.out.println(tmp.get());
			    	System.out.println(tmp1.get());

			    }
		});
		thread1.start();
		thread2.start();
		thread3.start();
	}

}
