package com.alya.ecommerce_serang.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.alya.ecommerce_serang.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PopUpDialog {
    fun showConfirmDialog(
        context: Context,
        title: String ,
        message: String? = null,
        iconRes: Int? = null,
        positiveText: String? = null,
        negativeText: String? = null,
        onYesClicked: (() -> Unit)? = null,
        onNoClicked: (() -> Unit)? = null
    ) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_popup, null)

        val iconView = dialogView.findViewById<ImageView>(R.id.dialogIcon)
        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val yesButton = dialogView.findViewById<MaterialButton>(R.id.btnYes)
        val noButton = dialogView.findViewById<MaterialButton>(R.id.btnNo)

        if (iconRes != null) {
            iconView.setImageResource(iconRes)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }

        // Title
        titleView.text = title

        // Message
        if (message.isNullOrEmpty()) {
            messageView.visibility = View.GONE
        } else {
            messageView.text = message
            messageView.visibility = View.VISIBLE
        }

        // Yes button (always visible, but customizable text)

        if (positiveText.isNullOrEmpty()) {
            yesButton.visibility = View.GONE
        } else {
            yesButton.text = positiveText
            yesButton.visibility = View.VISIBLE
        }

        // No button (optional)
        if (negativeText.isNullOrEmpty()) {
            noButton.visibility = View.GONE
        } else {
            noButton.text = negativeText
            noButton.visibility = View.VISIBLE
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_MyApp_AlertDialog)
            .setView(dialogView)
            .create()

        yesButton.setOnClickListener {
            onYesClicked?.invoke()
            dialog.dismiss()
        }

        noButton.setOnClickListener {
            onNoClicked?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}