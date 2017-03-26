package com.guolifeng.android.servicebestpractice.myinterface;

/**
 * Created by 郭利锋 on 2017/3/26 0026.
 * [简要描述]<BR>
 */

public interface DownloadListener
{
    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
