package com.guolifeng.android.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import com.guolifeng.android.servicebestpractice.myinterface.DownloadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by 郭利锋 on 2017/3/26 0026.
 * [简要描述]<BR>
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer>
{
    private static final String TAG = "DownloadTask";
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    private DownloadListener listener;
    private boolean isCanceled;
    private boolean isPaused;
    private int lastProgress;

    public DownloadTask(DownloadListener listener)
    {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params)
    {
        InputStream is = null;
        RandomAccessFile saveFile = null;
        File file = null;
        try
        {
            long downloadLength = 0;//记录已经下载的文件的长度
            String downloadUrl = params[0];
            //获取文件的名字
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //获取SD卡下的download目录
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            //创建我们要使用的文件
            file = new File(directory + fileName);
            //判断文件是否已经存在（这个在文件的操作中必须要考虑到），如果已经存在，则需要读取已经下载的字节数
            if (file.exists())
            {
                downloadLength = file.length();
            }
            //获取我们的要下载的内容的字节数
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0)
            {
                return TYPE_FAILED;
            } else if (contentLength == downloadLength)
            {
                return TYPE_SUCCESS;
            }
            //okHttp的简单实用
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null)
            {
                //将相应转化为输入流
                is = response.body().byteStream();
                saveFile = new RandomAccessFile(file, "rw");
                saveFile.seek(downloadLength);
                //设置一个buffer
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1)
                {
                    if (isCanceled)
                    {
                        return TYPE_CANCELED;
                    }
                    if (isPaused)
                    {
                        return TYPE_PAUSED;
                    }
                    total += len;
                    //写入文件
                    saveFile.write(b, 0, len);
                    int progress = (int) ((total + downloadLength) * 100 / contentLength);
                    publishProgress(progress);
                }
                response.body().close();
                return TYPE_SUCCESS;
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            //最后的一些文件关闭的操作，注意这些必须做
            try
            {
                if (is != null)
                {
                    is.close();
                }
                if (saveFile != null)
                {
                    saveFile.close();
                }
                if (isCanceled && file != null)
                {
                    file.delete();
                }

            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values)
    {
        int progress = values[0];
        //更新进度条
        if (progress > lastProgress)
        {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status)
    {
        //任务结束时需要走的步骤
        switch (status)
        {
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            default:
                break;
        }
    }

    /**
     * 按下了暂停的按钮
     */
    public void pauseDownload()
    {
        isPaused = true;
    }


    public void cancelDownload()
    {
        isCanceled = true;
    }

    /**
     * 根据URL 获取要下载文件的字节数
     *
     * @param downloadUrl url
     * @return 文件的字节数
     * @throws IOException io 异常
     */
    private long getContentLength(String downloadUrl) throws IOException
    {
        //okHttp 的使用 创建客户端
        OkHttpClient client = new OkHttpClient();
        //根据url 创建请求,一定要熟练这种写法
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        //响应
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful())
        {
            //获取内容
            long contentLength = response.body().contentLength();
            //最后别忘了关闭
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
