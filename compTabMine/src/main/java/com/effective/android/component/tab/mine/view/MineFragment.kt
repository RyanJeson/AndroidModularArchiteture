package com.effective.android.component.tab.mine.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import com.effective.android.base.fragment.BaseVmFragment
import com.effective.android.base.toast.ToastUtils
import com.effective.android.base.util.DisplayUtils
import com.effective.android.base.util.ResourceUtils
import com.effective.android.base.view.dialog.CommonDialog
import com.effective.android.component.tab.mine.R
import com.effective.android.component.tab.mine.Sdks
import com.effective.android.component.tab.mine.vm.MineViewModel
import com.effective.android.service.account.AccountChangeListener
import com.effective.android.service.account.UserInfo
import com.effective.android.service.skin.Skin
import com.effective.android.service.skin.SkinChangeListener
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.mine_fragment_main_layout.*


class MineFragment : BaseVmFragment<MineViewModel>() {

    private var userInfo: UserInfo? = null
    private lateinit var accountDisposable: Disposable
    private val accountChangeListener = object : AccountChangeListener {

        override fun onAccountChange(userInfo: UserInfo?, login: Boolean, success: Boolean, message: String?) {
            if (success) {
                this@MineFragment.userInfo = if (login) userInfo else null
                checkoutStatus()
            } else {
                ToastUtils.show(context!!, message ?: "操作失败")
            }
        }
    }
    private var skinChangeListener : SkinChangeListener? = null

    override fun getViewModel(): Class<MineViewModel> = MineViewModel::class.java

    override fun getLayoutRes(): Int = R.layout.mine_fragment_main_layout

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        initListener()
        initData()
    }

    private fun initView() {
        val type = Typeface.createFromAsset(context?.assets, "fonts/DIN-Condensed-Bold-2.ttf")
        share_count.typeface = type
        collect_count.typeface = type
        initSkin()
    }

    private fun initSkin(){
        if(Sdks.serviceSkin.isLoadingDefaultSkin()){
            night_action.isSelected = false
            statusView.setBackgroundColor(ResourceUtils.getColor(context!!,R.color.colorPrimary))
            infoContainer.setBackgroundColor(ResourceUtils.getColor(context!!,R.color.colorPrimary))
        }else{
            night_action.isSelected = true
            statusView.setBackgroundColor(ResourceUtils.getColor(context!!,R.color.blockBackground))
            infoContainer.setBackgroundColor(ResourceUtils.getColor(context!!,R.color.blockBackground))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.removeAccountChangeListener(accountChangeListener)
        if (!accountDisposable.isDisposed) {
            accountDisposable.dispose()
        }
        Sdks.serviceSkin.removeSkinChangeListener(skinChangeListener)
    }

    private fun initListener() {
        avatar.setOnClickListener {
            if (isLogin()) {
                CommonDialog.Builder(it.context)
                        .content("确认退出登录吗")
                        .left("取消")
                        .right("确定", View.OnClickListener {
                            viewModel.logout()
                        })
                        .build()
                        .show()
            }
        }
        viewModel.addAccountChangeListener(accountChangeListener)
        night_mode.setOnClickListener {
            /**
             * 如果app有多种皮肤，则需要 changeSkin(skin: com.effective.android.service.skin.Skin)
             */
            if(skinChangeListener == null){
                skinChangeListener = object : SkinChangeListener {

                    override fun onSkinChange(skin: Skin, success: Boolean) {
                        if(success){
                            night_action.isSelected = !Sdks.serviceSkin.isLoadingDefaultSkin()
                            initSkin()
                        }
                    }
                }
                Sdks.serviceSkin.addSkinChangeListener(skinChangeListener)
            }
            Sdks.serviceSkin.changeSkin()
        }
    }



    private fun initData() {
        checkoutStatus()
        accountDisposable = viewModel.getLoginAccount()
                .subscribe({
                    if (it.isValid()) {
                        userInfo = it
                    }
                    checkoutStatus()
                }, {
                    checkoutStatus()
                })
    }

    private fun checkoutStatus() {
        refreshUserInfo()
        refreshRank()
        refreshShareAndCollection()
    }

    private fun refreshUserInfo() {
        val hasLogin = userInfo != null
        avatar.isSelected = hasLogin
        if (hasLogin) {
            //个人信息
            login.visibility = View.GONE
            avatar.setImageResource(R.drawable.mine_ic_login)
            nick.visibility = View.VISIBLE
            nick.text = userInfo!!.nickname
        } else {
            //个人信息
            login.visibility = View.VISIBLE
            avatar.setImageResource(R.drawable.mine_ic_logout)
            nick.visibility = View.GONE
            login.setOnClickListener {
                viewModel.login(context!!)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshRank() {
        if (isLogin()) {
            userInfo?.rankInfo.let {
                rank_title.text = getString(R.string.mine_rank)
                rank_hint.text = getString(R.string.mine_rank_check)
                tv_privilege_level.text = "Lv.${it?.level.toString()}"
                pb_privilege_exp.setPrivilegeProgress(it?.coinCount?.toInt() ?: 0)
            }
        } else {
            rank_title.text = getString(R.string.mine_rank_logout)
            rank_hint.text = getString(R.string.mine_rank_check_logout)
            tv_privilege_level.text = "Lv.--"
            pb_privilege_exp.setPrivilegeProgress(0)
        }
    }

    private fun refreshShareAndCollection() {
        if (isLogin()) {
            userInfo?.actionInfo.let {
                share_count.text = it?.shareCount.toString()
                collect_count.text = it?.collectCount.toString()
            }
        } else {
            share_count.text = "--"
            collect_count.text = "--"
        }
    }

    private fun isLogin() = userInfo != null
}