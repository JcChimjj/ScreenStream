package info.dvkr.screenstream.ui.fragment

import android.graphics.Color.parseColor
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.R
import info.dvkr.screenstream.data.other.getLog
import info.dvkr.screenstream.data.settings.Settings
import info.dvkr.screenstream.data.settings.SettingsReadOnly
import info.dvkr.screenstream.logging.cleanLogFiles
import info.dvkr.screenstream.service.helper.NotificationHelper
import kotlinx.android.synthetic.main.dialog_settings_crop.view.*
import kotlinx.android.synthetic.main.dialog_settings_resize.view.*
import kotlinx.android.synthetic.main.fragment_settings.*
import org.koin.android.ext.android.inject

class SettingsFragment : Fragment() {

    private val notificationHelper: NotificationHelper by inject()
    private val settings: Settings by inject()
    private val settingsListener = object : SettingsReadOnly.OnSettingsChangeListener {
        override fun onSettingsChanged(key: String) = when (key) {
            Settings.Key.NIGHT_MODE -> {
                val index = nightModeList.first { it.second == settings.nightMode }.first
                tv_fragment_settings_night_mode_summary.text =
                    resources.getStringArray(R.array.pref_night_mode_options)[index]
            }

            Settings.Key.HTML_BACK_COLOR ->
                v_fragment_settings_html_back_color.color = settings.htmlBackColor

            Settings.Key.RESIZE_FACTOR ->
                tv_fragment_settings_resize_image_value.text =
                    getString(R.string.pref_resize_value, settings.resizeFactor)

            Settings.Key.ROTATION ->
                tv_fragment_settings_rotation_value.text = getString(R.string.pref_rotate_value, settings.rotation)

            Settings.Key.JPEG_QUALITY ->
                tv_fragment_settings_jpeg_quality_value.text = settings.jpegQuality.toString()

            Settings.Key.PIN ->
                tv_fragment_settings_set_pin_value.text = settings.pin

            Settings.Key.SERVER_PORT ->
                tv_fragment_settings_server_port_value.text = settings.severPort.toString()

            else -> Unit
        }
    }

    private val screenSize: Point by lazy {
        Point().apply {
            ContextCompat.getSystemService(requireContext(), WindowManager::class.java)
                ?.defaultDisplay?.getRealSize(this)
        }
    }

    private val nightModeList = listOf(
        0 to AppCompatDelegate.MODE_NIGHT_YES,
        1 to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        2 to AppCompatDelegate.MODE_NIGHT_AUTO,
        3 to AppCompatDelegate.MODE_NIGHT_NO
    )

    private val rotationList = listOf(
        0 to Settings.Values.ROTATION_0,
        1 to Settings.Values.ROTATION_90,
        2 to Settings.Values.ROTATION_180,
        3 to Settings.Values.ROTATION_270
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Interface - Night mode
        val index = nightModeList.first { it.second == settings.nightMode }.first
        tv_fragment_settings_night_mode_summary.text = resources.getStringArray(R.array.pref_night_mode_options)[index]
        cl_fragment_settings_night_mode.setOnClickListener {
            val indexOld = nightModeList.first { it.second == settings.nightMode }.first
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_night_mode)
                icon(R.drawable.ic_settings_night_mode_24dp)
                listItemsSingleChoice(R.array.pref_night_mode_options, initialSelection = indexOld) { _, index, _ ->
                    settings.nightMode = nightModeList.firstOrNull { item -> item.first == index }?.second
                        ?: throw IllegalArgumentException("Unknown night mode index")
                }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }

