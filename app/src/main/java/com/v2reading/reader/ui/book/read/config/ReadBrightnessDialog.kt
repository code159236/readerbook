package com.v2reading.reader.ui.book.read.config

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.databinding.DialogReadBrightnessStyleBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.lib.theme.getPrimaryTextColor
import com.v2reading.reader.ui.book.read.EyeCareService
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.widget.seekbar.SeekBarChangeListener
import com.v2reading.reader.utils.ColorUtils
import com.v2reading.reader.utils.getPrefBoolean
import com.v2reading.reader.utils.putPrefBoolean
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding


class ReadBrightnessDialog : BaseDialogFragment(R.layout.dialog_read_brightness_style){

    private val binding by viewBinding(DialogReadBrightnessStyleBinding::bind)
    private val callBack get() = activity as? ReadBookActivity

    private val showBrightnessView
        get() = context?.getPrefBoolean(
            PreferKey.showBrightnessView,
            true
        )


    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        (activity as ReadBookActivity).bottomDialog++
        initView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
        (activity as ReadBookActivity).bottomDialog--
    }

    private fun initView() = binding.run {
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        rootView.setBackgroundColor(bg)

        seekBrightness.post {
            seekBrightness.progress = AppConfig.readBrightness
        }

        upBrightnessState()

        //亮度跟随
        ivBrightnessAuto.setOnClickListener {
            context?.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seekBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setScreenBrightness(progress)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppConfig.readBrightness = seekBar.progress
            }

        })

        careEye.setOnCheckedChangeListener { _, b ->
            run {
                if (b) {
                    openEyeCareMode()
                } else {
                    closeEyeCareMode()
                }
            }
        }

    }

    fun openEyeCareMode() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(context)) { //有悬浮窗权限开启服务绑定 绑定权限
                val intent = Intent(context, EyeCareService::class.java)
                context?.startService(intent)
            } else { //没有悬浮窗权限,去开启悬浮窗权限
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
                    startActivityForResult(intent, 1234)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            val intent = Intent(context, EyeCareService::class.java)
            context?.startService(intent)
        }
    }

    fun closeEyeCareMode() {
         val intent = Intent(context, EyeCareService::class.java)
        context?.stopService(intent)
    }


     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(context)) {
                     //权限授予失败，无法开启悬浮窗
                    binding.careEye.isChecked = false
                    return
                } else {
                     //权限授予成功
                } //有悬浮窗权限开启服务绑定 绑定权限
            }
            val intent = Intent(context, EyeCareService::class.java)
            context?.startService(intent)
        } else if (requestCode == 10) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(context)) {
//                    Toast.makeText(context, "not granted", Toast.LENGTH_SHORT)
                    binding.careEye.isChecked = false
                }
            }
        }
    }

    private fun brightnessAuto(): Boolean {
        return context?.getPrefBoolean("brightnessAuto", true) == true || !showBrightnessView!!
    }


    /**
     * 设置屏幕亮度
     */
    private fun setScreenBrightness(value: Int) {
        var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (!brightnessAuto()) {
            brightness = value.toFloat()
            if (brightness < 1f) brightness = 1f
            brightness /= 255f
        }
        val params = activity?.window?.attributes
        params?.screenBrightness = brightness
        activity?.window?.attributes = params
    }


    fun upBrightnessState() {
        if (brightnessAuto()) {
            binding.ivBrightnessAuto.isChecked = true
            binding.seekBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.isChecked = false
            binding.seekBrightness.isEnabled = true
        }
        setScreenBrightness(AppConfig.readBrightness)
    }

}