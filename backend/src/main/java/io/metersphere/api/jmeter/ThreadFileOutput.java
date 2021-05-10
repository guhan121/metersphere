package io.metersphere.api.jmeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

@SuppressWarnings("resource")
public class ThreadFileOutput extends PrintStream {

    private static ThreadLocal<ByteArrayOutputStream> threadOutput = new ThreadLocal<>();
    private static PrintStream stdout = System.out;
    private static PrintStream stderr = System.err;

    static {
        System.setOut(new ThreadFileOutput(stdout));
        System.setErr(new ThreadFileOutput(stderr));
    }

    public ThreadFileOutput(OutputStream out) {
        super(out);
    }

    public static void startThreadOutputRedirect(ByteArrayOutputStream stream) {
        threadOutput.set(stream);
    }

    public static void stopThreadOutputRedirect() {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            threadOutput.set(null);
            try {
                stream.flush();
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void forceOut(String line) {
        stdout.println(line);
    }

    public static void forceErr(String line) {
        stderr.println(line);
    }

    @Override
    public void write(byte[] b) throws IOException {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            try {
                stream.write(b);
            } catch (IOException e) {
                threadOutput.set(null);
                throw new RuntimeException(e);
            }
        } else {
            super.write(b);
        }
    }

    @Override
    public void write(int b) {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            stream.write(b);
        } else {
            super.write(b);
        }
    }

    public void write(String b) {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            try {
                stream.write(b.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                super.write(b.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            stream.write(buf, off, len);
        } else {
            super.write(buf, off, len);
        }
    }

    @Override
    public void flush() {
        ByteArrayOutputStream stream = threadOutput.get();
        if (stream != null) {
            try {
                stream.flush();
            } catch (IOException e) {
                threadOutput.set(null);
                throw new RuntimeException(e);
            }
        } else {
            super.flush();
        }
    }
}