        // Interface - Device notification settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cl_fragment_settings_notification.setOnClickListener {
                startActivity(notificationHelper.getNotificationSettingsIntent())
            }
        } else {
            cl_fragment_settings_notification.visibility = View.GONE
            v_fragment_settings_notification.visibility = View.GONE
        }

        // Interface - Minimize on stream
        with(cb_fragment_settings_minimize_on_stream) {
            isChecked = settings.minimizeOnStream
            setOnClickListener { settings.minimizeOnStream = isChecked }
            cl_fragment_settings_minimize_on_stream.setOnClickListener { performClick() }
        }

        // Interface - Stop on sleep
        with(cb_fragment_settings_stop_on_sleep) {
            isChecked = settings.stopOnSleep
            setOnClickListener { settings.stopOnSleep = isChecked }
            cl_fragment_settings_stop_on_sleep.setOnClickListener { performClick() }
        }

        // Interface - StartService on boot
        with(cb_fragment_settings_start_on_boot) {
            isChecked = settings.startOnBoot
            setOnClickListener { settings.startOnBoot = isChecked }
            cl_fragment_settings_start_on_boot.setOnClickListener { performClick() }
        }

        // Interface - Auto start stop
        with(cb_fragment_settings_auto_start_stop) {
            isChecked = settings.autoStartStop
            setOnClickListener { settings.autoStartStop = isChecked }
            cl_fragment_settings_auto_start_stop.setOnClickListener { performClick() }
        }

        // Interface - Notify slow connections
        with(cb_fragment_settings_notify_slow_connections) {
            isChecked = settings.notifySlowConnections
            setOnClickListener { settings.notifySlowConnections = isChecked }
            cl_fragment_settings_notify_slow_connections.setOnClickListener { performClick() }
        }

        // Web page - Image buttons
        with(cb_fragment_settings_html_buttons) {
            isChecked = settings.htmlEnableButtons
            setOnClickListener { settings.htmlEnableButtons = isChecked }
            cl_fragment_settings_html_buttons.setOnClickListener { performClick() }
        }

        // Web page - HTML Back color
        v_fragment_settings_html_back_color.color = settings.htmlBackColor
        v_fragment_settings_html_back_color.border = ContextCompat.getColor(requireContext(), R.color.textColorPrimary)
        cl_fragment_settings_html_back_color.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_html_back_color_title)
                icon(R.drawable.ic_settings_html_back_color_24dp)
                colorChooser(
                    colors = ColorPalette.Primary + parseColor("#000000"),
                    initialSelection = settings.htmlBackColor,
                    allowCustomArgb = true
                ) { _, color -> if (settings.htmlBackColor != color) settings.htmlBackColor = color }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }


        // Image - Crop image
        with(cb_fragment_settings_crop_image) {
            isChecked = settings.imageCrop
            setOnClickListener {
                if (settings.imageCrop) settings.imageCrop = false
                else {
                    isChecked = false
                    MaterialDialog(requireActivity())
                        .lifecycleOwner(viewLifecycleOwner)
                        .title(R.string.pref_crop)
                        .icon(R.drawable.ic_settings_crop_24dp)
                        .customView(R.layout.dialog_settings_crop)
                        .positiveButton(android.R.string.ok) { dialog ->
                            dialog.getCustomView().apply DialogView@{
                                val newTopCrop = tiet_dialog_settings_crop_top.text.toString().toInt()
                                if (newTopCrop != settings.imageCropTop) settings.imageCropTop = newTopCrop
                                val newBottomCrop = tiet_dialog_settings_crop_bottom.text.toString().toInt()
                                if (newBottomCrop != settings.imageCropBottom) settings.imageCropBottom = newBottomCrop
                                val newLeftCrop = tiet_dialog_settings_crop_left.text.toString().toInt()
                                if (newLeftCrop != settings.imageCropLeft) settings.imageCropLeft = newLeftCrop
                                val newRightCrop = tiet_dialog_settings_crop_right.text.toString().toInt()
                                if (newRightCrop != settings.imageCropRight) settings.imageCropRight = newRightCrop

                                settings.imageCrop = newTopCrop + newBottomCrop + newLeftCrop + newRightCrop != 0
                                cb_fragment_settings_crop_image.isChecked = settings.imageCrop
                            }
                        }
                        .negativeButton(android.R.string.cancel)
                        .apply Dialog@{
                            getCustomView().apply DialogView@{
                                tv_dialog_settings_crop_error_message.visibility = View.GONE
                                tiet_dialog_settings_crop_top.setText(settings.imageCropTop.toString())
                                tiet_dialog_settings_crop_bottom.setText(settings.imageCropBottom.toString())
                                tiet_dialog_settings_crop_left.setText(settings.imageCropLeft.toString())
                                tiet_dialog_settings_crop_right.setText(settings.imageCropRight.toString())

                                tiet_dialog_settings_crop_top.setSelection(settings.imageCropTop.toString().length)

                                val validateTextWatcher = SimpleTextWatcher { _ ->
                                    validateCropValues(
                                        this@Dialog,
                                        tiet_dialog_settings_crop_top,
                                        tiet_dialog_settings_crop_bottom,
                                        tiet_dialog_settings_crop_left,
                                        tiet_dialog_settings_crop_right,
                                        tv_dialog_settings_crop_error_message
                                    )
                                }

                                tiet_dialog_settings_crop_top.addTextChangedListener(validateTextWatcher)
                                tiet_dialog_settings_crop_bottom.addTextChangedListener(validateTextWatcher)
                                tiet_dialog_settings_crop_left.addTextChangedListener(validateTextWatcher)
                                tiet_dialog_settings_crop_right.addTextChangedListener(validateTextWatcher)
                            }

                            show()
                        }

                }
            }
            cl_fragment_settings_crop_image.setOnClickListener { performClick() }
        }

        // Image - Resize factor
        tv_fragment_settings_resize_image_value.text = getString(R.string.pref_resize_value, settings.resizeFactor)
        val resizePictureSizeString = getString(R.string.pref_resize_dialog_result)
        cl_fragment_settings_resize_image.setOnClickListener {
            MaterialDialog(requireActivity())
                .lifecycleOwner(viewLifecycleOwner)
                .title(R.string.pref_resize)
                .icon(R.drawable.ic_settings_resize_24dp)
                .customView(R.layout.dialog_settings_resize)
                .positiveButton(android.R.string.ok) { dialog ->
                    dialog.getCustomView().apply DialogView@{
                        val newResizeFactor = tiet_dialog_settings_resize.text.toString().toInt()
                        if (newResizeFactor != settings.resizeFactor) settings.resizeFactor = newResizeFactor
                    }
                }
                .negativeButton(android.R.string.cancel)
                .apply Dialog@{
                    getCustomView().apply DialogView@{
                        tv_dialog_settings_resize_content.text =
                            getString(R.string.pref_resize_dialog_text, screenSize.x, screenSize.y)

                        ti_dialog_settings_resize.isCounterEnabled = true
                        ti_dialog_settings_resize.counterMaxLength = 3

                        with(tiet_dialog_settings_resize) {
                            addTextChangedListener(SimpleTextWatcher { text ->
                                val isValid = text.length in 2..3 && text.toString().toInt() in 10..150
                                this@Dialog.setActionButtonEnabled(
                                    WhichButton.POSITIVE, isValid
                                )
                                val newResizeFactor =
                                    (if (isValid) text.toString().toInt() else settings.resizeFactor) / 100f
                                this@DialogView.tv_dialog_settings_resize_result.text = resizePictureSizeString.format(
                                    (screenSize.x * newResizeFactor).toInt(), (screenSize.y * newResizeFactor).toInt()
                                )
                            })
                            setText(settings.resizeFactor.toString())
                            setSelection(settings.resizeFactor.toString().length)
                            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
                        }

                        tv_dialog_settings_resize_result.text = resizePictureSizeString.format(
                            (screenSize.x * settings.resizeFactor / 100f).toInt(),
                            (screenSize.y * settings.resizeFactor / 100f).toInt()
                        )

                        show()
                    }
                }
        }

        // Image - Rotation
        tv_fragment_settings_rotation_value.text = getString(R.string.pref_rotate_value, settings.rotation)
        cl_fragment_settings_rotation.setOnClickListener {
            val indexOld = rotationList.first { it.second == settings.rotation }.first
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_rotate)
                icon(R.drawable.ic_settings_rotation_24dp)
                listItemsSingleChoice(R.array.pref_rotate_options, initialSelection = indexOld) { _, index, _ ->
                    settings.rotation = rotationList.firstOrNull { item -> item.first == index }?.second
                        ?: throw IllegalArgumentException("Unknown rotation index")
                }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }

        // Image - Jpeg Quality
        tv_fragment_settings_jpeg_quality_value.text = settings.jpegQuality.toString()
        cl_fragment_settings_jpeg_quality.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_jpeg_quality)
                icon(R.drawable.ic_settings_high_quality_24dp)
                message(R.string.pref_jpeg_quality_dialog)
                input(
                    prefill = settings.jpegQuality.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 3,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 2..3 && text.toString().toInt() in 10..100
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField().text?.toString()?.toInt() ?: settings.jpegQuality
                    if (settings.jpegQuality != newValue) settings.jpegQuality = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField().filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
                getInputField().imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }


        // Security - Enable pin
        with(cb_fragment_settings_enable_pin) {
            isChecked = settings.enablePin
            enableDisableViewWithChildren(cl_fragment_settings_hide_pin_on_start, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_new_pin_on_app_start, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_auto_change_pin, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                if (isChecked) settings.pin = String(settings.pin.toCharArray()) // Workaround for BinaryPreferences IPC
                settings.enablePin = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_hide_pin_on_start, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_new_pin_on_app_start, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_auto_change_pin, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_enable_pin.setOnClickListener { performClick() }
        }

        // Security - Hide pin on start
        with(cb_fragment_settings_hide_pin_on_start) {
            isChecked = settings.hidePinOnStart
            setOnClickListener { settings.hidePinOnStart = isChecked }
            cl_fragment_settings_hide_pin_on_start.setOnClickListener { performClick() }
        }

        // Security - New pin on app start
        with(cb_fragment_settings_new_pin_on_app_start) {
            isChecked = settings.newPinOnAppStart
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                settings.newPinOnAppStart = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_new_pin_on_app_start.setOnClickListener { performClick() }
        }

        // Security - Auto change pin
        with(cb_fragment_settings_auto_change_pin) {
            isChecked = settings.autoChangePin
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                settings.autoChangePin = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_auto_change_pin.setOnClickListener { performClick() }
        }

        // Security - Set pin
        tv_fragment_settings_set_pin_value.text = settings.pin
        cl_fragment_settings_set_pin.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_set_pin)
                icon(R.drawable.ic_settings_key_24dp)
                message(R.string.pref_set_pin_dialog)
                input(
                    prefill = settings.pin,
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 4,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 4..4 && text.toString().toInt() in 0..9999
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField().text?.toString() ?: settings.pin
                    if (settings.pin != newValue) settings.pin = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField().filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
                getInputField().imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }

        // Advanced - Use WiFi Only
        with(cb_fragment_settings_use_wifi_only) {
            isChecked = settings.useWiFiOnly
            setOnClickListener { settings.useWiFiOnly = isChecked }
            cl_fragment_settings_use_wifi_only.setOnClickListener { performClick() }
        }

        // Advanced - Enable IPv6 support
        with(cb_fragment_settings_enable_ipv6) {
            isChecked = settings.enableIPv6
            setOnClickListener { settings.enableIPv6 = isChecked }
            cl_fragment_settings_enable_ipv6.setOnClickListener { performClick() }
        }

        // Advanced - Server port
        tv_fragment_settings_server_port_value.text = settings.severPort.toString()
        cl_fragment_settings_server_port.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                lifecycleOwner(viewLifecycleOwner)
                title(R.string.pref_server_port)
                icon(R.drawable.ic_settings_http_24dp)
                message(R.string.pref_server_port_dialog)
                input(
                    prefill = settings.severPort.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 5,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 4..5 && text.toString().toInt() in 1025..65535
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField().text?.toString()?.toInt() ?: settings.severPort
                    if (settings.severPort != newValue) settings.severPort = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField().filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
                getInputField().imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }

        // Advanced - Enable application logs
        with(cb_fragment_settings_logging) {
            setOnClickListener {
                settings.loggingOn = isChecked
                if (settings.loggingOn.not()) cleanLogFiles(requireContext().applicationContext)
            }
            cl_fragment_settings_logging.setOnClickListener { performClick() }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.registerChangeListener(settingsListener)
        XLog.d(getLog("onStart", "Invoked"))
        tv_fragment_settings_set_pin_value.text = settings.pin
    }

    override fun onStop() {
        XLog.d(getLog("onStop", "Invoked"))
        settings.unregisterChangeListener(settingsListener)
        super.onStop()
    }

    private fun validateCropValues(
        dialog: MaterialDialog,
        topView: EditText,
        bottomView: EditText,
        leftView: EditText,
        rightView: EditText,
        errorView: TextView
    ) {
        val topCrop = topView.text.let { if (it.isNullOrBlank()) -1 else it.toString().toInt() }
        val bottomCrop = bottomView.text.let { if (it.isNullOrBlank()) -1 else it.toString().toInt() }
        val leftCrop = leftView.text.let { if (it.isNullOrBlank()) -1 else it.toString().toInt() }
        val rightCrop = rightView.text.let { if (it.isNullOrBlank()) -1 else it.toString().toInt() }

        val isTopBottomValid = (topCrop + bottomCrop) < screenSize.y
        val isLeftRightValid = (leftCrop + rightCrop) < screenSize.x

        if (isTopBottomValid && isLeftRightValid) {
            if (topCrop >= 0 && bottomCrop >= 0 && leftCrop >= 0 && rightCrop >= 0) {
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, true)
                errorView.visibility = View.GONE
            } else {
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, false)
                errorView.text = getString(R.string.pref_crop_dialog_error_message)
                errorView.visibility = View.VISIBLE
            }
        } else {
            dialog.setActionButtonEnabled(WhichButton.POSITIVE, false)
            if (isTopBottomValid.not()) {
                errorView.text = getString(R.string.pref_crop_dialog_error_message_top_bottom, screenSize.y)
            } else {
                errorView.text = getString(R.string.pref_crop_dialog_error_message_left_right, screenSize.x)
            }
            errorView.visibility = View.VISIBLE
        }
    }

    private fun canEnableSetPin(): Boolean =
        cb_fragment_settings_enable_pin.isChecked &&
                cb_fragment_settings_new_pin_on_app_start.isChecked.not() &&
                cb_fragment_settings_auto_change_pin.isChecked.not()

    private fun enableDisableViewWithChildren(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else .5f
        if (view is ViewGroup)
            for (idx in 0 until view.childCount) enableDisableViewWithChildren(view.getChildAt(idx), enabled)
    }

    private class SimpleTextWatcher(private val afterTextChangedBlock: (s: Editable) -> Unit) : TextWatcher {
        override fun afterTextChanged(s: Editable?) = s?.let { afterTextChangedBlock(it) } as Unit
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
    }
}