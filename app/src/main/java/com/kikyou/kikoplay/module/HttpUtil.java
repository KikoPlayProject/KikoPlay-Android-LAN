package com.kikyou.kikoplay.module;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class HttpUtil {
    //获取当前设备的CPU数
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心池大小设为CPU数加1
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    //设置线程池的最大大小
    private static final int MAX_POOL_SIZE = 2 * CPU_COUNT + 1;
    //存活时间
    private static final long KEEP_ALIVE = 5L;
    //创建线程池对象
    public static final Executor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public static void getAsync(final String url, final ResponseHandler responseHandler, final int connectTimeout, final int readTimeout) {
        final Handler mHandler = new Handler(Looper.getMainLooper());
        //创建一个新的请求任务
        Runnable requestRunnable = new Runnable() {
            @Override
            public void run() {
                final byte[] result = get(url,connectTimeout,readTimeout);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        responseHandler.onResponse(result);
                    }
                });

            }
        };
        threadPoolExecutor.execute(requestRunnable);
    }
    public static void postAsync(final String url, final String data, final ResponseHandler responseHandler, final int connectTimeout, final int readTimeout) {
        final Handler mHandler = new Handler(Looper.getMainLooper());
        //创建一个新的请求任务
        Runnable requestRunnable = new Runnable() {
            @Override
            public void run() {
                final byte[] result = post(url,data,connectTimeout,readTimeout);
                if(responseHandler==null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        responseHandler.onResponse(result);
                    }
                });

            }
        };
        threadPoolExecutor.execute(requestRunnable);
    }
    public static void getAsync(final String url, final ResponseHandler responseHandler){
        getAsync(url,responseHandler,8000,8000);
    }
    public static byte[] get(String urlString, int connectTimeout, int readTimeout) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            //设置请求方法
            urlConnection.setRequestMethod("GET");
            //设置超时时间
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);

            //获取响应的状态码
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                InputStream in = urlConnection.getInputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                close(in);
                byte[] result = bos.toByteArray();
                close(bos);
                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }
    public static byte[] post(String urlString, String data, int connectTimeout, int readTimeout){
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            DataOutputStream dos= new DataOutputStream(urlConnection.getOutputStream());
            dos.writeBytes(data);
            close(dos);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                InputStream in = urlConnection.getInputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                close(in);
                byte[] result = bos.toByteArray();
                close(bos);
                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }
    private static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
