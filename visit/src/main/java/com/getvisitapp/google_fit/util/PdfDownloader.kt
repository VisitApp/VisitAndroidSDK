package com.getvisitapp.google_fit.util

import android.util.Log
import androidx.annotation.Keep
import com.getvisitapp.google_fit.network.RetroServiceInterface
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.*
import kotlin.concurrent.thread

@Keep
class PdfDownloader {

    private lateinit var pdfFileName: File
    private lateinit var dirPath: String
    private lateinit var fileName: String


    fun downloadPdfFile(
        fileDir: File,
        pdfUrl: String,
        authorization:String,
        onDownloadComplete: (file: File) -> Unit,
        onDownloadFailed: () -> Unit
    ) {

        dirPath = "${fileDir}/files/pdfFiles"
        val dirFile = File(dirPath)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        fileName = "${UUID.randomUUID()}.pdf"
        val file = "${dirPath}/${fileName}"
        pdfFileName = File(file)
        if (pdfFileName.exists()) {
            pdfFileName.delete()
        }
        thread {
            val retroServiceInterface = RetrofitInstance.getRetroInstance().create(
                RetroServiceInterface::class.java
            )
            retroServiceInterface.downloadPdfFile(pdfUrl, authorization = authorization).enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onDownloadFailed()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.e("====", "====response : " + response)
                    Log.e("====", "====response : " + response.isSuccessful)
                    if (response.isSuccessful) {
                        val result = response.body()?.byteStream()
                        result?.let {
                            writeToFile(it, onDownloadComplete, onDownloadFailed)
                        } ?: kotlin.run {
                            onDownloadFailed()
                        }
                    } else {
                        onDownloadFailed()
                    }
                }
            })
        }
    }

    private fun writeToFile(
        inputStream: InputStream, onDownloadComplete: (file: File) -> Unit,
        onDownloadFailed: () -> Unit
    ) {
        try {
            Log.e("====", "====writeToFile : ")
            val fileReader = ByteArray(4096)
            var fileSizeDownloaded = 0
            val fos: OutputStream = FileOutputStream(pdfFileName)
            do {
                val read = inputStream.read(fileReader)
                if (read != -1) {
                    fos.write(fileReader, 0, read)
                    fileSizeDownloaded += read
                }
            } while (read != -1)
            fos.flush()
            fos.close()
            onDownloadComplete(pdfFileName)

        } catch (e: IOException) {
            Log.e("====", "====IOException : " + e)
            onDownloadFailed()
        }
    }
}