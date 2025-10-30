package com.cashpal.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.cashpal.app.R

class CsvViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csv_viewer)

        val html = intent.getStringExtra(EXTRA_HTML).orEmpty()
        val fileUri = intent.getStringExtra(EXTRA_URI)?.let { Uri.parse(it) }

        val webView: WebView = findViewById(R.id.csvWebView)
        webView.settings.apply {
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        findViewById<Button>(R.id.btnShareCsv).setOnClickListener {
            if (fileUri != null) {
                shareCsv(fileUri)
            } else {
                Toast.makeText(this, R.string.csv_share_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnCloseCsv).setOnClickListener {
            finish()
        }
    }

    private fun shareCsv(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.csv_share_title)))
    }

    companion object {
        const val EXTRA_HTML = "extra_html"
        const val EXTRA_URI = "extra_uri"
    }
}
