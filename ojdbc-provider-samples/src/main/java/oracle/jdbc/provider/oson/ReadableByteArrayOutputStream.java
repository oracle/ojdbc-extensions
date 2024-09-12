package oracle.jdbc.provider.oson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

public class ReadableByteArrayOutputStream extends ByteArrayOutputStream {
 
  private final Lock lock = new ReentrantLock();

  public ReadableByteArrayOutputStream() {
  }

  public ReadableByteArrayOutputStream(int size) {
      super(size);
  }

  public synchronized InputStream read() {
      lock.lock();
      try{
          return new ByteArrayInputStream(buf, 0, count);
      }
      finally {
          lock.unlock();
      }
  }

  @Override
  public synchronized void reset() {
     lock.lock();
     try {
       buf = new byte[buf.length];
       count = 0;
     }
     finally {
       lock.unlock();
     }
  }
}