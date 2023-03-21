package com.v2reading.reader.ui.about


import android.os.Bundle
import androidx.core.view.isGone
import com.tradplus.ads.base.bean.TPAdError
import com.tradplus.ads.base.bean.TPAdInfo
import com.tradplus.ads.base.bean.TPBaseAd
import com.tradplus.ads.open.banner.BannerAdListener
import com.tradplus.ads.open.banner.TPBanner
import com.tradplus.ads.open.interstitial.InterstitialAdListener
import com.tradplus.ads.open.interstitial.TPInterstitial
import com.tradplus.ads.open.nativead.NativeAdListener
import com.tradplus.ads.open.nativead.TPNative
import com.tradplus.ads.open.reward.RewardAdListener
import com.tradplus.ads.open.reward.TPReward
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseActivity
import com.v2reading.reader.databinding.ActivityDonateBinding
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding


/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity<ActivityDonateBinding>() {

    override val binding by viewBinding(ActivityDonateBinding::inflate)


    private var mTpBanner: TPBanner? = null
    private var mTPInterstitial: TPInterstitial? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        binding.banner.setOnClickListener {
            binding.action.isGone = false
            binding.btnLoad.isEnabled = true
            binding.btnShow.isEnabled = false
            if (mTpBanner != null) {
                mTpBanner?.onDestroy()
                mTpBanner = null
            }
            mTpBanner = TPBanner(this)
            mTpBanner?.setAdListener(object : BannerAdListener() {
                // 广告加载完成 首个广告源加载成功时回调 一次加载流程只会回调一次
                override fun onAdLoaded(tpAdInfo: TPAdInfo?) {
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = true
                }

                // 广告被点击
                override fun onAdClicked(tpAdInfo: TPAdInfo?) {}

                // 广告成功展示在页面上
                override fun onAdImpression(tpAdInfo: TPAdInfo?) {}

                // 广告加载失败
                override fun onAdLoadFailed(error: TPAdError?) {
                    toastOnUi("加载失败,请稍后重试")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = false
                }

                // 广告被关闭
                override fun onAdClosed(tpAdInfo: TPAdInfo?) {}
            })
            binding.btnLoad.setOnClickListener {
                mTpBanner?.loadAd("A4C0DDEBB4A36746F3FFF18344C95ABB")
            }
            binding.btnShow.setOnClickListener {
                binding.adContainer.addView(mTpBanner)
                onDonateSuccess()
            }
        }

        binding.nativeAd.setOnClickListener {
            binding.action.isGone = false
            binding.btnLoad.isEnabled = true
            binding.btnShow.isEnabled = false

            val tpNative = TPNative(this, "830BE5A73E92256473F3D3731CD7091A")
            tpNative.setAdListener(object : NativeAdListener() {
                override fun onAdLoaded(tpAdInfo: TPAdInfo, tpBaseAd: TPBaseAd?) {
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = true
                }

                override fun onAdClicked(tpAdInfo: TPAdInfo) {
                }

                override fun onAdImpression(tpAdInfo: TPAdInfo) {
                }

                override fun onAdShowFailed(tpAdError: TPAdError?, tpAdInfo: TPAdInfo) {
                }

                override fun onAdLoadFailed(tpAdError: TPAdError) {
                    toastOnUi("加载失败,请稍后重试")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = false
                }

                override fun onAdClosed(tpAdInfo: TPAdInfo) {
                }
            })
            binding.btnLoad.setOnClickListener {
                tpNative.loadAd()
            }
            binding.btnShow.setOnClickListener {
                tpNative.showAd(binding.adContainer, R.layout.tp_native_ad_list_item, "")
                onDonateSuccess()
            }
        }

        binding.page.setOnClickListener {
            binding.action.isGone = false
            binding.btnLoad.isEnabled = true
            binding.btnShow.isEnabled = false

            if (mTPInterstitial != null) {
                mTPInterstitial?.onDestroy()
                mTPInterstitial = null
            }
            mTPInterstitial = TPInterstitial(this, "8A892D088C7945172261AE4BB29A3696")
            // 监听广告的不同状态
            mTPInterstitial?.setAdListener(object : InterstitialAdListener {
                override fun onAdLoaded(tpAdInfo: TPAdInfo?) {
//                    Log.i(TAG, "onAdLoaded: ")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = true
                }

                override fun onAdClicked(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdClicked: 广告" + tpAdInfo.adSourceName + "被点击")
                }

                override fun onAdImpression(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdImpression: 广告" + tpAdInfo.adSourceName + "展示")
                }

                override fun onAdFailed(tpAdError: TPAdError?) {
//                    Log.i(TAG, "onAdFailed: ")
                    toastOnUi("加载失败,请稍后重试")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = false
                }

                override fun onAdClosed(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "被关闭")
                }

                override fun onAdVideoError(tpAdInfo: TPAdInfo, tpAdError: TPAdError?) {
//                    Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "展示失败")
                }

                override fun onAdVideoStart(tpAdInfo: TPAdInfo?) {
                    // V8.1.0.1 播放开始
                }

                override fun onAdVideoEnd(tpAdInfo: TPAdInfo?) {
                    // V8.1.0.1 播放结束
                }
            })

            binding.btnLoad.setOnClickListener {
                mTPInterstitial?.loadAd()
            }
            binding.btnShow.setOnClickListener {
                if (mTPInterstitial?.isReady == true) {
                    mTPInterstitial?.showAd(this, null)
                    onDonateSuccess()
                }
            }
        }

        binding.reward.setOnClickListener {
            binding.action.isGone = false
            binding.btnLoad.isEnabled = true
            binding.btnShow.isEnabled = false

            val tpReward = TPReward(this, "76576D318D9B258BD0A1C70A6C4E26D5")
            // 监听广告的不同状态
            tpReward.setAdListener(object : RewardAdListener {
                override fun onAdLoaded(tpAdInfo: TPAdInfo?) {
//                    Log.i(TAG, "onAdLoaded: ")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = true
                }

                override fun onAdClicked(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdClicked: 广告" + tpAdInfo.adSourceName + "被点击")
                }

                override fun onAdImpression(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdImpression: 广告" + tpAdInfo.adSourceName + "展示")
                }

                override fun onAdFailed(tpAdError: TPAdError?) {
//                    Log.i(TAG, "onAdFailed: ")
                    toastOnUi("加载失败,请稍后重试")
                    binding.btnLoad.isEnabled = false
                    binding.btnShow.isEnabled = false
                }

                override fun onAdClosed(tpAdInfo: TPAdInfo) {
//                    Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "被关闭")
                }

                override fun onAdReward(p0: TPAdInfo?) {
                    onDonateSuccess()
                }

                override fun onAdVideoError(tpAdInfo: TPAdInfo, tpAdError: TPAdError?) {
//                    Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "展示失败")
                }

                override fun onAdVideoStart(tpAdInfo: TPAdInfo?) {
                    // V8.1.0.1 播放开始
                }

                override fun onAdVideoEnd(tpAdInfo: TPAdInfo?) {
                    // V8.1.0.1 播放结束
                }
            })

            binding.btnLoad.setOnClickListener {
                tpReward.loadAd()
            }
            binding.btnShow.setOnClickListener {
                if (tpReward.isReady) {
                    tpReward.showAd(this, null)
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        if (mTpBanner != null) {
            mTpBanner?.onDestroy()
            mTpBanner = null
        }
        if (mTPInterstitial != null) {
            mTPInterstitial?.onDestroy()
            mTPInterstitial = null
        }

    }

    private fun onDonateSuccess() {
        toastOnUi("感谢您的支持,我们会努力做的更好!")
    }

}
