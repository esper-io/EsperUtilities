package io.esper.android.files.filelist

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.shockwave.pdfium.PdfDocument
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import io.esper.android.files.R
import io.esper.android.files.databinding.PdfFragmentBinding
import io.esper.android.files.util.ParcelableArgs
import io.esper.android.files.util.args
import io.esper.android.files.util.extraPathList
import io.esper.android.files.util.finish
import io.esper.android.files.util.mediumAnimTime
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.File


class PdfViewerFragment : Fragment(), OnPageErrorListener, OnLoadCompleteListener, OnErrorListener,
    OnTapListener {
    private var pdfView: PDFView? = null
    private lateinit var binding: PdfFragmentBinding
    private val PdfViewerFragmentTag = "PdfViewerFragment"
    private val args by args<Args>()
    private lateinit var systemUiHelper: SystemUiHelper
    private val argsPaths by lazy { args.intent.extraPathList }
    val activity: AppCompatActivity by lazy { requireActivity() as AppCompatActivity }
    private var wrongPasswordEntered = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PdfFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (argsPaths.isEmpty()) {
            // TODO: Show a toast.
            finish()
            return
        }

        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Our app bar will draw the status bar background.
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        ) { visible: Boolean ->
            binding.appBarLayout.animate().alpha(if (visible) 1f else 0f)
                .translationY(if (visible) 0f else -binding.appBarLayout.bottom.toFloat())
                .setDuration(mediumAnimTime.toLong()).setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
        // This will set up window flags.
        systemUiHelper.show()
        loadPdf()
    }

    private fun loadPdf(pdfPassword: String? = null) {
        try {
            val uiMode =
                requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            binding.pdfView.fromFile(argsPaths[0].toFile()).password(pdfPassword)
                .enableAnnotationRendering(true).onError(this).onTap(this).onLoad(this)
                .onPageError(this).autoSpacing(true)
                .nightMode(uiMode == Configuration.UI_MODE_NIGHT_YES).load()
        } catch (e: Exception) {
            Log.e(PdfViewerFragmentTag, "loadPdf: error = $e")
        }
    }

    override fun loadComplete(nbPages: Int) {
        wrongPasswordEntered = 0
        systemUiHelper.delayHide(5000)
        var title = File(argsPaths[0].toString()).name
        try {
            val meta: PdfDocument.Meta = pdfView!!.getDocumentMeta()
            title = meta.title
        } catch (e: Exception) {
            Log.e(PdfViewerFragmentTag, "loadComplete: error = $e")
        }
        updateTitle(title)
    }

    private fun updateTitle(title: String) {
        if (!TextUtils.isEmpty(title)) {
            activity.title = title
        }
    }

    @Parcelize
    class Args(val intent: Intent, val position: Int) : ParcelableArgs

    override fun onPageError(page: Int, t: Throwable?) {
        Log.e(PdfViewerFragmentTag, "Cannot load page $page, throwable: $t")
        Toast.makeText(requireContext(), "Cannot load page $page", Toast.LENGTH_SHORT).show()
    }

    override fun onError(t: Throwable?) {
        if (t != null && t.message?.contains("Password required or incorrect password") == true) {
            if (wrongPasswordEntered > 0) {
                // If wrong password was previously entered, show error message
                showPasswordDialog(
                    requireContext(), showError = "Incorrect password. Please try again."
                )
            } else {
                // Otherwise, show password dialog without error message
                wrongPasswordEntered++
                showPasswordDialog(requireContext())
            }
        } else {
            Log.e(PdfViewerFragmentTag, "Cannot load PDF, throwable: $t")
            Toast.makeText(
                requireContext(), "Cannot load PDF, Maybe Corrupted!", Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onTap(e: MotionEvent?): Boolean {
        systemUiHelper.toggle()
        systemUiHelper.delayHide(5000)
        return true
    }

    private fun showPasswordDialog(context: Context, showError: String? = null) {
        if (wrongPasswordEntered > 3) {
            finish()
        }
        val passwordInput = TextInputEditText(context)
        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val showPasswordCheckBox = CheckBox(context)
        showPasswordCheckBox.text = resources.getString(R.string.show_password)
        showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val cursorPosition = passwordInput.selectionStart // Save cursor position
            if (isChecked) {
                // Show password
                passwordInput.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                passwordInput.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordInput.setSelection(cursorPosition) // Restore cursor position
        }

        // Set margins for password input field
        val passwordInputParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val passwordInputMarginInPixels =
            resources.getDimensionPixelSize(R.dimen.password_input_margin)
        passwordInputParams.setMargins(
            passwordInputMarginInPixels, 0, passwordInputMarginInPixels, 0
        )
        passwordInput.layoutParams = passwordInputParams

        // Set margins for checkbox
        val checkBoxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val checkBoxMarginInPixels = resources.getDimensionPixelSize(R.dimen.checkbox_margin)
        checkBoxParams.setMargins(checkBoxMarginInPixels, 0, 0, 0)
        showPasswordCheckBox.layoutParams = checkBoxParams

        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.addView(passwordInput)
        container.addView(showPasswordCheckBox)

        val dialogBuilder =
            MaterialAlertDialogBuilder(context).setTitle("Password Protected Document").setMessage(
                if (showError.isNullOrEmpty()) {
                    "This document is password protected. Please enter the password to view it."
                } else {
                    showError
                }
            ).setView(container).setPositiveButton(android.R.string.ok) { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isEmpty()) {
                    wrongPasswordEntered++
                    showPasswordDialog(
                        context, showError = "Password cannot be empty. Please try again."
                    )
                } else {
                    loadPdf(password)
                }
            }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }.setCancelable(false)

        if (!showError.isNullOrEmpty()) {
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert)
        }

        dialogBuilder.show()
    }
}
