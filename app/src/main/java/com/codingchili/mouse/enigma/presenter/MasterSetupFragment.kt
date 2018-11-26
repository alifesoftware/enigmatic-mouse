package com.codingchili.mouse.enigma.presenter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.Fragment
import com.codingchili.mouse.enigma.R
import com.codingchili.mouse.enigma.model.CredentialBank
import com.codingchili.mouse.enigma.model.MousePreferences
import com.google.android.material.textfield.TextInputEditText
import io.realm.Realm


/**
 * @author Robin Duda
 *
 * Fragment used to setup the master password and generate a TEE key
 * protected by fingerprint.
 */
class MasterSetupFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Realm.init(context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_master_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences = MousePreferences(activity!!.application)
        val password = view.findViewById<TextInputEditText>(R.id.master_password)
        password.onEditorAction(EditorInfo.IME_ACTION_DONE)

        CredentialBank.setPreferences(preferences)

        if (preferences.isTeeGenerated()) {
            view.findViewById<TextView>(R.id.fp_header).text = getString(R.string.fp_authenticate)
            view.findViewById<View>(R.id.master_password).visibility = View.GONE
            view.findViewById<View>(R.id.master_password_header).visibility = View.GONE
            CredentialBank.initCipher(false)
        } else {
            CredentialBank.generateTEEKey()
            CredentialBank.initCipher(true)
        }

        val fingerprints: FingerprintManagerCompat = FingerprintManagerCompat.from(context!!)
        fingerprints.authenticate(
                FingerprintManagerCompat.CryptoObject(CredentialBank.getCipher()),
                0,
                androidx.core.os.CancellationSignal(),
                object : FingerprintManagerCompat.AuthenticationCallback() {

                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                        //onAuthenticationFailed()
                    }

                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        CredentialBank.setPreferences(MousePreferences(activity!!.application))

                        if (preferences.isTeeGenerated()) {
                            CredentialBank.load()
                        } else {
                            CredentialBank.install(password.text.toString())
                        }
                        FragmentSelector.list()
                    }

                    override fun onAuthenticationFailed() {
                        view.findViewById<TextView>(R.id.fp_header).text = getString(R.string.fp_authenticate_fail)
                        view.findViewById<ImageView>(R.id.fp_icon)
                                .setColorFilter(
                                        ContextCompat.getColor(context!!, R.color.accent),
                                        android.graphics.PorterDuff.Mode.MULTIPLY)
                    }
                },
                null)
    }
}